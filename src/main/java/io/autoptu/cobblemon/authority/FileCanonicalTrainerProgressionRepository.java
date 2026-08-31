package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Durable server-owned Trainer progression state.
 *
 * <p>This store intentionally persists only explicit progression facts. It does not calculate PTU
 * level thresholds, award XP, unlock Features, alter combat statistics, or infer progression from
 * Minecraft/Cobblemon state.</p>
 */
public final class FileCanonicalTrainerProgressionRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41545047; // ATPG
    private final Path progressionDirectory;

    public FileCanonicalTrainerProgressionRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        progressionDirectory = rootDirectory.toAbsolutePath().normalize().resolve("trainer-progression");
        try {
            Files.createDirectories(progressionDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical Trainer progression store", error);
        }
    }

    /** Creates the neutral server-owned baseline only when no progression record exists. */
    public synchronized ProgressionState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        ProgressionState created = new ProgressionState(owner, 1, 0L, 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<ProgressionState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    /**
     * Revision-CAS write boundary for a future server-authorized progression service.
     * Callers must provide the complete resulting explicit state; no level-up policy lives here.
     */
    public synchronized boolean replaceIfRevision(ProgressionState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        ProgressionState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private ProgressionState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical Trainer progression file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical Trainer progression schema version");
                ProgressionState state = new ProgressionState(input.readUTF(), input.readInt(), input.readLong(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical Trainer progression owner mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical Trainer progression data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical Trainer progression", error);
        }
    }

    private void writeAtomically(Path target, ProgressionState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(progressionDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical Trainer progression store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical Trainer progression", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(ProgressionState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeInt(state.trainerLevel());
                output.writeLong(state.trainerXp());
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical Trainer progression", error);
        }
    }

    private Path statePath(String playerId) {
        return progressionDirectory.resolve(fileKey(playerId) + ".bin");
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

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public record ProgressionState(String playerId, int trainerLevel, long trainerXp, long revision) {
        public ProgressionState {
            playerId = requireId(playerId, "playerId");
            if (trainerLevel < 1) throw new IllegalArgumentException("trainerLevel must be positive");
            if (trainerXp < 0) throw new IllegalArgumentException("trainerXp must not be negative");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
