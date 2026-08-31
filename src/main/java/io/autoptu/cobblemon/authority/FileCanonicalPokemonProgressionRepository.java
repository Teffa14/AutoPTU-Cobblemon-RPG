package io.autoptu.cobblemon.authority;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Durable server-owned persistence for explicit Pokemon progression facts.
 *
 * <p>This store never calculates XP thresholds, changes canonical Pokemon level,
 * chooses an evolution, learns a move, or derives progression from Cobblemon.
 * Mutation callers must supply already-authorized progression facts.</p>
 */
public final class FileCanonicalPokemonProgressionRepository {
    private static final int MAGIC = 0x41545050; // ATPP
    private static final int SCHEMA = 1;

    private final Path root;

    public FileCanonicalPokemonProgressionRepository(Path canonicalStateRoot) {
        if (canonicalStateRoot == null) throw new IllegalArgumentException("canonicalStateRoot must not be null");
        this.root = canonicalStateRoot.resolve("pokemon-progression").normalize();
    }

    public synchronized ProgressionState findOrCreate(String ownerPlayerId, String pokemonId) {
        String owner = requireId(ownerPlayerId, "ownerPlayerId");
        String id = requireId(pokemonId, "pokemonId");
        Optional<ProgressionState> existing = find(id);
        if (existing.isPresent()) {
            ProgressionState state = existing.get();
            if (!state.ownerPlayerId().equals(owner)) {
                throw new IllegalStateException("Pokemon progression ownership mismatch: " + id);
            }
            return state;
        }
        ProgressionState created = new ProgressionState(owner, id, 0L, null, 0L);
        write(created);
        return created;
    }

    public synchronized Optional<ProgressionState> find(String pokemonId) {
        String id = requireId(pokemonId, "pokemonId");
        Path file = fileFor(id);
        if (!Files.exists(file)) return Optional.empty();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("Invalid Pokemon progression file magic");
            int schema = input.readInt();
            if (schema != SCHEMA) throw new IllegalStateException("Unsupported Pokemon progression schema: " + schema);
            String owner = input.readUTF();
            String storedId = input.readUTF();
            long xp = input.readLong();
            String pendingEvolution = input.readBoolean() ? input.readUTF() : null;
            long revision = input.readLong();
            ProgressionState state = new ProgressionState(owner, storedId, xp, pendingEvolution, revision);
            if (!state.pokemonId().equals(id)) {
                throw new IllegalStateException("Pokemon progression file identity mismatch");
            }
            return Optional.of(state);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Pokemon progression for " + id, exception);
        }
    }

    public synchronized boolean replaceIfRevision(ProgressionState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement must not be null");
        Optional<ProgressionState> currentResult = find(replacement.pokemonId());
        if (currentResult.isEmpty()) return false;
        ProgressionState current = currentResult.get();
        if (current.revision() != expectedRevision) return false;
        if (!current.ownerPlayerId().equals(replacement.ownerPlayerId())) return false;
        if (replacement.revision() != expectedRevision + 1L) {
            throw new IllegalArgumentException("replacement revision must advance by exactly one");
        }
        write(replacement);
        return true;
    }

    private void write(ProgressionState state) {
        try {
            Files.createDirectories(root);
            Path target = fileFor(state.pokemonId());
            Path temporary = Files.createTempFile(root, target.getFileName().toString(), ".tmp");
            try {
                try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(
                        temporary, StandardOpenOption.TRUNCATE_EXISTING)))) {
                    output.writeInt(MAGIC);
                    output.writeInt(SCHEMA);
                    output.writeUTF(state.ownerPlayerId());
                    output.writeUTF(state.pokemonId());
                    output.writeLong(state.pokemonXp());
                    output.writeBoolean(state.pendingEvolutionChoiceId() != null);
                    if (state.pendingEvolutionChoiceId() != null) output.writeUTF(state.pendingEvolutionChoiceId());
                    output.writeLong(state.revision());
                }
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    channel.force(true);
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist Pokemon progression for " + state.pokemonId(), exception);
        }
    }

    private Path fileFor(String pokemonId) {
        return root.resolve(sha256(pokemonId) + ".bin");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String requireId(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }

    public record ProgressionState(
            String ownerPlayerId,
            String pokemonId,
            long pokemonXp,
            String pendingEvolutionChoiceId,
            long revision
    ) {
        public ProgressionState {
            ownerPlayerId = requireId(ownerPlayerId, "ownerPlayerId");
            pokemonId = requireId(pokemonId, "pokemonId");
            if (pokemonXp < 0L) throw new IllegalArgumentException("pokemonXp must be >= 0");
            pendingEvolutionChoiceId = pendingEvolutionChoiceId == null || pendingEvolutionChoiceId.isBlank()
                    ? null : pendingEvolutionChoiceId.strip();
            if (revision < 0L) throw new IllegalArgumentException("revision must be >= 0");
        }
    }
}
