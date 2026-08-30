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
import java.util.Optional;
import java.util.Set;

/** Durable owner-scoped discovery flags for server-authored RPG locations. */
public final class FileCanonicalLocationDiscoveryRepository {
    private static final int MAGIC = 0x41504c44; // APLD
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public FileCanonicalLocationDiscoveryRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("location-discoveries");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create location discovery store", error);
        }
    }

    public synchronized State findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        State created = new State(owner, 0L, Set.of());
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<State> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    public synchronized DiscoverResult discover(String playerId, String locationId, long expectedRevision) {
        String owner = requireId(playerId, "playerId");
        String location = requireId(locationId, "locationId");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        State current = findOrCreate(owner);
        if (current.locationIds().contains(location)) return new DiscoverResult(Status.ALREADY_DISCOVERED, current);
        if (current.revision() != expectedRevision) return new DiscoverResult(Status.STALE_REVISION, current);
        LinkedHashSet<String> locations = new LinkedHashSet<>(current.locationIds());
        locations.add(location);
        State updated = new State(owner, Math.addExact(current.revision(), 1L), locations);
        writeAtomically(statePath(owner), updated);
        return new DiscoverResult(Status.DISCOVERED, updated);
    }

    private State read(Path path, String expectedPlayerId) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid location discovery file magic");
            if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported location discovery schema version");
            String playerId = input.readUTF();
            if (!playerId.equals(expectedPlayerId)) throw new IllegalStateException("location discovery owner mismatch");
            long revision = input.readLong();
            int count = input.readInt();
            if (count < 0) throw new IllegalStateException("invalid location discovery count");
            LinkedHashSet<String> locations = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                if (!locations.add(requireId(input.readUTF(), "locationId"))) {
                    throw new IllegalStateException("duplicate persisted location discovery");
                }
            }
            if (input.available() != 0) throw new IllegalStateException("unexpected trailing location discovery data");
            return new State(playerId, revision, locations);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read location discovery state", error);
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
                throw new IllegalStateException("location discovery store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist location discovery state", error);
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
                output.writeUTF(state.playerId());
                output.writeLong(state.revision());
                output.writeInt(state.locationIds().size());
                for (String locationId : state.locationIds()) output.writeUTF(locationId);
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode location discovery state", error);
        }
    }

    private Path statePath(String playerId) { return directory.resolve(fileKey(playerId) + ".bin"); }

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

    public record State(String playerId, long revision, Set<String> locationIds) {
        public State {
            playerId = requireId(playerId, "playerId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
            if (locationIds == null) throw new IllegalArgumentException("locationIds are required");
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String locationId : locationIds) normalized.add(requireId(locationId, "locationId"));
            locationIds = Set.copyOf(normalized);
        }
    }

    public enum Status { DISCOVERED, ALREADY_DISCOVERED, STALE_REVISION }

    public record DiscoverResult(Status status, State state) {
        public DiscoverResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (state == null) throw new IllegalArgumentException("state is required");
        }
    }
}
