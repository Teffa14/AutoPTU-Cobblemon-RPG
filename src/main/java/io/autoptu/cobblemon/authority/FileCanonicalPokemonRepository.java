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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Durable schema-versioned store for complete server-owned canonical Pokemon aggregates.
 *
 * <p>One file is atomically replaced per Pokemon. Writes are serialized across repository
 * instances by an in-process lock and an OS file lock, then guarded by the aggregate revision.
 * Minecraft/Cobblemon data never participates in reconstruction of this record.</p>
 */
public final class FileCanonicalPokemonRepository implements VersionedCanonicalPokemonRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x4150504B; // APPK
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path pokemonDirectory;

    public FileCanonicalPokemonRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        pokemonDirectory = rootDirectory.toAbsolutePath().normalize().resolve("pokemon");
        try {
            Files.createDirectories(pokemonDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical Pokemon store", error);
        }
    }

    @Override
    public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
        String id = requireId(pokemonId);
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        CanonicalPokemonState state = read(path);
        requireStoredIdentity(id, state);
        return Optional.of(state);
    }

    @Override
    public boolean createPokemonIfAbsent(CanonicalPokemonState initialState) {
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
    public boolean replacePokemonIfRevision(
            String pokemonId,
            long expectedRevision,
            CanonicalPokemonState replacement
    ) {
        String id = requireId(pokemonId);
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow exactly one revision advance");
        }
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.pokemonId().equals(id)) {
            throw new IllegalArgumentException("replacement Pokemon identity must match pokemonId");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must advance expectedRevision exactly once");
        }

        return withLock(id, () -> {
            Path path = statePath(id);
            if (!Files.exists(path)) return false;
            CanonicalPokemonState current = read(path);
            requireStoredIdentity(id, current);
            if (current.revision() != expectedRevision) return false;
            writeAtomically(path, replacement);
            return true;
        });
    }

    private CanonicalPokemonState read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical Pokemon file magic");
                int schema = input.readInt();
                if (schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical Pokemon schema version: " + schema);
                }
                CanonicalPokemonState state = decodeState(input);
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical Pokemon data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical Pokemon state", error);
        }
    }

    private CanonicalPokemonState decodeState(DataInputStream input) throws IOException {
        String pokemonId = input.readUTF();
        String ownerPlayerId = input.readUTF();
        String speciesId = input.readUTF();
        int level = input.readInt();
        Set<String> capabilities = readStringSet(input);
        CanonicalStatusState statusState = readStatusState(input);
        Set<String> statuses = statusState.names();
        CanonicalCombatStats combatStats = readNullableCombatStats(input);
        CanonicalHealth health = readNullableHealth(input);
        CanonicalMoveLoadout moveLoadout = readNullableMoveLoadout(input);
        CanonicalBaseMovement baseMovement = readNullableBaseMovement(input);
        CanonicalBattleTraits battleTraits = readNullableBattleTraits(input);
        CanonicalAccuracyEvasion accuracyEvasion = readNullableAccuracyEvasion(input);
        CanonicalInjuryState injuryState = readNullableInjuryState(input);
        String heldItemInstanceId = readNullableString(input);
        long revision = input.readLong();
        return new CanonicalPokemonState(
                pokemonId, ownerPlayerId, speciesId, level, capabilities, statuses, statusState,
                combatStats, health, moveLoadout, baseMovement, battleTraits, accuracyEvasion,
                injuryState, heldItemInstanceId, revision);
    }

    private void writeAtomically(Path target, CanonicalPokemonState state) {
        byte[] encoded = encode(state);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(pokemonDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical Pokemon store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical Pokemon state", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup. The atomically replaced target is authoritative.
                }
            }
        }
    }

    private byte[] encode(CanonicalPokemonState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.pokemonId());
                output.writeUTF(state.ownerPlayerId());
                output.writeUTF(state.speciesId());
                output.writeInt(state.level());
                writeStringSet(output, state.capabilities());
                writeStatusState(output, state.statusState());
                writeNullableCombatStats(output, state.combatStats());
                writeNullableHealth(output, state.health());
                writeNullableMoveLoadout(output, state.moveLoadout());
                writeNullableBaseMovement(output, state.baseMovement());
                writeNullableBattleTraits(output, state.battleTraits());
                writeNullableAccuracyEvasion(output, state.accuracyEvasion());
                writeNullableInjuryState(output, state.injuryState());
                writeNullableString(output, state.heldItemInstanceId());
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical Pokemon state", error);
        }
    }

    private static void writeStatusState(DataOutputStream output, CanonicalStatusState state) throws IOException {
        List<CanonicalStatusEntry> entries = state == null ? List.of() : state.entries();
        output.writeInt(entries.size());
        for (CanonicalStatusEntry entry : entries) {
            output.writeUTF(entry.name());
            output.writeInt(entry.payload().size());
            for (Map.Entry<String, Object> payload : entry.payload().entrySet()) {
                output.writeUTF(payload.getKey());
                writeScalar(output, payload.getValue());
            }
        }
    }

    private static CanonicalStatusState readStatusState(DataInputStream input) throws IOException {
        int count = readCount(input, "status entries");
        ArrayList<CanonicalStatusEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = input.readUTF();
            int payloadCount = readCount(input, "status payload");
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            for (int p = 0; p < payloadCount; p++) payload.put(input.readUTF(), readScalar(input));
            entries.add(new CanonicalStatusEntry(name, payload));
        }
        return new CanonicalStatusState(entries);
    }

    private static void writeScalar(DataOutputStream output, Object value) throws IOException {
        if (value == null) { output.writeByte(0); return; }
        if (value instanceof String text) { output.writeByte(1); output.writeUTF(text); return; }
        if (value instanceof Integer number) { output.writeByte(2); output.writeInt(number); return; }
        if (value instanceof Long number) { output.writeByte(3); output.writeLong(number); return; }
        if (value instanceof Double number) { output.writeByte(4); output.writeDouble(number); return; }
        if (value instanceof Boolean flag) { output.writeByte(5); output.writeBoolean(flag); return; }
        throw new IllegalArgumentException("unsupported canonical status scalar: " + value.getClass().getName());
    }

    private static Object readScalar(DataInputStream input) throws IOException {
        return switch (input.readUnsignedByte()) {
            case 0 -> null;
            case 1 -> input.readUTF();
            case 2 -> input.readInt();
            case 3 -> input.readLong();
            case 4 -> input.readDouble();
            case 5 -> input.readBoolean();
            default -> throw new IllegalStateException("unsupported canonical status scalar tag");
        };
    }

    private static void writeNullableCombatStats(DataOutputStream output, CanonicalCombatStats value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeInt(value.atk()); output.writeInt(value.def()); output.writeInt(value.spatk());
            output.writeInt(value.spdef()); output.writeInt(value.spd());
        }
    }
    private static CanonicalCombatStats readNullableCombatStats(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalCombatStats(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt()) : null;
    }
    private static void writeNullableHealth(DataOutputStream output, CanonicalHealth value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) { output.writeInt(value.currentHp()); output.writeInt(value.maxHp()); }
    }
    private static CanonicalHealth readNullableHealth(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalHealth(input.readInt(), input.readInt()) : null;
    }
    private static void writeNullableMoveLoadout(DataOutputStream output, CanonicalMoveLoadout value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) writeStringList(output, value.moveIds());
    }
    private static CanonicalMoveLoadout readNullableMoveLoadout(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalMoveLoadout(readStringList(input)) : null;
    }
    private static void writeNullableBaseMovement(DataOutputStream output, CanonicalBaseMovement value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeInt(value.overland()); output.writeInt(value.swim()); output.writeInt(value.sky());
            output.writeInt(value.longJump()); output.writeInt(value.highJump());
        }
    }
    private static CanonicalBaseMovement readNullableBaseMovement(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalBaseMovement(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt()) : null;
    }
    private static void writeNullableBattleTraits(DataOutputStream output, CanonicalBattleTraits value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) { writeStringList(output, value.types()); writeStringList(output, value.abilities()); }
    }
    private static CanonicalBattleTraits readNullableBattleTraits(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalBattleTraits(readStringList(input), readStringList(input)) : null;
    }
    private static void writeNullableAccuracyEvasion(DataOutputStream output, CanonicalAccuracyEvasion value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeInt(value.accuracyStage()); output.writeInt(value.physicalEvasionBonus());
            output.writeInt(value.specialEvasionBonus()); output.writeInt(value.statusEvasionBonus());
        }
    }
    private static CanonicalAccuracyEvasion readNullableAccuracyEvasion(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalAccuracyEvasion(input.readInt(), input.readInt(), input.readInt(), input.readInt()) : null;
    }
    private static void writeNullableInjuryState(DataOutputStream output, CanonicalInjuryState value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) output.writeInt(value.injuries());
    }
    private static CanonicalInjuryState readNullableInjuryState(DataInputStream input) throws IOException {
        return input.readBoolean() ? new CanonicalInjuryState(input.readInt()) : null;
    }
    private static void writeNullableString(DataOutputStream output, String value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) output.writeUTF(value);
    }
    private static String readNullableString(DataInputStream input) throws IOException {
        return input.readBoolean() ? input.readUTF() : null;
    }
    private static void writeStringSet(DataOutputStream output, Set<String> values) throws IOException {
        output.writeInt(values.size());
        for (String value : values) output.writeUTF(value);
    }
    private static Set<String> readStringSet(DataInputStream input) throws IOException {
        int count = readCount(input, "string set");
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) values.add(input.readUTF());
        return Set.copyOf(values);
    }
    private static void writeStringList(DataOutputStream output, List<String> values) throws IOException {
        output.writeInt(values.size());
        for (String value : values) output.writeUTF(value);
    }
    private static List<String> readStringList(DataInputStream input) throws IOException {
        int count = readCount(input, "string list");
        ArrayList<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) values.add(input.readUTF());
        return List.copyOf(values);
    }
    private static int readCount(DataInputStream input, String field) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > 100_000) throw new IllegalStateException("invalid canonical " + field + " count: " + count);
        return count;
    }

    private <T> T withLock(String pokemonId, IoSupplier<T> operation) {
        Path lockPath = lockPath(pokemonId);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to lock canonical Pokemon state", error);
        } finally {
            processLock.unlock();
        }
    }

    private Path statePath(String pokemonId) { return pokemonDirectory.resolve(hash(pokemonId) + ".bin"); }
    private Path lockPath(String pokemonId) { return pokemonDirectory.resolve(hash(pokemonId) + ".lock"); }

    private static void requireStoredIdentity(String expectedId, CanonicalPokemonState state) {
        if (!state.pokemonId().equals(expectedId)) throw new IllegalStateException("canonical Pokemon file identity mismatch");
    }
    private static String requireId(String pokemonId) {
        if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId must not be blank");
        return pokemonId.strip();
    }
    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required", impossible);
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> { T get() throws IOException; }
}
