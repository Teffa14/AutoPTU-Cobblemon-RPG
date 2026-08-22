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
import java.nio.charset.StandardCharsets;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Durable single-player aggregate store with schema-versioned binary records and OS-level write locks.
 *
 * <p>Each player is stored in its own atomically replaced file. Writers for the same player serialize
 * through a stable lock file, so independent repository instances in the same JVM or different JVMs
 * observe one compare-and-set winner for a given revision. The file contents are forced before the
 * atomic rename. This provides process-restart durability for the player aggregate while deliberately
 * making no cross-aggregate transaction claim for Pokemon/items.</p>
 */
public final class FileVersionedCanonicalStateRepository implements VersionedCanonicalStateRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41505455; // APTU
    private static final int MAX_COLLECTION_SIZE = 100_000;

    private final Path playersDirectory;

    public FileVersionedCanonicalStateRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        this.playersDirectory = rootDirectory.toAbsolutePath().normalize().resolve("players");
        try {
            Files.createDirectories(playersDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical player store", error);
        }
    }

    @Override
    public Optional<CanonicalPlayerState> findPlayer(String playerId) {
        String normalizedPlayerId = requirePlayerId(playerId);
        Path statePath = statePath(normalizedPlayerId);
        if (!Files.exists(statePath)) return Optional.empty();
        CanonicalPlayerState state = readState(statePath);
        if (!state.playerId().equals(normalizedPlayerId)) {
            throw new IllegalStateException("canonical player file identity mismatch");
        }
        return Optional.of(state);
    }

    /** Creates the initial server-owned aggregate only when no record exists for the player. */
    public boolean createPlayerIfAbsent(CanonicalPlayerState initialState) {
        if (initialState == null) throw new IllegalArgumentException("initialState is required");
        String playerId = requirePlayerId(initialState.playerId());
        return withPlayerLock(playerId, () -> {
            Path statePath = statePath(playerId);
            if (Files.exists(statePath)) return false;
            writeAtomically(statePath, initialState);
            return true;
        });
    }

    @Override
    public boolean replacePlayerIfRevision(
            String playerId,
            long expectedRevision,
            CanonicalPlayerState replacement
    ) {
        String normalizedPlayerId = requirePlayerId(playerId);
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow exactly one revision advance");
        }
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.playerId().equals(normalizedPlayerId)) {
            throw new IllegalArgumentException("replacement player identity must match playerId");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must advance expectedRevision exactly once");
        }

        return withPlayerLock(normalizedPlayerId, () -> {
            Path statePath = statePath(normalizedPlayerId);
            if (!Files.exists(statePath)) return false;
            CanonicalPlayerState current = readState(statePath);
            if (!current.playerId().equals(normalizedPlayerId)) {
                throw new IllegalStateException("canonical player file identity mismatch");
            }
            if (current.revision() != expectedRevision) return false;
            writeAtomically(statePath, replacement);
            return true;
        });
    }

    private <T> T withPlayerLock(String playerId, IoSupplier<T> operation) {
        Path lockPath = lockPath(playerId);
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        ); FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("canonical player store operation failed", error);
        }
    }

    private CanonicalPlayerState readState(Path statePath) {
        try {
            byte[] bytes = Files.readAllBytes(statePath);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical player file magic");
                int schemaVersion = input.readInt();
                if (schemaVersion != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical player schema version: " + schemaVersion);
                }
                String playerId = input.readUTF();
                long revision = input.readLong();
                Set<String> trainerClasses = readStringSet(input);
                Map<String, Integer> skillRanks = readIntMap(input);
                Set<String> capabilities = readStringSet(input);
                Set<String> trainerFeatures = readStringSet(input);
                int actionPoints = input.readInt();
                int initiativeModifier = input.readInt();
                Integer explicitInitiativeSpeed = input.readBoolean() ? input.readInt() : null;
                String teamId = input.readUTF();
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical player data");
                return new CanonicalPlayerState(
                        playerId,
                        trainerClasses,
                        skillRanks,
                        capabilities,
                        trainerFeatures,
                        actionPoints,
                        initiativeModifier,
                        explicitInitiativeSpeed,
                        teamId,
                        revision
                );
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical player state", error);
        }
    }

    private void writeAtomically(Path statePath, CanonicalPlayerState state) {
        byte[] encoded = encode(state);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(playersDirectory, statePath.getFileName().toString() + ".", ".tmp");
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
                Files.move(
                        temporary,
                        statePath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical player store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical player state", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only. The authoritative target was either atomically replaced or untouched.
                }
            }
        }
    }

    private byte[] encode(CanonicalPlayerState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeLong(state.revision());
                writeStringSet(output, state.trainerClasses());
                writeIntMap(output, state.skillRanks());
                writeStringSet(output, state.availablePokemonCapabilities());
                writeStringSet(output, state.trainerFeatures());
                output.writeInt(state.actionPoints());
                output.writeInt(state.initiativeModifier());
                output.writeBoolean(state.explicitInitiativeSpeed() != null);
                if (state.explicitInitiativeSpeed() != null) output.writeInt(state.explicitInitiativeSpeed());
                output.writeUTF(state.teamId());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical player state", error);
        }
    }

    private static void writeStringSet(DataOutputStream output, Set<String> values) throws IOException {
        List<String> ordered = new ArrayList<>(values);
        ordered.sort(Comparator.naturalOrder());
        output.writeInt(ordered.size());
        for (String value : ordered) output.writeUTF(value);
    }

    private static Set<String> readStringSet(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) values.add(input.readUTF());
        if (values.size() != size) throw new IllegalStateException("duplicate canonical set entry");
        return Set.copyOf(values);
    }

    private static void writeIntMap(DataOutputStream output, Map<String, Integer> values) throws IOException {
        List<Map.Entry<String, Integer>> ordered = new ArrayList<>(values.entrySet());
        ordered.sort(Map.Entry.comparingByKey());
        output.writeInt(ordered.size());
        for (Map.Entry<String, Integer> entry : ordered) {
            output.writeUTF(entry.getKey());
            output.writeInt(entry.getValue());
        }
    }

    private static Map<String, Integer> readIntMap(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            String key = input.readUTF();
            if (values.put(key, input.readInt()) != null) throw new IllegalStateException("duplicate canonical map key");
        }
        return Map.copyOf(values);
    }

    private static int readCollectionSize(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_COLLECTION_SIZE) {
            throw new IllegalStateException("invalid canonical collection size: " + size);
        }
        return size;
    }

    private Path statePath(String playerId) {
        return playersDirectory.resolve(fileKey(playerId) + ".bin");
    }

    private Path lockPath(String playerId) {
        return playersDirectory.resolve(fileKey(playerId) + ".lock");
    }

    private static String fileKey(String playerId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(playerId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", error);
        }
    }

    private static String requirePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
        return playerId;
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
