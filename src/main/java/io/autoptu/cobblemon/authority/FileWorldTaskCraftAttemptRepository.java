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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable file journal for capability-sensitive world-task crafting attempts. */
public final class FileWorldTaskCraftAttemptRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41504341; // APCA
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path attemptsDirectory;

    public FileWorldTaskCraftAttemptRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        attemptsDirectory = rootDirectory.toAbsolutePath().normalize().resolve("craft-attempts");
        try {
            Files.createDirectories(attemptsDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create craft attempt store", error);
        }
    }

    public Optional<WorldTaskCraftAttempt> find(String attemptId) {
        String id = requireId(attemptId);
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        WorldTaskCraftAttempt attempt = read(path);
        if (!attempt.attemptId().equals(id)) throw new IllegalStateException("craft attempt identity mismatch");
        return Optional.of(attempt);
    }

    public boolean createIfAbsent(WorldTaskCraftAttempt attempt) {
        if (attempt == null) throw new IllegalArgumentException("attempt is required");
        return withLock(attempt.attemptId(), () -> {
            Path path = statePath(attempt.attemptId());
            if (Files.exists(path)) return false;
            writeAtomically(path, attempt);
            return true;
        });
    }

    public boolean replaceIfPhase(
            String attemptId,
            WorldTaskCraftAttempt.Phase expectedPhase,
            WorldTaskCraftAttempt replacement
    ) {
        String id = requireId(attemptId);
        if (expectedPhase == null) throw new IllegalArgumentException("expectedPhase is required");
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.attemptId().equals(id)) {
            throw new IllegalArgumentException("replacement attempt identity must match attemptId");
        }
        return withLock(id, () -> {
            Path path = statePath(id);
            if (!Files.exists(path)) return false;
            WorldTaskCraftAttempt current = read(path);
            if (!current.attemptId().equals(id)) throw new IllegalStateException("craft attempt identity mismatch");
            if (current.phase() != expectedPhase) return false;
            requireStableIdentity(current, replacement);
            writeAtomically(path, replacement);
            return true;
        });
    }

    private static void requireStableIdentity(WorldTaskCraftAttempt current, WorldTaskCraftAttempt replacement) {
        if (!current.playerId().equals(replacement.playerId())
                || !current.recipeId().equals(replacement.recipeId())
                || current.quantity() != replacement.quantity()
                || !current.ingredientReservations().equals(replacement.ingredientReservations())
                || !current.outputItemInstanceId().equals(replacement.outputItemInstanceId())) {
            throw new IllegalArgumentException("craft attempt immutable identity changed");
        }
    }

    private WorldTaskCraftAttempt read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid craft attempt file magic");
                int schema = input.readInt();
                if (schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported craft attempt schema version: " + schema);
                }
                String attemptId = input.readUTF();
                String playerId = input.readUTF();
                String recipeId = input.readUTF();
                int quantity = input.readInt();
                WorldTaskCraftAttempt.Phase phase = WorldTaskCraftAttempt.Phase.valueOf(input.readUTF());
                int reservationCount = input.readInt();
                if (reservationCount <= 0 || reservationCount > 4096) {
                    throw new IllegalStateException("invalid craft reservation count");
                }
                List<WorldTaskCraftAttempt.PlannedReservation> reservations = new ArrayList<>(reservationCount);
                for (int index = 0; index < reservationCount; index++) {
                    reservations.add(new WorldTaskCraftAttempt.PlannedReservation(
                            input.readUTF(),
                            input.readUTF(),
                            input.readUTF(),
                            input.readInt(),
                            input.readLong()
                    ));
                }
                int rollPercent = input.readInt();
                WorldTaskRecipeDefinition.CraftQuality quality = input.readBoolean()
                        ? WorldTaskRecipeDefinition.CraftQuality.valueOf(input.readUTF())
                        : null;
                String outputItemInstanceId = input.readUTF();
                String outputTemplateId = input.readBoolean() ? input.readUTF() : null;
                int outputQuantity = input.readInt();
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing craft attempt data");
                return new WorldTaskCraftAttempt(
                        attemptId,
                        playerId,
                        recipeId,
                        quantity,
                        phase,
                        reservations,
                        rollPercent,
                        quality,
                        outputItemInstanceId,
                        outputTemplateId,
                        outputQuantity
                );
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read craft attempt", error);
        }
    }

    private void writeAtomically(Path target, WorldTaskCraftAttempt attempt) {
        byte[] encoded = encode(attempt);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(attemptsDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("craft attempt store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist craft attempt", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only. The target is authoritative.
                }
            }
        }
    }

    private byte[] encode(WorldTaskCraftAttempt attempt) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(attempt.attemptId());
                output.writeUTF(attempt.playerId());
                output.writeUTF(attempt.recipeId());
                output.writeInt(attempt.quantity());
                output.writeUTF(attempt.phase().name());
                output.writeInt(attempt.ingredientReservations().size());
                for (WorldTaskCraftAttempt.PlannedReservation reservation : attempt.ingredientReservations()) {
                    output.writeUTF(reservation.reservationId());
                    output.writeUTF(reservation.itemInstanceId());
                    output.writeUTF(reservation.itemTemplateId());
                    output.writeInt(reservation.quantity());
                    output.writeLong(reservation.itemRevision());
                }
                output.writeInt(attempt.rollPercent());
                output.writeBoolean(attempt.quality() != null);
                if (attempt.quality() != null) output.writeUTF(attempt.quality().name());
                output.writeUTF(attempt.outputItemInstanceId());
                output.writeBoolean(attempt.outputTemplateId() != null);
                if (attempt.outputTemplateId() != null) output.writeUTF(attempt.outputTemplateId());
                output.writeInt(attempt.outputQuantity());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode craft attempt", error);
        }
    }

    private <T> T withLock(String attemptId, IoSupplier<T> operation) {
        Path lockPath = lockPath(attemptId);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("craft attempt store operation failed", error);
        } finally {
            processLock.unlock();
        }
    }

    private Path statePath(String attemptId) {
        return attemptsDirectory.resolve(fileKey(attemptId) + ".bin");
    }

    private Path lockPath(String attemptId) {
        return attemptsDirectory.resolve(fileKey(attemptId) + ".lock");
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
        if (value == null || value.isBlank()) throw new IllegalArgumentException("attemptId must not be blank");
        return value.trim();
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
