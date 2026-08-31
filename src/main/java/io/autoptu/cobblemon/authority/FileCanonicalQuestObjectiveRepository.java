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
import java.util.LinkedHashSet;
import java.util.Set;

/** Durable owner-scoped idempotent quest-objective completion ledger. */
public final class FileCanonicalQuestObjectiveRepository {
    private static final int MAGIC = 0x4150514f; // APQO
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public FileCanonicalQuestObjectiveRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("quest-objectives");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new UncheckedIOException("failed to create canonical quest objective store", error); }
    }

    public synchronized ObjectiveState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        ObjectiveState created = new ObjectiveState(owner, 0L, Set.of());
        writeAtomically(path, created);
        return created;
    }

    public synchronized CompleteResult complete(String playerId, String questId, String objectiveId, long expectedRevision) {
        String owner = requireId(playerId, "playerId");
        String key = key(requireId(questId, "questId"), requireId(objectiveId, "objectiveId"));
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        ObjectiveState current = findOrCreate(owner);
        if (current.completedObjectives().contains(key)) return new CompleteResult(CompleteStatus.ALREADY_COMPLETED, current);
        if (current.revision() != expectedRevision) return new CompleteResult(CompleteStatus.STALE_REVISION, current);
        LinkedHashSet<String> completed = new LinkedHashSet<>(current.completedObjectives());
        completed.add(key);
        ObjectiveState updated = new ObjectiveState(owner, Math.addExact(current.revision(), 1L), completed);
        writeAtomically(statePath(owner), updated);
        return new CompleteResult(CompleteStatus.COMPLETED, updated);
    }

    public static String key(String questId, String objectiveId) { return requireId(questId, "questId") + "/" + requireId(objectiveId, "objectiveId"); }

    private ObjectiveState read(Path path, String expectedPlayerId) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical quest objective file magic");
            if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical quest objective schema version");
            String playerId = input.readUTF();
            if (!playerId.equals(expectedPlayerId)) throw new IllegalStateException("canonical quest objective owner mismatch");
            long revision = input.readLong();
            int count = input.readInt();
            if (count < 0) throw new IllegalStateException("invalid canonical quest objective count");
            LinkedHashSet<String> completed = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) if (!completed.add(input.readUTF())) throw new IllegalStateException("duplicate canonical quest objective");
            if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical quest objective data");
            return new ObjectiveState(playerId, revision, completed);
        } catch (IOException error) { throw new UncheckedIOException("failed to read canonical quest objective state", error); }
    }

    private void writeAtomically(Path target, ObjectiveState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { throw new IllegalStateException("canonical quest objective store requires atomic file replacement", error); }
        } catch (IOException error) { throw new UncheckedIOException("failed to persist canonical quest objective state", error); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }

    private byte[] encode(ObjectiveState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeLong(state.revision());
                output.writeInt(state.completedObjectives().size());
                for (String completed : state.completedObjectives()) output.writeUTF(completed);
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) { throw new UncheckedIOException("failed to encode canonical quest objective state", error); }
    }

    private Path statePath(String playerId) { return directory.resolve(fileKey(playerId) + ".bin"); }

    private static String fileKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 is required by the Java runtime", error); }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public record ObjectiveState(String playerId, long revision, Set<String> completedObjectives) {
        public ObjectiveState {
            playerId = requireId(playerId, "playerId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
            completedObjectives = Set.copyOf(completedObjectives);
        }
    }
    public enum CompleteStatus { COMPLETED, ALREADY_COMPLETED, STALE_REVISION }
    public record CompleteResult(CompleteStatus status, ObjectiveState state) {}
}
