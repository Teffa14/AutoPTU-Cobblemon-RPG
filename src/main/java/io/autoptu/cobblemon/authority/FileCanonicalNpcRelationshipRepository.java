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

/** Durable owner-scoped RPG relationship state for server-authored NPC identities. */
public final class FileCanonicalNpcRelationshipRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41544E52; // ATNR
    private final Path relationshipDirectory;

    public FileCanonicalNpcRelationshipRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        relationshipDirectory = rootDirectory.toAbsolutePath().normalize().resolve("npc-relationships");
        try {
            Files.createDirectories(relationshipDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical NPC relationship store", error);
        }
    }

    /** Creates only a neutral RPG baseline. It does not imply that the Trainer has met the NPC. */
    public synchronized RelationshipState findOrCreate(String playerId, String npcId) {
        String owner = requireId(playerId, "playerId");
        String npc = requireId(npcId, "npcId");
        Path path = statePath(owner, npc);
        if (Files.exists(path)) return read(path, owner, npc);
        RelationshipState created = new RelationshipState(owner, npc, false, 0, 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<RelationshipState> find(String playerId, String npcId) {
        String owner = requireId(playerId, "playerId");
        String npc = requireId(npcId, "npcId");
        Path path = statePath(owner, npc);
        return Files.exists(path) ? Optional.of(read(path, owner, npc)) : Optional.empty();
    }

    /** Revision-CAS boundary. Reputation values must already have been authorized by an RPG service. */
    public synchronized boolean replaceIfRevision(RelationshipState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        String npc = requireId(replacement.npcId(), "npcId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner, npc);
        if (!Files.exists(path)) return false;
        RelationshipState current = read(path, owner, npc);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private RelationshipState read(Path path, String expectedPlayerId, String expectedNpcId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical NPC relationship file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical NPC relationship schema version");
                RelationshipState state = new RelationshipState(
                        input.readUTF(), input.readUTF(), input.readBoolean(), input.readInt(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId) || !state.npcId().equals(expectedNpcId)) {
                    throw new IllegalStateException("canonical NPC relationship identity mismatch");
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical NPC relationship data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical NPC relationship", error);
        }
    }

    private void writeAtomically(Path target, RelationshipState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(relationshipDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical NPC relationship store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical NPC relationship", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(RelationshipState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.npcId());
                output.writeBoolean(state.met());
                output.writeInt(state.reputation());
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical NPC relationship", error);
        }
    }

    private Path statePath(String playerId, String npcId) {
        return relationshipDirectory.resolve(fileKey(playerId + "\u0000" + npcId) + ".bin");
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

    public record RelationshipState(String playerId, String npcId, boolean met, int reputation, long revision) {
        public RelationshipState {
            playerId = requireId(playerId, "playerId");
            npcId = requireId(npcId, "npcId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
