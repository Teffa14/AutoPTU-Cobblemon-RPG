package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable immutable-intent journal for bag <-> item-storage transfers. */
public final class FileCanonicalItemStorageTransferRepository {
    private static final int MAGIC = 0x41505453; // APTS
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final Path directory;

    public enum Direction { DEPOSIT, WITHDRAW }
    public enum Stage { CREATED, SOURCE_REMOVED, TARGET_ADDED, COMMITTED }

    public record TransferAttempt(
            String transferId,
            String playerId,
            Direction direction,
            String bagItemInstanceId,
            String templateId,
            int quantity,
            Stage stage
    ) {
        public TransferAttempt {
            if (transferId == null || transferId.isBlank()) throw new IllegalArgumentException("transferId is required");
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
            if (direction == null) throw new IllegalArgumentException("direction is required");
            if (bagItemInstanceId == null || bagItemInstanceId.isBlank()) throw new IllegalArgumentException("bagItemInstanceId is required");
            if (templateId == null || templateId.isBlank()) throw new IllegalArgumentException("templateId is required");
            if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
            if (stage == null) throw new IllegalArgumentException("stage is required");
        }
    }

    public FileCanonicalItemStorageTransferRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("item-storage-transfers");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new UncheckedIOException("failed to create item storage transfer journal", error); }
    }

    public Optional<TransferAttempt> find(String transferId) {
        Path path = path(requireId(transferId));
        return Files.exists(path) ? Optional.of(read(path)) : Optional.empty();
    }

    public TransferAttempt createIfAbsent(TransferAttempt attempt) {
        Path path = path(requireId(attempt.transferId()));
        return withLock(path, () -> {
            if (Files.exists(path)) return read(path);
            write(path, attempt);
            return attempt;
        });
    }

    public boolean advance(String transferId, Stage expected, Stage next) {
        Path path = path(requireId(transferId));
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            TransferAttempt current = read(path);
            if (current.stage() == next) return true;
            if (current.stage() != expected || next.ordinal() != expected.ordinal() + 1) return false;
            write(path, new TransferAttempt(current.transferId(), current.playerId(), current.direction(),
                    current.bagItemInstanceId(), current.templateId(), current.quantity(), next));
            return true;
        });
    }

    public List<TransferAttempt> findPending() {
        ArrayList<TransferAttempt> pending = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                TransferAttempt attempt = read(path);
                if (attempt.stage() != Stage.COMMITTED) pending.add(attempt);
            }
        } catch (IOException error) { throw new UncheckedIOException("failed to scan item storage transfers", error); }
        return List.copyOf(pending);
    }

    private TransferAttempt read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC || in.readInt() != SCHEMA_VERSION) throw new IllegalStateException("invalid item storage transfer journal");
            return new TransferAttempt(in.readUTF(), in.readUTF(), Direction.valueOf(in.readUTF()), in.readUTF(),
                    in.readUTF(), in.readInt(), Stage.valueOf(in.readUTF()));
        } catch (IOException error) { throw new UncheckedIOException("failed to read item storage transfer", error); }
    }

    private void write(Path path, TransferAttempt attempt) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
            out.writeInt(MAGIC); out.writeInt(SCHEMA_VERSION); out.writeUTF(attempt.transferId());
            out.writeUTF(attempt.playerId()); out.writeUTF(attempt.direction().name()); out.writeUTF(attempt.bagItemInstanceId());
            out.writeUTF(attempt.templateId()); out.writeInt(attempt.quantity()); out.writeUTF(attempt.stage().name());
        } catch (IOException error) { throw new UncheckedIOException("failed to write item storage transfer", error); }
        try {
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) { throw new UncheckedIOException("failed to replace item storage transfer", error); }
    }

    private Path path(String transferId) {
        return directory.resolve(Integer.toHexString(transferId.hashCode()) + "-" + transferId.replaceAll("[^A-Za-z0-9._-]", "_") + ".bin");
    }
    private static String requireId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("identity must not be blank");
        return value.strip();
    }
    private <T> T withLock(Path path, java.util.concurrent.Callable<T> action) {
        ReentrantLock lock = LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
        lock.lock();
        try { return action.call(); }
        catch (RuntimeException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException(error); }
        finally { lock.unlock(); }
    }
}
