package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable one-file-per-Pokemon store for server-owned nickname metadata. */
public final class FileCanonicalPokemonNicknameRepository implements VersionedCanonicalPokemonNicknameRepository {
    private static final int MAGIC = 0x41504E4B; // APNK
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();
    private final Path directory;

    public FileCanonicalPokemonNicknameRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("pokemon-nicknames");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new UncheckedIOException("failed to create Pokemon nickname store", error); }
    }

    @Override
    public Optional<CanonicalPokemonNicknameState> findNickname(String pokemonId) {
        String id = requireId(pokemonId);
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        CanonicalPokemonNicknameState state = read(path);
        if (!state.pokemonId().equals(id)) throw new IllegalStateException("nickname file identity mismatch");
        return Optional.of(state);
    }

    @Override
    public boolean createNicknameIfAbsent(CanonicalPokemonNicknameState initialState) {
        if (initialState == null) throw new IllegalArgumentException("initialState is required");
        String id = requireId(initialState.pokemonId());
        return withLock(id, () -> {
            Path path = statePath(id);
            if (Files.exists(path)) return false;
            writeAtomically(path, initialState);
            return true;
        });
    }

    @Override
    public boolean replaceNicknameIfRevision(String pokemonId, long expectedRevision, CanonicalPokemonNicknameState replacement) {
        String id = requireId(pokemonId);
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) throw new IllegalArgumentException("invalid expectedRevision");
        if (replacement == null || !replacement.pokemonId().equals(id)) throw new IllegalArgumentException("replacement identity must match");
        if (replacement.revision() != expectedRevision + 1) throw new IllegalArgumentException("replacement revision must advance exactly once");
        return withLock(id, () -> {
            Path path = statePath(id);
            if (!Files.exists(path)) return false;
            CanonicalPokemonNicknameState current = read(path);
            if (!current.pokemonId().equals(id) || current.revision() != expectedRevision) return false;
            writeAtomically(path, replacement);
            return true;
        });
    }

    private CanonicalPokemonNicknameState read(Path path) {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid Pokemon nickname file magic");
            int schema = input.readInt();
            if (schema != SCHEMA_VERSION) throw new IllegalStateException("unsupported Pokemon nickname schema: " + schema);
            CanonicalPokemonNicknameState state = new CanonicalPokemonNicknameState(input.readUTF(), input.readUTF(), input.readUTF(), input.readLong());
            if (input.read() != -1) throw new IllegalStateException("unexpected trailing Pokemon nickname data");
            return state;
        } catch (IOException error) { throw new UncheckedIOException("failed to read Pokemon nickname", error); }
    }

    private void writeAtomically(Path target, CanonicalPokemonNicknameState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 DataOutputStream output = new DataOutputStream(java.nio.channels.Channels.newOutputStream(channel))) {
                output.writeInt(MAGIC); output.writeInt(SCHEMA_VERSION); output.writeUTF(state.pokemonId());
                output.writeUTF(state.ownerPlayerId()); output.writeUTF(state.nickname()); output.writeLong(state.revision());
                output.flush(); channel.force(true);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { throw new IllegalStateException("nickname store requires atomic replacement", error); }
        } catch (IOException error) { throw new UncheckedIOException("failed to persist Pokemon nickname", error); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {} }
    }

    private <T> T withLock(String pokemonId, IoSupplier<T> operation) {
        Path lockPath = directory.resolve(hash(pokemonId) + ".lock");
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) { throw new UncheckedIOException("failed to lock Pokemon nickname state", error); }
        finally { processLock.unlock(); }
    }

    private Path statePath(String pokemonId) { return directory.resolve(hash(pokemonId) + ".bin"); }
    private static String requireId(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("pokemonId must not be blank"); return value.strip(); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 unavailable", error); }
    }
    @FunctionalInterface private interface IoSupplier<T> { T get(); }
}
