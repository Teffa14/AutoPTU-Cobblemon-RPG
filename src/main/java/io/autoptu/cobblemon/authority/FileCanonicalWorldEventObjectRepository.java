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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Durable world-scoped state for explicitly authored Ouros world-event objects. */
public final class FileCanonicalWorldEventObjectRepository {
    private static final int MAGIC = 0x41505745; // APWE
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public FileCanonicalWorldEventObjectRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("world-event-objects");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create world event object store", error);
        }
    }

    public synchronized State findOrCreate(String objectId, String eventKey) {
        String object = requireId(objectId, "objectId");
        String event = requireId(eventKey, "eventKey");
        Path path = statePath(object);
        if (Files.exists(path)) {
            State existing = read(path, object);
            if (!existing.eventKey().equals(event)) throw new IllegalStateException("world event object event key mismatch");
            return existing;
        }
        State created = new State(object, event, Phase.DORMANT, 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<State> find(String objectId) {
        String object = requireId(objectId, "objectId");
        Path path = statePath(object);
        return Files.exists(path) ? Optional.of(read(path, object)) : Optional.empty();
    }

    /** Returns a stable snapshot of all persisted authored world-event objects for restart reconciliation. */
    public synchronized List<State> findAll() {
        try (var paths = Files.list(directory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".bin"))
                    .map(this::readAny)
                    .sorted(Comparator.comparing(State::objectId))
                    .toList();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to list world event object state", error);
        }
    }

    public synchronized MutationResult activate(String objectId, String eventKey, long expectedRevision) {
        String object = requireId(objectId, "objectId");
        String event = requireId(eventKey, "eventKey");
        if (expectedRevision < 0L) throw new IllegalArgumentException("expectedRevision must not be negative");
        State current = findOrCreate(object, event);
        if (current.phase() == Phase.ACTIVATED) return new MutationResult(Status.ALREADY_ACTIVE, current);
        if (current.revision() != expectedRevision) return new MutationResult(Status.STALE_REVISION, current);
        State updated = new State(object, event, Phase.ACTIVATED, Math.addExact(current.revision(), 1L));
        writeAtomically(statePath(object), updated);
        return new MutationResult(Status.ACTIVATED, updated);
    }

    private State read(Path path, String expectedObjectId) {
        State state = readAny(path);
        if (!state.objectId().equals(expectedObjectId)) throw new IllegalStateException("world event object identity mismatch");
        return state;
    }

    private State readAny(Path path) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid world event object file magic");
            if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported world event object schema version");
            String objectId = requireId(input.readUTF(), "objectId");
            String eventKey = requireId(input.readUTF(), "eventKey");
            Phase phase = Phase.valueOf(input.readUTF());
            long revision = input.readLong();
            if (input.available() != 0) throw new IllegalStateException("unexpected trailing world event object data");
            return new State(objectId, eventKey, phase, revision);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read world event object state", error);
        }
    }

    private void writeAtomically(Path target, State state) {
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
                throw new IllegalStateException("world event object store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist world event object state", error);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private byte[] encode(State state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.objectId());
                output.writeUTF(state.eventKey());
                output.writeUTF(state.phase().name());
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode world event object state", error);
        }
    }

    private Path statePath(String objectId) { return directory.resolve(fileKey(objectId) + ".bin"); }

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

    public enum Phase { DORMANT, ACTIVATED }
    public enum Status { ACTIVATED, ALREADY_ACTIVE, STALE_REVISION }

    public record State(String objectId, String eventKey, Phase phase, long revision) {
        public State {
            objectId = requireId(objectId, "objectId");
            eventKey = requireId(eventKey, "eventKey");
            if (phase == null) throw new IllegalArgumentException("phase is required");
            if (revision < 0L) throw new IllegalArgumentException("revision must not be negative");
        }
    }

    public record MutationResult(Status status, State state) {
        public MutationResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (state == null) throw new IllegalArgumentException("state is required");
        }
    }
}
