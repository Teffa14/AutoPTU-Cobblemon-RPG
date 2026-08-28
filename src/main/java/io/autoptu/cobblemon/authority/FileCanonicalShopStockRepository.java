package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** World-save-scoped stock persistence. Shop/offer IDs and stock limits remain server-authored. */
public final class FileCanonicalShopStockRepository implements CanonicalShopStockRepository {
    private static final int MAGIC = 0x41505353; // APSS
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private final Path directory;

    public FileCanonicalShopStockRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("shop-stock");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create shop stock store", error);
        }
    }

    @Override
    public StockState getOrCreate(String shopId, String offerId, int authoredStockLimit) {
        String shop = requireId(shopId, "shopId");
        String offer = requireId(offerId, "offerId");
        if (authoredStockLimit <= 0) throw new IllegalArgumentException("authoredStockLimit must be positive");
        Path path = statePath(shop, offer);
        return withLock(path, () -> {
            if (!Files.exists(path)) {
                StockState initial = new StockState(shop, offer, authoredStockLimit, 0);
                write(path, initial);
                return initial;
            }
            StockState current = read(path);
            requireIdentity(shop, offer, current);
            if (current.remainingStock() > authoredStockLimit) {
                throw new IllegalStateException("persisted shop stock exceeds authored stock limit");
            }
            return current;
        });
    }

    @Override
    public boolean replaceIfRevision(
            String shopId,
            String offerId,
            long expectedRevision,
            int remainingStock
    ) {
        String shop = requireId(shopId, "shopId");
        String offer = requireId(offerId, "offerId");
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow one revision advance");
        }
        if (remainingStock < 0) throw new IllegalArgumentException("remainingStock cannot be negative");
        Path path = statePath(shop, offer);
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            StockState current = read(path);
            requireIdentity(shop, offer, current);
            if (current.revision() != expectedRevision) return false;
            if (remainingStock > current.remainingStock()) {
                throw new IllegalArgumentException("stock mutation cannot replenish an authored offer");
            }
            write(path, new StockState(shop, offer, remainingStock, expectedRevision + 1));
            return true;
        });
    }

    private Path statePath(String shopId, String offerId) {
        return directory.resolve(safe(shopId) + "--" + safe(offerId) + ".bin");
    }

    private static String safe(String value) {
        return java.util.HexFormat.of().formatHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static StockState read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC) throw new IllegalStateException("invalid shop stock file magic");
            if (in.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported shop stock schema");
            String shopId = in.readUTF();
            String offerId = in.readUTF();
            int remainingStock = in.readInt();
            long revision = in.readLong();
            if (in.read() != -1) throw new IllegalStateException("unexpected trailing shop stock data");
            return new StockState(shopId, offerId, remainingStock, revision);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read shop stock", error);
        }
    }

    private static void write(Path path, StockState state) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
                out.writeInt(MAGIC);
                out.writeInt(SCHEMA_VERSION);
                out.writeUTF(state.shopId());
                out.writeUTF(state.offerId());
                out.writeInt(state.remainingStock());
                out.writeLong(state.revision());
                out.flush();
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist shop stock", error);
        } finally {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private static void requireIdentity(String shopId, String offerId, StockState state) {
        if (!state.shopId().equals(shopId) || !state.offerId().equals(offerId)) {
            throw new IllegalStateException("shop stock identity mismatch");
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
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
}
