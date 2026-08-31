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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Durable owner-scoped Trainer presentation state. Contains no PTU stats or progression authority. */
public final class FileCanonicalTrainerProfileRepository {
    private static final int MAGIC = 0x41545046; // ATPF
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public FileCanonicalTrainerProfileRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("trainer-profiles");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical trainer profile store", error);
        }
    }

    public synchronized TrainerProfile findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        TrainerProfile created = new TrainerProfile(owner, "rookie", "classic", 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized boolean replaceIfRevision(TrainerProfile replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        TrainerProfile current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private TrainerProfile read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical trainer profile file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical trainer profile schema version");
                TrainerProfile state = new TrainerProfile(input.readUTF(), input.readUTF(), input.readUTF(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical trainer profile identity mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical trainer profile data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical trainer profile", error);
        }
    }

    private byte[] encode(TrainerProfile state) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.titleId());
                output.writeUTF(state.cardThemeId());
                output.writeLong(state.revision());
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
    }

    private void writeAtomically(Path target, TrainerProfile state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical trainer profile store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical trainer profile", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private Path statePath(String playerId) {
        return directory.resolve(sha256(playerId) + ".bin").normalize();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        if (value.length() > 256) throw new IllegalArgumentException(field + " is too long");
        return value;
    }

    public record TrainerProfile(String playerId, String titleId, String cardThemeId, long revision) {
        public TrainerProfile {
            playerId = requireId(playerId, "playerId");
            titleId = requireId(titleId, "titleId");
            cardThemeId = requireId(cardThemeId, "cardThemeId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
