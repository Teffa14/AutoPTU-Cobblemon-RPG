package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Schema-versioned durable store for server-owned player encounter selections.
 *
 * The profile chooses canonical ids and a battle-grid placement only. BattleAuthorityService still
 * re-resolves Pokemon/items and verifies ownership, revisions and quantities before a reservation
 * can succeed. Minecraft/Cobblemon data never becomes asset truth through this store.
 */
public final class FileCanonicalPlayerEncounterProfileRepository
        implements VersionedCanonicalPlayerEncounterProfileRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41504550; // APEP
    private static final int MAX_COLLECTION_SIZE = 100_000;
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path profilesDirectory;

    public FileCanonicalPlayerEncounterProfileRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        profilesDirectory = rootDirectory.toAbsolutePath().normalize().resolve("encounter-profiles");
        try {
            Files.createDirectories(profilesDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical encounter profile store", error);
        }
    }

    @Override
    public Optional<CanonicalPlayerEncounterProfile> findProfile(String playerId) {
        String normalized = requirePlayerId(playerId);
        Path path = statePath(normalized);
        if (!Files.exists(path)) return Optional.empty();
        CanonicalPlayerEncounterProfile profile = read(path);
        if (!profile.playerId().equals(normalized)) {
            throw new IllegalStateException("canonical encounter profile identity mismatch");
        }
        return Optional.of(profile);
    }

    @Override
    public boolean createProfileIfAbsent(CanonicalPlayerEncounterProfile initialProfile) {
        if (initialProfile == null) throw new IllegalArgumentException("initialProfile is required");
        String playerId = requirePlayerId(initialProfile.playerId());
        return withPlayerLock(playerId, () -> {
            Path path = statePath(playerId);
            if (Files.exists(path)) return false;
            writeAtomically(path, initialProfile);
            return true;
        });
    }

    @Override
    public boolean replaceProfileIfRevision(
            String playerId,
            long expectedRevision,
            CanonicalPlayerEncounterProfile replacement
    ) {
        String normalized = requirePlayerId(playerId);
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow one revision advance");
        }
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.playerId().equals(normalized)) {
            throw new IllegalArgumentException("replacement player identity must match playerId");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must advance exactly once");
        }

        return withPlayerLock(normalized, () -> {
            Path path = statePath(normalized);
            if (!Files.exists(path)) return false;
            CanonicalPlayerEncounterProfile current = read(path);
            if (!current.playerId().equals(normalized)) {
                throw new IllegalStateException("canonical encounter profile identity mismatch");
            }
            if (current.revision() != expectedRevision) return false;
            writeAtomically(path, replacement);
            return true;
        });
    }

    private <T> T withPlayerLock(String playerId, IoSupplier<T> operation) {
        Path lockPath = lockPath(playerId);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        ); FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("canonical encounter profile store operation failed", error);
        } finally {
            processLock.unlock();
        }
    }

    private CanonicalPlayerEncounterProfile read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid encounter profile file magic");
                int schema = input.readInt();
                if (schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported encounter profile schema version: " + schema);
                }
                String playerId = input.readUTF();
                long revision = input.readLong();
                List<String> pokemonIds = readStringList(input);
                Map<String, Integer> consumables = readIntMap(input);
                BattleArenaSnapshot arena = new BattleArenaSnapshot(
                        input.readUTF(),
                        input.readInt(), input.readInt(), input.readInt(),
                        input.readInt(), input.readInt(), input.readInt(), input.readInt()
                );
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing encounter profile data");
                return new CanonicalPlayerEncounterProfile(playerId, pokemonIds, consumables, arena, revision);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical encounter profile", error);
        }
    }

    private void writeAtomically(Path path, CanonicalPlayerEncounterProfile profile) {
        byte[] encoded = encode(profile);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(profilesDirectory, path.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("encounter profile store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical encounter profile", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private byte[] encode(CanonicalPlayerEncounterProfile profile) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(profile.playerId());
                output.writeLong(profile.revision());
                output.writeInt(profile.pokemonIds().size());
                for (String pokemonId : profile.pokemonIds()) output.writeUTF(pokemonId);
                List<Map.Entry<String, Integer>> consumables = new ArrayList<>(profile.consumableQuantities().entrySet());
                consumables.sort(Map.Entry.comparingByKey());
                output.writeInt(consumables.size());
                for (Map.Entry<String, Integer> entry : consumables) {
                    output.writeUTF(entry.getKey());
                    output.writeInt(entry.getValue());
                }
                BattleArenaSnapshot arena = profile.arena();
                output.writeUTF(arena.dimensionId());
                output.writeInt(arena.originX());
                output.writeInt(arena.originY());
                output.writeInt(arena.originZ());
                output.writeInt(arena.gridXdx());
                output.writeInt(arena.gridXdz());
                output.writeInt(arena.gridYdx());
                output.writeInt(arena.gridYdz());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical encounter profile", error);
        }
    }

    private static List<String> readStringList(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        ArrayList<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) values.add(input.readUTF());
        return List.copyOf(values);
    }

    private static Map<String, Integer> readIntMap(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            String key = input.readUTF();
            int value = input.readInt();
            if (values.putIfAbsent(key, value) != null) throw new IllegalStateException("duplicate consumable id");
        }
        return Map.copyOf(values);
    }

    private static int readCollectionSize(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_COLLECTION_SIZE) throw new IllegalStateException("invalid encounter profile collection size");
        return size;
    }

    private Path statePath(String playerId) {
        return profilesDirectory.resolve(hashKey(playerId) + ".bin");
    }

    private Path lockPath(String playerId) {
        return profilesDirectory.resolve(hashKey(playerId) + ".lock");
    }

    private static String requirePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        return playerId.strip();
    }

    private static String hashKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
