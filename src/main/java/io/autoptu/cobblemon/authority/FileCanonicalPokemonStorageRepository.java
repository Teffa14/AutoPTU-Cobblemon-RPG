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
import java.util.Optional;

/** World-save-scoped durable boxed Pokemon identity store. */
public final class FileCanonicalPokemonStorageRepository implements VersionedCanonicalPokemonStorageRepository {
    private static final int MAGIC = 0x41505354; // APST
    private static final int SCHEMA_VERSION = 1;
    private final Path storageDirectory;

    public FileCanonicalPokemonStorageRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        storageDirectory = rootDirectory.toAbsolutePath().normalize().resolve("pokemon-storage");
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical Pokemon storage", error);
        }
    }

    @Override
    public synchronized Optional<CanonicalPokemonStorageState> findStorage(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    @Override
    public synchronized CanonicalPokemonStorageState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        CanonicalPokemonStorageState created = new CanonicalPokemonStorageState(owner, java.util.List.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    @Override
    public synchronized boolean replaceIfRevision(
            String playerId,
            long expectedRevision,
            CanonicalPokemonStorageState replacement
    ) {
        String owner = requireId(playerId, "playerId");
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.playerId().equals(owner)) throw new IllegalArgumentException("storage owner cannot change");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must be >= 0");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        CanonicalPokemonStorageState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private CanonicalPokemonStorageState read(Path path, String expectedOwner) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical Pokemon storage magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical Pokemon storage schema");
                String owner = input.readUTF();
                long revision = input.readLong();
                int count = input.readInt();
                if (count < 0) throw new IllegalStateException("invalid canonical Pokemon storage count");
                ArrayList<String> pokemonIds = new ArrayList<>(count);
                for (int i = 0; i < count; i++) pokemonIds.add(input.readUTF());
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical Pokemon storage data");
                CanonicalPokemonStorageState state = new CanonicalPokemonStorageState(owner, pokemonIds, revision);
                if (!state.playerId().equals(expectedOwner)) throw new IllegalStateException("canonical Pokemon storage owner mismatch");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical Pokemon storage", error);
        }
    }

    private void writeAtomically(Path target, CanonicalPokemonStorageState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(storageDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical Pokemon storage requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical Pokemon storage", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static byte[] encode(CanonicalPokemonStorageState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeLong(state.revision());
                output.writeInt(state.pokemonIds().size());
                for (String pokemonId : state.pokemonIds()) output.writeUTF(pokemonId);
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical Pokemon storage", error);
        }
    }

    private Path statePath(String playerId) {
        return storageDirectory.resolve(fileKey(playerId) + ".bin");
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
}
