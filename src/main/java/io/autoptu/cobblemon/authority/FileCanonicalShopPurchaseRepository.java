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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable server-owned purchase journal used to resume cross-store shop commits after restart. */
public final class FileCanonicalShopPurchaseRepository {
    private static final int MAGIC = 0x41505350; // APSP
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private final Path directory;

    public FileCanonicalShopPurchaseRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("shop-purchases");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create shop purchase store", error);
        }
    }

    public PurchaseAttempt createIfAbsent(PurchaseAttempt initial) {
        if (initial == null) throw new IllegalArgumentException("initial is required");
        if (initial.stage() != Stage.CREATED) throw new IllegalArgumentException("initial purchase stage must be CREATED");
        Path path = statePath(initial.purchaseId());
        return withLock(path, () -> {
            if (Files.exists(path)) {
                PurchaseAttempt existing = read(path);
                requireSameIntent(existing, initial);
                return existing;
            }
            write(path, initial);
            return initial;
        });
    }

    public Optional<PurchaseAttempt> find(String purchaseId) {
        String id = requireId(purchaseId, "purchaseId");
        Path path = statePath(id);
        if (!Files.exists(path)) return Optional.empty();
        return Optional.of(withLock(path, () -> read(path)));
    }

    public boolean advance(String purchaseId, Stage expected, Stage next) {
        String id = requireId(purchaseId, "purchaseId");
        if (expected == null || next == null) throw new IllegalArgumentException("purchase stages are required");
        if (!expected.canAdvanceTo(next)) throw new IllegalArgumentException("invalid purchase stage transition");
        Path path = statePath(id);
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            PurchaseAttempt current = read(path);
            if (current.stage() == next) return true;
            if (current.stage() != expected) return false;
            write(path, current.withStage(next));
            return true;
        });
    }

    public List<PurchaseAttempt> findPending() {
        List<PurchaseAttempt> pending = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                PurchaseAttempt attempt = withLock(path, () -> read(path));
                if (!attempt.stage().terminal()) pending.add(attempt);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan shop purchase journal", error);
        }
        pending.sort(Comparator.comparing(PurchaseAttempt::purchaseId));
        return List.copyOf(pending);
    }

    private static void requireSameIntent(PurchaseAttempt existing, PurchaseAttempt requested) {
        if (!existing.sameIntent(requested)) {
            throw new IllegalStateException("purchaseId already belongs to a different immutable purchase intent");
        }
    }

    private Path statePath(String purchaseId) {
        return directory.resolve(fileKey(requireId(purchaseId, "purchaseId")) + ".bin");
    }

    private static PurchaseAttempt read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC) throw new IllegalStateException("invalid shop purchase file magic");
            if (in.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported shop purchase schema");
            PurchaseAttempt attempt = new PurchaseAttempt(
                    in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(),
                    in.readInt(), in.readLong(), in.readLong(), in.readInt(), Stage.valueOf(in.readUTF()));
            if (in.read() != -1) throw new IllegalStateException("unexpected trailing shop purchase data");
            return attempt;
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read shop purchase", error);
        }
    }

    private static void write(Path path, PurchaseAttempt attempt) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
                out.writeInt(MAGIC);
                out.writeInt(SCHEMA_VERSION);
                out.writeUTF(attempt.purchaseId());
                out.writeUTF(attempt.playerId());
                out.writeUTF(attempt.shopId());
                out.writeUTF(attempt.offerId());
                out.writeUTF(attempt.itemTemplateId());
                out.writeUTF(attempt.currencyId());
                out.writeInt(attempt.quantity());
                out.writeLong(attempt.unitPrice());
                out.writeLong(attempt.totalPrice());
                out.writeInt(attempt.authoredStockLimit());
                out.writeUTF(attempt.stage().name());
                out.flush();
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("shop purchase journal requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist shop purchase", error);
        } finally {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
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

    public enum Stage {
        CREATED,
        DEBITED,
        STOCK_DEPLETED,
        ITEM_GRANTED,
        COMMITTED,
        FAILED_INSUFFICIENT_FUNDS,
        FAILED_OUT_OF_STOCK;

        public boolean terminal() {
            return this == COMMITTED || this == FAILED_INSUFFICIENT_FUNDS || this == FAILED_OUT_OF_STOCK;
        }

        boolean canAdvanceTo(Stage next) {
            return switch (this) {
                case CREATED -> next == DEBITED || next == FAILED_INSUFFICIENT_FUNDS;
                case DEBITED -> next == STOCK_DEPLETED || next == FAILED_OUT_OF_STOCK;
                case STOCK_DEPLETED -> next == ITEM_GRANTED;
                case ITEM_GRANTED -> next == COMMITTED;
                default -> false;
            };
        }
    }

    public record PurchaseAttempt(
            String purchaseId,
            String playerId,
            String shopId,
            String offerId,
            String itemTemplateId,
            String currencyId,
            int quantity,
            long unitPrice,
            long totalPrice,
            int authoredStockLimit,
            Stage stage
    ) {
        public PurchaseAttempt {
            purchaseId = requireId(purchaseId, "purchaseId");
            playerId = requireId(playerId, "playerId");
            shopId = requireId(shopId, "shopId");
            offerId = requireId(offerId, "offerId");
            itemTemplateId = requireId(itemTemplateId, "itemTemplateId");
            currencyId = requireId(currencyId, "currencyId");
            if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
            if (unitPrice <= 0 || totalPrice <= 0) throw new IllegalArgumentException("purchase prices must be positive");
            if (authoredStockLimit <= 0) throw new IllegalArgumentException("authoredStockLimit must be positive");
            if (stage == null) throw new IllegalArgumentException("stage is required");
            try {
                if (Math.multiplyExact(unitPrice, (long) quantity) != totalPrice) {
                    throw new IllegalArgumentException("totalPrice must equal unitPrice times quantity");
                }
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("purchase price overflow", overflow);
            }
        }

        PurchaseAttempt withStage(Stage replacement) {
            return new PurchaseAttempt(
                    purchaseId, playerId, shopId, offerId, itemTemplateId, currencyId,
                    quantity, unitPrice, totalPrice, authoredStockLimit, replacement);
        }

        boolean sameIntent(PurchaseAttempt other) {
            return purchaseId.equals(other.purchaseId)
                    && playerId.equals(other.playerId)
                    && shopId.equals(other.shopId)
                    && offerId.equals(other.offerId)
                    && itemTemplateId.equals(other.itemTemplateId)
                    && currencyId.equals(other.currencyId)
                    && quantity == other.quantity
                    && unitPrice == other.unitPrice
                    && totalPrice == other.totalPrice
                    && authoredStockLimit == other.authoredStockLimit;
        }
    }
}
