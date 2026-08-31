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

/** Durable owner-scoped Gym/League registration state. No battle outcomes are stored here. */
public final class FileCanonicalLeagueRegistrationRepository {
    private static final int MAGIC = 0x414C4752; // ALGR
    static final int SCHEMA_VERSION = 1;
    private static final int MAX_REGISTRATIONS = 128;
    private final Path directory;

    public FileCanonicalLeagueRegistrationRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("league-registrations");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical league registration store", error);
        }
    }

    public synchronized RegistrationState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        RegistrationState created = new RegistrationState(owner, List.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<RegistrationState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    public synchronized boolean replaceIfRevision(RegistrationState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        String owner = requireId(replacement.playerId(), "playerId");
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        RegistrationState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private RegistrationState read(Path path, String expectedOwner) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical league registration file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical league registration schema version");
                String owner = input.readUTF();
                int count = input.readInt();
                if (count < 0 || count > MAX_REGISTRATIONS) throw new IllegalStateException("invalid league registration count");
                ArrayList<String> ids = new ArrayList<>(count);
                for (int i = 0; i < count; i++) ids.add(requireId(input.readUTF(), "challengeId"));
                long revision = input.readLong();
                RegistrationState state = new RegistrationState(owner, ids, revision);
                if (!state.playerId().equals(expectedOwner)) throw new IllegalStateException("canonical league registration owner mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical league registration data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical league registration state", error);
        }
    }

    private void writeAtomically(Path target, RegistrationState state) {
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
                throw new IllegalStateException("canonical league registration store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical league registration state", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static byte[] encode(RegistrationState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeInt(state.challengeIds().size());
                for (String id : state.challengeIds()) output.writeUTF(id);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical league registration state", error);
        }
    }

    private Path statePath(String playerId) {
        return directory.resolve(fileKey(playerId) + ".bin");
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
        return value.strip();
    }

    public record RegistrationState(String playerId, List<String> challengeIds, long revision) {
        public RegistrationState {
            playerId = requireId(playerId, "playerId");
            challengeIds = List.copyOf(challengeIds == null ? List.of() : challengeIds);
            if (challengeIds.size() > MAX_REGISTRATIONS) throw new IllegalArgumentException("too many league registrations");
            if (challengeIds.stream().distinct().count() != challengeIds.size()) throw new IllegalArgumentException("duplicate league registration");
            for (String challengeId : challengeIds) requireId(challengeId, "challengeId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
