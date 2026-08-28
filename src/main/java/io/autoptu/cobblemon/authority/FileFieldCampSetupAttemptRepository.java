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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable world-save journal for physical field-camp establishment. */
public final class FileFieldCampSetupAttemptRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41504643; // APFC
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();
    private final Path attemptsDirectory;

    public FileFieldCampSetupAttemptRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        attemptsDirectory = rootDirectory.toAbsolutePath().normalize().resolve("field-camp-attempts");
        try {
            Files.createDirectories(attemptsDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create field camp attempt store", error);
        }
    }

    public Optional<FieldCampSetupAttempt> find(String attemptId) {
        String id = requireId(attemptId);
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        FieldCampSetupAttempt attempt = read(path);
        if (!attempt.attemptId().equals(id)) throw new IllegalStateException("field camp attempt identity mismatch");
        return Optional.of(attempt);
    }

    public boolean createIfAbsent(FieldCampSetupAttempt attempt) {
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
            FieldCampSetupAttempt.Phase expectedPhase,
            FieldCampSetupAttempt replacement
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
            FieldCampSetupAttempt current = read(path);
            if (current.phase() != expectedPhase) return false;
            requireStableIdentity(current, replacement);
            writeAtomically(path, replacement);
            return true;
        });
    }

    private static void requireStableIdentity(FieldCampSetupAttempt current, FieldCampSetupAttempt replacement) {
        if (!current.campId().equals(replacement.campId())
                || !current.establishedByPlayerId().equals(replacement.establishedByPlayerId())
                || !current.taskId().equals(replacement.taskId())
                || current.canonicalSkillRank() != replacement.canonicalSkillRank()
                || current.improvisedPercent() != replacement.improvisedPercent()
                || current.standardPercent() != replacement.standardPercent()
                || current.excellentPercent() != replacement.excellentPercent()) {
            throw new IllegalArgumentException("field camp attempt immutable identity changed");
        }
    }

    private FieldCampSetupAttempt read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid field camp attempt file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported field camp attempt schema version");
                FieldCampSetupAttempt attempt = new FieldCampSetupAttempt(
                        input.readUTF(),
                        input.readUTF(),
                        input.readUTF(),
                        input.readUTF(),
                        FieldCampSetupAttempt.Phase.valueOf(input.readUTF()),
                        input.readInt(),
                        input.readInt(),
                        input.readInt(),
                        input.readInt(),
                        input.readInt(),
                        input.readBoolean() ? FieldCampSetupAttempt.Quality.valueOf(input.readUTF()) : null
                );
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing field camp attempt data");
                return attempt;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read field camp attempt", error);
        }
    }

    private void writeAtomically(Path target, FieldCampSetupAttempt attempt) {
        byte[] encoded = encode(attempt);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(attemptsDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("field camp attempt store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist field camp attempt", error);
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

    private byte[] encode(FieldCampSetupAttempt attempt) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(attempt.attemptId());
                output.writeUTF(attempt.campId());
                output.writeUTF(attempt.establishedByPlayerId());
                output.writeUTF(attempt.taskId());
                output.writeUTF(attempt.phase().name());
                output.writeInt(attempt.canonicalSkillRank());
                output.writeInt(attempt.improvisedPercent());
                output.writeInt(attempt.standardPercent());
                output.writeInt(attempt.excellentPercent());
                output.writeInt(attempt.rollPercent());
                output.writeBoolean(attempt.quality() != null);
                if (attempt.quality() != null) output.writeUTF(attempt.quality().name());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode field camp attempt", error);
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
            throw new UncheckedIOException("field camp attempt store operation failed", error);
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
