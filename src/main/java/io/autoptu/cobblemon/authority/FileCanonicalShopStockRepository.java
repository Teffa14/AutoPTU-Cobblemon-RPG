package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** World-save-scoped stock persistence. Shop/offer IDs and stock limits remain server-authored. */
public final class FileCanonicalShopStockRepository implements CanonicalShopStockRepository {
    private static final int MAGIC = 0x41505353; // APSS
    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int SCHEMA_VERSION = 2;
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
                StockDocument initial = new StockDocument(new StockState(shop, offer, authoredStockLimit, 0), Map.of());
                write(path, initial);
                return initial.state();
            }
            StockDocument current = read(path);
            requireIdentity(shop, offer, current.state());
            if (current.state().remainingStock() > authoredStockLimit) {
                throw new IllegalStateException("persisted shop stock exceeds authored stock limit");
            }
            return current.state();
        });
    }

    @Override
    public boolean replaceIfRevision(String shopId, String offerId, long expectedRevision, int remainingStock) {
        String shop = requireId(shopId, "shopId");
        String offer = requireId(offerId, "offerId");
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow one revision advance");
        }
        if (remainingStock < 0) throw new IllegalArgumentException("remainingStock cannot be negative");
        Path path = statePath(shop, offer);
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            StockDocument document = read(path);
            StockState current = document.state();
            requireIdentity(shop, offer, current);
            if (current.revision() != expectedRevision) return false;
            if (remainingStock > current.remainingStock()) {
                throw new IllegalArgumentException("stock mutation cannot replenish an authored offer");
            }
            write(path, new StockDocument(
                    new StockState(shop, offer, remainingStock, expectedRevision + 1),
                    document.appliedDepletions()));
            return true;
        });
    }

    @Override
    public DepletionResult deplete(
            String transactionId,
            String shopId,
            String offerId,
            int quantity,
            int authoredStockLimit,
            long expectedRevision
    ) {
        String txId = requireId(transactionId, "transactionId");
        String shop = requireId(shopId, "shopId");
        String offer = requireId(offerId, "offerId");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (authoredStockLimit <= 0) throw new IllegalArgumentException("authoredStockLimit must be positive");
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow one revision advance");
        }
        Path path = statePath(shop, offer);
        return withLock(path, () -> {
            StockDocument document;
            if (!Files.exists(path)) {
                document = new StockDocument(new StockState(shop, offer, authoredStockLimit, 0), Map.of());
                write(path, document);
            } else {
                document = read(path);
            }
            StockState current = document.state();
            requireIdentity(shop, offer, current);
            if (current.remainingStock() > authoredStockLimit) {
                throw new IllegalStateException("persisted shop stock exceeds authored stock limit");
            }
            AppliedDepletion existing = document.appliedDepletions().get(txId);
            if (existing != null) {
                if (existing.quantity() != quantity) {
                    return new DepletionResult(DepletionStatus.TRANSACTION_CONFLICT, current, existing);
                }
                return new DepletionResult(DepletionStatus.ALREADY_APPLIED, current, existing);
            }
            if (current.revision() != expectedRevision) {
                return new DepletionResult(DepletionStatus.STALE_REVISION, current, null);
            }
            if (current.remainingStock() < quantity) {
                return new DepletionResult(DepletionStatus.OUT_OF_STOCK, current, null);
            }
            int nextStock = current.remainingStock() - quantity;
            long nextRevision = current.revision() + 1;
            StockState updated = new StockState(shop, offer, nextStock, nextRevision);
            AppliedDepletion receipt = new AppliedDepletion(
                    txId, quantity, current.remainingStock(), nextStock, nextRevision);
            LinkedHashMap<String, AppliedDepletion> receipts = new LinkedHashMap<>(document.appliedDepletions());
            receipts.put(txId, receipt);
            write(path, new StockDocument(updated, receipts));
            return new DepletionResult(DepletionStatus.APPLIED, updated, receipt);
        });
    }

    private Path statePath(String shopId, String offerId) {
        return directory.resolve(safe(shopId) + "--" + safe(offerId) + ".bin");
    }

    private static String safe(String value) {
        return java.util.HexFormat.of().formatHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static StockDocument read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC) throw new IllegalStateException("invalid shop stock file magic");
            int schema = in.readInt();
            if (schema != LEGACY_SCHEMA_VERSION && schema != SCHEMA_VERSION) {
                throw new IllegalStateException("unsupported shop stock schema");
            }
            StockState state = new StockState(in.readUTF(), in.readUTF(), in.readInt(), in.readLong());
            LinkedHashMap<String, AppliedDepletion> receipts = new LinkedHashMap<>();
            if (schema >= 2) {
                int count = in.readInt();
                if (count < 0) throw new IllegalStateException("invalid depletion receipt count");
                for (int i = 0; i < count; i++) {
                    AppliedDepletion receipt = new AppliedDepletion(
                            in.readUTF(), in.readInt(), in.readInt(), in.readInt(), in.readLong());
                    if (receipts.put(receipt.transactionId(), receipt) != null) {
                        throw new IllegalStateException("duplicate depletion transaction id");
                    }
                }
            }
            if (in.read() != -1) throw new IllegalStateException("unexpected trailing shop stock data");
            return new StockDocument(state, receipts);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read shop stock", error);
        }
    }

    private static void write(Path path, StockDocument document) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
                StockState state = document.state();
                out.writeInt(MAGIC);
                out.writeInt(SCHEMA_VERSION);
                out.writeUTF(state.shopId());
                out.writeUTF(state.offerId());
                out.writeInt(state.remainingStock());
                out.writeLong(state.revision());
                out.writeInt(document.appliedDepletions().size());
                for (AppliedDepletion receipt : document.appliedDepletions().values()) {
                    out.writeUTF(receipt.transactionId());
                    out.writeInt(receipt.quantity());
                    out.writeInt(receipt.stockBefore());
                    out.writeInt(receipt.stockAfter());
                    out.writeLong(receipt.resultingRevision());
                }
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

    private record StockDocument(StockState state, Map<String, AppliedDepletion> appliedDepletions) {
        private StockDocument {
            if (state == null) throw new IllegalArgumentException("state is required");
            if (appliedDepletions == null) throw new IllegalArgumentException("appliedDepletions is required");
            appliedDepletions = Map.copyOf(appliedDepletions);
        }
    }
}
