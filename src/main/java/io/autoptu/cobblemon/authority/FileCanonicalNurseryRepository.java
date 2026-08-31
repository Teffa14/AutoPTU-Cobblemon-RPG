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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Durable owner-scoped custody state for server-authored Pokemon nursery facilities. */
public final class FileCanonicalNurseryRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41544E52; // ATNR
    private final Path directory;

    public FileCanonicalNurseryRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("nursery-custody");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical nursery store", error);
        }
    }

    public synchronized NurseryState findOrCreate(String playerId, String facilityId) {
        String owner = requireId(playerId, "playerId");
        String facility = requireId(facilityId, "facilityId");
        Path path = statePath(owner, facility);
        if (Files.exists(path)) return read(path, owner, facility);
        NurseryState created = new NurseryState(owner, facility, List.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<NurseryState> find(String playerId, String facilityId) {
        String owner = requireId(playerId, "playerId");
        String facility = requireId(facilityId, "facilityId");
        Path path = statePath(owner, facility);
        return Files.exists(path) ? Optional.of(read(path, owner, facility)) : Optional.empty();
    }

    public synchronized List<NurseryState> findAll() {
        try {
            if (!Files.exists(directory)) return List.of();
            ArrayList<NurseryState> states = new ArrayList<>();
            try (var paths = Files.list(directory)) {
                for (Path path : paths.filter(candidate -> candidate.getFileName().toString().endsWith(".bin")).toList()) {
                    states.add(readUnverified(path));
                }
            }
            return List.copyOf(states);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to enumerate canonical nursery state", error);
        }
    }

    public synchronized boolean replaceIfRevision(NurseryState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        String owner = requireId(replacement.playerId(), "playerId");
        String facility = requireId(replacement.facilityId(), "facilityId");
        Path path = statePath(owner, facility);
        if (!Files.exists(path)) return false;
        NurseryState current = read(path, owner, facility);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private NurseryState read(Path path, String expectedPlayerId, String expectedFacilityId) {
        NurseryState state = readUnverified(path);
        if (!state.playerId().equals(expectedPlayerId) || !state.facilityId().equals(expectedFacilityId)) {
            throw new IllegalStateException("canonical nursery identity mismatch");
        }
        return state;
    }

    private NurseryState readUnverified(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical nursery file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical nursery schema version");
                String playerId = input.readUTF();
                String facilityId = input.readUTF();
                int count = input.readInt();
                if (count < 0 || count > 128) throw new IllegalStateException("invalid canonical nursery member count");
                ArrayList<String> ids = new ArrayList<>(count);
                for (int i = 0; i < count; i++) ids.add(input.readUTF());
                long revision = input.readLong();
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical nursery data");
                return new NurseryState(playerId, facilityId, ids, revision);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical nursery state", error);
        }
    }

    private void writeAtomically(Path target, NurseryState state) {
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
                throw new IllegalStateException("canonical nursery store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical nursery state", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(NurseryState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.facilityId());
                output.writeInt(state.pokemonIds().size());
                for (String pokemonId : state.pokemonIds()) output.writeUTF(pokemonId);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical nursery state", error);
        }
    }

    private Path statePath(String playerId, String facilityId) {
        return directory.resolve(fileKey(playerId + "\u0000" + facilityId) + ".bin");
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

    public record NurseryState(String playerId, String facilityId, List<String> pokemonIds, long revision) {
        public NurseryState {
            playerId = requireId(playerId, "playerId");
            facilityId = requireId(facilityId, "facilityId");
            if (pokemonIds == null) throw new IllegalArgumentException("pokemonIds is required");
            ArrayList<String> normalized = new ArrayList<>();
            for (String pokemonId : pokemonIds) {
                String id = requireId(pokemonId, "pokemonId");
                if (normalized.contains(id)) throw new IllegalArgumentException("nursery cannot contain duplicate Pokemon");
                normalized.add(id);
            }
            pokemonIds = List.copyOf(normalized);
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}