package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/** Durable owner-scoped reputation state for server-authored RPG factions. */
public final class FileCanonicalFactionReputationRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41544652; // ATFR
    private final Path reputationDirectory;

    public FileCanonicalFactionReputationRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        reputationDirectory = rootDirectory.toAbsolutePath().normalize().resolve("faction-reputation");
        try {
            Files.createDirectories(reputationDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical faction reputation store", error);
        }
    }

    /** Creates only a neutral baseline; it does not imply membership, favor, hostility, or unlocks. */
    public synchronized ReputationState findOrCreate(String playerId, String factionId) {
        String owner = requireId(playerId, "playerId");
        String faction = requireId(factionId, "factionId");
        Path path = statePath(owner, faction);
        if (Files.exists(path)) return read(path, owner, faction);
        ReputationState created = new ReputationState(owner, faction, 0, 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<ReputationState> find(String playerId, String factionId) {
        String owner = requireId(playerId, "playerId");
        String faction = requireId(factionId, "factionId");
        Path path = statePath(owner, faction);
        return Files.exists(path) ? Optional.of(read(path, owner, faction)) : Optional.empty();
    }

    /** Revision-CAS boundary for a future server-authorized reputation event service. */
    public synchronized boolean replaceIfRevision(ReputationState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        String faction = requireId(replacement.factionId(), "factionId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner, faction);
        if (!Files.exists(path)) return false;
        ReputationState current = read(path, owner, faction);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private ReputationState read(Path path, String expectedPlayerId, String expectedFactionId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical faction reputation file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical faction reputation schema version");
                ReputationState state = new ReputationState(input.readUTF(), input.readUTF(), input.readInt(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId) || !state.factionId().equals(expectedFactionId)) {
                    throw new IllegalStateException("canonical faction reputation identity mismatch");
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical faction reputation data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical faction reputation", error);
        }
    }

    private void writeAtomically(Path target, ReputationState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(reputationDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical faction reputation store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical faction reputation", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(ReputationState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.factionId());
                output.writeInt(state.reputation());
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical faction reputation", error);
        }
    }

    private Path statePath(String playerId, String factionId) {
        return reputationDirectory.resolve(fileKey(playerId + "\u0000" + factionId) + ".bin");
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

    public record ReputationState(String playerId, String factionId, int reputation, long revision) {
        public ReputationState {
            playerId = requireId(playerId, "playerId");
            factionId = requireId(factionId, "factionId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
