package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable journal for the non-atomic Minecraft-inventory to canonical-inventory boundary. */
public final class FileCraftIngredientDepositHandoffRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41504448; // APDH
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();
    private final Path handoffsDirectory;

    public FileCraftIngredientDepositHandoffRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        handoffsDirectory = rootDirectory.toAbsolutePath().normalize().resolve("craft-deposit-handoffs");
        try {
            Files.createDirectories(handoffsDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create craft deposit handoff store", error);
        }
    }

    public Optional<CraftIngredientDepositHandoff> find(String handoffId) {
        String id = requireId(handoffId);
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        CraftIngredientDepositHandoff handoff = read(path);
        if (!handoff.handoffId().equals(id)) throw new IllegalStateException("craft deposit handoff identity mismatch");
        return Optional.of(handoff);
    }

    public List<CraftIngredientDepositHandoff> findPendingForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        String owner = playerId.strip();
        List<CraftIngredientDepositHandoff> matches = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(handoffsDirectory, "*.bin")) {
            for (Path path : files) {
                CraftIngredientDepositHandoff handoff = read(path);
                if (handoff.playerId().equals(owner) && !handoff.terminal()) matches.add(handoff);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan craft deposit handoffs", error);
        }
        matches.sort(Comparator.comparing(CraftIngredientDepositHandoff::handoffId));
        return List.copyOf(matches);
    }

    public boolean createIfAbsent(CraftIngredientDepositHandoff handoff) {
        if (handoff == null) throw new IllegalArgumentException("handoff is required");
        return withLock(handoff.handoffId(), () -> {
            Path path = statePath(handoff.handoffId());
            if (Files.exists(path)) return false;
            writeAtomically(path, handoff);
            return true;
        });
    }

    public boolean replaceIfPhase(
            String handoffId,
            CraftIngredientDepositHandoff.Phase expectedPhase,
            CraftIngredientDepositHandoff replacement
    ) {
        String id = requireId(handoffId);
        if (expectedPhase == null) throw new IllegalArgumentException("expectedPhase is required");
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.handoffId().equals(id)) throw new IllegalArgumentException("replacement identity must match handoffId");
        return withLock(id, () -> {
            Path path = statePath(id);
            if (!Files.exists(path)) return false;
            CraftIngredientDepositHandoff current = read(path);
            if (current.phase() != expectedPhase) return false;
            requireStableIdentity(current, replacement);
            writeAtomically(path, replacement);
            return true;
        });
    }

    private static void requireStableIdentity(CraftIngredientDepositHandoff current, CraftIngredientDepositHandoff replacement) {
        if (!current.handoffId().equals(replacement.handoffId())
                || !current.playerId().equals(replacement.playerId())
                || !current.itemTemplateId().equals(replacement.itemTemplateId())
                || current.inventorySlot() != replacement.inventorySlot()
                || current.beforeCount() != replacement.beforeCount()
                || current.quantity() != replacement.quantity()) {
            throw new IllegalArgumentException("craft deposit handoff immutable identity changed");
        }
    }

    private CraftIngredientDepositHandoff read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid craft deposit handoff file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported craft deposit handoff schema version");
                CraftIngredientDepositHandoff handoff = new CraftIngredientDepositHandoff(
                        input.readUTF(), input.readUTF(), input.readUTF(), input.readInt(), input.readInt(), input.readInt(),
                        CraftIngredientDepositHandoff.Phase.valueOf(input.readUTF()));
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing craft deposit handoff data");
                return handoff;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read craft deposit handoff", error);
        }
    }

    private void writeAtomically(Path target, CraftIngredientDepositHandoff handoff) {
        byte[] encoded = encode(handoff);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(handoffsDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("craft deposit handoff store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist craft deposit handoff", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private byte[] encode(CraftIngredientDepositHandoff handoff) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(handoff.handoffId());
                output.writeUTF(handoff.playerId());
                output.writeUTF(handoff.itemTemplateId());
                output.writeInt(handoff.inventorySlot());
                output.writeInt(handoff.beforeCount());
                output.writeInt(handoff.quantity());
                output.writeUTF(handoff.phase().name());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode craft deposit handoff", error);
        }
    }

    private <T> T withLock(String handoffId, IoSupplier<T> operation) {
        Path lockPath = lockPath(handoffId);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("craft deposit handoff store operation failed", error);
        } finally {
            processLock.unlock();
        }
    }

    private Path statePath(String handoffId) {
        return handoffsDirectory.resolve(fileKey(handoffId) + ".bin");
    }

    private Path lockPath(String handoffId) {
        return handoffsDirectory.resolve(fileKey(handoffId) + ".lock");
    }

    private static String fileKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", error);
        }
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("handoffId must not be blank");
        return value.strip();
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
