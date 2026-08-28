package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable journal for crash-recoverable party/box Pokemon transfers. */
public final class FileCanonicalPokemonTransferRepository {
    private static final int MAGIC = 0x41505452; // APTR
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private final Path directory;

    public FileCanonicalPokemonTransferRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("pokemon-transfers");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create Pokemon transfer journal", error);
        }
    }

    public TransferAttempt createIfAbsent(TransferAttempt initial) {
        if (initial == null) throw new IllegalArgumentException("initial is required");
        if (initial.stage() != Stage.CREATED) throw new IllegalArgumentException("initial transfer stage must be CREATED");
        Path path = statePath(initial.transferId());
        return withLock(path, () -> {
            if (Files.exists(path)) {
                TransferAttempt existing = read(path);
                if (!existing.sameIntent(initial)) {
                    throw new IllegalStateException("transferId already belongs to a different immutable transfer intent");
                }
                return existing;
            }
            write(path, initial);
            return initial;
        });
    }

    public Optional<TransferAttempt> find(String transferId) {
        String id = requireId(transferId, "transferId");
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        return Optional.of(withLock(path, () -> read(path)));
    }

    public boolean advance(String transferId, Stage expected, Stage next) {
        String id = requireId(transferId, "transferId");
        if (expected == null || next == null) throw new IllegalArgumentException("transfer stages are required");
        if (!expected.canAdvanceTo(next)) throw new IllegalArgumentException("invalid transfer stage transition");
        Path path = statePath(id);
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            TransferAttempt current = read(path);
            if (current.stage() == next) return true;
            if (current.stage() != expected) return false;
            write(path, current.withStage(next));
            return true;
        });
    }

    public List<TransferAttempt> findPending() {
        ArrayList<TransferAttempt> pending = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                TransferAttempt attempt = withLock(path, () -> read(path));
                if (!attempt.stage().terminal()) pending.add(attempt);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan Pokemon transfer journal", error);
        }
        pending.sort(Comparator.comparing(TransferAttempt::transferId));
        return List.copyOf(pending);
    }

    private Path statePath(String transferId) {
        return directory.resolve(fileKey(requireId(transferId, "transferId")) + ".bin");
    }

    private static TransferAttempt read(Path path) {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid Pokemon transfer journal magic");
            if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported Pokemon transfer journal schema");
            TransferAttempt attempt = new TransferAttempt(
                    input.readUTF(),
                    input.readUTF(),
                    Direction.valueOf(input.readUTF()),
                    input.readUTF(),
                    Stage.valueOf(input.readUTF())
            );
            if (input.read() != -1) throw new IllegalStateException("unexpected trailing Pokemon transfer journal data");
            return attempt;
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read Pokemon transfer journal", error);
        }
    }

    private static void write(Path path, TransferAttempt attempt) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(temporary))) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(attempt.transferId());
                output.writeUTF(attempt.playerId());
                output.writeUTF(attempt.direction().name());
                output.writeUTF(attempt.pokemonId());
                output.writeUTF(attempt.stage().name());
                output.flush();
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("Pokemon transfer journal requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist Pokemon transfer journal", error);
        } finally {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private static String fileKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", error);
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static <T> T withLock(Path path, java.util.concurrent.Callable<T> operation) {
        ReentrantLock lock = LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            return operation.call();
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException(error);
        } finally {
            lock.unlock();
        }
    }

    public enum Direction { DEPOSIT, WITHDRAW }

    public enum Stage {
        CREATED,
        SOURCE_REMOVED,
        TARGET_ADDED,
        COMMITTED;

        public boolean terminal() { return this == COMMITTED; }

        boolean canAdvanceTo(Stage next) {
            return switch (this) {
                case CREATED -> next == SOURCE_REMOVED;
                case SOURCE_REMOVED -> next == TARGET_ADDED;
                case TARGET_ADDED -> next == COMMITTED;
                case COMMITTED -> false;
            };
        }
    }

    public record TransferAttempt(
            String transferId,
            String playerId,
            Direction direction,
            String pokemonId,
            Stage stage
    ) {
        public TransferAttempt {
            transferId = requireId(transferId, "transferId");
            playerId = requireId(playerId, "playerId");
            pokemonId = requireId(pokemonId, "pokemonId");
            if (direction == null) throw new IllegalArgumentException("direction is required");
            if (stage == null) throw new IllegalArgumentException("stage is required");
        }

        TransferAttempt withStage(Stage next) {
            return new TransferAttempt(transferId, playerId, direction, pokemonId, next);
        }

        boolean sameIntent(TransferAttempt other) {
            return transferId.equals(other.transferId)
                    && playerId.equals(other.playerId)
                    && direction == other.direction
                    && pokemonId.equals(other.pokemonId);
        }
    }
}
