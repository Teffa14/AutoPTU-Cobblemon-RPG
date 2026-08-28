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

/** Durable server-owned journal for restart-safe shop sales. */
public final class FileCanonicalShopSaleRepository {
    private static final int MAGIC = 0x41505353; // APSS
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final Path directory;

    public FileCanonicalShopSaleRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("shop-sales");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new UncheckedIOException("failed to create shop sale store", error); }
    }

    public SaleAttempt createIfAbsent(SaleAttempt initial) {
        if (initial == null || initial.stage() != Stage.CREATED) throw new IllegalArgumentException("initial sale must be CREATED");
        Path path = statePath(initial.saleId());
        return withLock(path, () -> {
            if (Files.exists(path)) {
                SaleAttempt existing = read(path);
                if (!existing.sameIntent(initial)) throw new IllegalStateException("saleId belongs to a different immutable sale intent");
                return existing;
            }
            write(path, initial);
            return initial;
        });
    }

    public Optional<SaleAttempt> find(String saleId) {
        Path path = statePath(requireId(saleId, "saleId"));
        return Files.exists(path) ? Optional.of(withLock(path, () -> read(path))) : Optional.empty();
    }

    public boolean advance(String saleId, Stage expected, Stage next) {
        if (expected == null || next == null || !expected.canAdvanceTo(next)) throw new IllegalArgumentException("invalid sale stage transition");
        Path path = statePath(requireId(saleId, "saleId"));
        return withLock(path, () -> {
            if (!Files.exists(path)) return false;
            SaleAttempt current = read(path);
            if (current.stage() == next) return true;
            if (current.stage() != expected) return false;
            write(path, current.withStage(next));
            return true;
        });
    }

    public List<SaleAttempt> findPending() {
        List<SaleAttempt> pending = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                SaleAttempt attempt = withLock(path, () -> read(path));
                if (!attempt.stage().terminal()) pending.add(attempt);
            }
        } catch (IOException error) { throw new UncheckedIOException("failed to scan shop sale journal", error); }
        pending.sort(Comparator.comparing(SaleAttempt::saleId));
        return List.copyOf(pending);
    }

    private Path statePath(String saleId) { return directory.resolve(fileKey(saleId) + ".bin"); }

    private static SaleAttempt read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC || in.readInt() != SCHEMA_VERSION) throw new IllegalStateException("invalid shop sale journal");
            SaleAttempt attempt = new SaleAttempt(
                    in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(),
                    in.readInt(), in.readLong(), in.readLong(), in.readLong(), Stage.valueOf(in.readUTF()));
            if (in.read() != -1) throw new IllegalStateException("unexpected trailing shop sale data");
            return attempt;
        } catch (IOException error) { throw new UncheckedIOException("failed to read shop sale", error); }
    }

    private static void write(Path path, SaleAttempt attempt) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
                out.writeInt(MAGIC); out.writeInt(SCHEMA_VERSION);
                out.writeUTF(attempt.saleId()); out.writeUTF(attempt.playerId()); out.writeUTF(attempt.shopId());
                out.writeUTF(attempt.itemInstanceId()); out.writeUTF(attempt.itemTemplateId()); out.writeUTF(attempt.currencyId());
                out.writeInt(attempt.quantity()); out.writeLong(attempt.itemRevision()); out.writeLong(attempt.unitPrice());
                out.writeLong(attempt.totalPrice()); out.writeUTF(attempt.stage().name()); out.flush();
            }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { throw new IllegalStateException("shop sale journal requires atomic replacement", error); }
        } catch (IOException error) { throw new UncheckedIOException("failed to persist shop sale", error); }
        finally { try { Files.deleteIfExists(temp); } catch (IOException ignored) { } }
    }

    private static String fileKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 required", error); }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static <T> T withLock(Path path, java.util.concurrent.Callable<T> operation) {
        ReentrantLock lock = LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
        lock.lock();
        try { return operation.call(); }
        catch (RuntimeException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException(error); }
        finally { lock.unlock(); }
    }

    public enum Stage {
        CREATED, ITEM_RESERVED, ITEM_CONSUMED, WALLET_CREDITED, COMMITTED, FAILED_ITEM_UNAVAILABLE;
        public boolean terminal() { return this == COMMITTED || this == FAILED_ITEM_UNAVAILABLE; }
        boolean canAdvanceTo(Stage next) {
            return switch (this) {
                case CREATED -> next == ITEM_RESERVED || next == FAILED_ITEM_UNAVAILABLE;
                case ITEM_RESERVED -> next == ITEM_CONSUMED;
                case ITEM_CONSUMED -> next == WALLET_CREDITED;
                case WALLET_CREDITED -> next == COMMITTED;
                default -> false;
            };
        }
    }

    public record SaleAttempt(
            String saleId, String playerId, String shopId, String itemInstanceId, String itemTemplateId,
            String currencyId, int quantity, long itemRevision, long unitPrice, long totalPrice, Stage stage
    ) {
        public SaleAttempt {
            saleId = requireId(saleId, "saleId"); playerId = requireId(playerId, "playerId"); shopId = requireId(shopId, "shopId");
            itemInstanceId = requireId(itemInstanceId, "itemInstanceId"); itemTemplateId = requireId(itemTemplateId, "itemTemplateId");
            currencyId = requireId(currencyId, "currencyId");
            if (quantity <= 0 || itemRevision < 0 || unitPrice <= 0 || totalPrice <= 0 || stage == null) throw new IllegalArgumentException("invalid sale values");
            if (Math.multiplyExact(unitPrice, (long) quantity) != totalPrice) throw new IllegalArgumentException("totalPrice mismatch");
        }
        SaleAttempt withStage(Stage replacement) { return new SaleAttempt(saleId, playerId, shopId, itemInstanceId, itemTemplateId, currencyId, quantity, itemRevision, unitPrice, totalPrice, replacement); }
        boolean sameIntent(SaleAttempt other) {
            return saleId.equals(other.saleId) && playerId.equals(other.playerId) && shopId.equals(other.shopId)
                    && itemInstanceId.equals(other.itemInstanceId) && itemTemplateId.equals(other.itemTemplateId)
                    && currencyId.equals(other.currencyId) && quantity == other.quantity && itemRevision == other.itemRevision
                    && unitPrice == other.unitPrice && totalPrice == other.totalPrice;
        }
    }
}
