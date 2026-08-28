package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/**
 * Server-authoritative shop purchase orchestration.
 *
 * <p>The client selects only shop/offer/quantity. Price, currency, stock, item template and all
 * mutation truth are resolved from server-owned authored/persistent state. Each cross-store step
 * has a deterministic idempotency identity and the durable journal can resume after restart.</p>
 */
public final class CanonicalShopPurchaseService {
    private static final int MAX_STALE_RETRIES = 16;

    private final CanonicalShopCatalogue catalogue;
    private final FileCanonicalWalletRepository wallets;
    private final CanonicalShopStockRepository stock;
    private final FileCanonicalItemReservationRepository items;
    private final FileCanonicalShopPurchaseRepository purchases;

    public CanonicalShopPurchaseService(
            CanonicalShopCatalogue catalogue,
            FileCanonicalWalletRepository wallets,
            CanonicalShopStockRepository stock,
            FileCanonicalItemReservationRepository items,
            FileCanonicalShopPurchaseRepository purchases
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.wallets = Objects.requireNonNull(wallets, "wallets");
        this.stock = Objects.requireNonNull(stock, "stock");
        this.items = Objects.requireNonNull(items, "items");
        this.purchases = Objects.requireNonNull(purchases, "purchases");
    }

    public PurchaseResult purchase(
            String purchaseId,
            String playerId,
            String shopId,
            String offerId,
            int quantity
    ) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        CanonicalShopOffer offer = catalogue.offer(shopId, offerId)
                .orElseThrow(() -> new IllegalArgumentException("unknown authored shop offer"));
        long total;
        try {
            total = Math.multiplyExact(offer.unitPrice(), (long) quantity);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("purchase price overflow", overflow);
        }
        FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt = purchases.createIfAbsent(
                new FileCanonicalShopPurchaseRepository.PurchaseAttempt(
                        purchaseId,
                        playerId,
                        shopId,
                        offer.offerId(),
                        offer.itemTemplateId(),
                        offer.currencyId(),
                        quantity,
                        offer.unitPrice(),
                        total,
                        offer.stockLimit(),
                        FileCanonicalShopPurchaseRepository.Stage.CREATED
                ));
        return resume(attempt);
    }

    public List<PurchaseResult> recoverPending() {
        return purchases.findPending().stream().map(this::resume).toList();
    }

    private PurchaseResult resume(FileCanonicalShopPurchaseRepository.PurchaseAttempt original) {
        FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt = purchases.find(original.purchaseId())
                .orElseThrow(() -> new IllegalStateException("purchase journal disappeared during recovery"));

        if (attempt.stage() == FileCanonicalShopPurchaseRepository.Stage.CREATED) {
            FileCanonicalWalletRepository.WalletState wallet = wallets.findOrCreate(attempt.playerId());
            if (!wallet.currencyId().equals(attempt.currencyId())) {
                throw new IllegalStateException("wallet currency does not match frozen purchase currency");
            }
            CanonicalWalletTransactionService.TransactionResult debit =
                    new CanonicalWalletTransactionService(wallets).debit(
                            walletTransactionId(attempt),
                            attempt.playerId(),
                            attempt.totalPrice(),
                            walletSourceId(attempt));
            switch (debit.status()) {
                case APPLIED, ALREADY_APPLIED -> advance(attempt, FileCanonicalShopPurchaseRepository.Stage.DEBITED);
                case INSUFFICIENT_FUNDS -> {
                    advance(attempt, FileCanonicalShopPurchaseRepository.Stage.FAILED_INSUFFICIENT_FUNDS);
                    return project(attempt.purchaseId(), Status.INSUFFICIENT_FUNDS);
                }
                case TRANSACTION_CONFLICT -> throw new IllegalStateException("wallet purchase transaction identity conflict");
                case RETRY_EXHAUSTED -> throw new IllegalStateException("wallet purchase debit retry exhausted");
            }
            attempt = requireAttempt(attempt.purchaseId());
        }

        if (attempt.stage() == FileCanonicalShopPurchaseRepository.Stage.DEBITED) {
            CanonicalShopStockRepository.DepletionResult depletion = depleteStock(attempt);
            if (depletion.status() == CanonicalShopStockRepository.DepletionStatus.OUT_OF_STOCK) {
                refund(attempt);
                advance(attempt, FileCanonicalShopPurchaseRepository.Stage.FAILED_OUT_OF_STOCK);
                return project(attempt.purchaseId(), Status.OUT_OF_STOCK);
            }
            if (depletion.status() == CanonicalShopStockRepository.DepletionStatus.TRANSACTION_CONFLICT) {
                throw new IllegalStateException("shop stock purchase transaction identity conflict");
            }
            if (!depletion.committed()) {
                throw new IllegalStateException("shop stock purchase depletion did not reach a committed state");
            }
            advance(attempt, FileCanonicalShopPurchaseRepository.Stage.STOCK_DEPLETED);
            attempt = requireAttempt(attempt.purchaseId());
        }

        if (attempt.stage() == FileCanonicalShopPurchaseRepository.Stage.STOCK_DEPLETED) {
            grantItemExactlyOnce(attempt);
            advance(attempt, FileCanonicalShopPurchaseRepository.Stage.ITEM_GRANTED);
            attempt = requireAttempt(attempt.purchaseId());
        }

        if (attempt.stage() == FileCanonicalShopPurchaseRepository.Stage.ITEM_GRANTED) {
            advance(attempt, FileCanonicalShopPurchaseRepository.Stage.COMMITTED);
            attempt = requireAttempt(attempt.purchaseId());
        }

        return switch (attempt.stage()) {
            case COMMITTED -> project(attempt.purchaseId(), Status.COMMITTED);
            case FAILED_INSUFFICIENT_FUNDS -> project(attempt.purchaseId(), Status.INSUFFICIENT_FUNDS);
            case FAILED_OUT_OF_STOCK -> project(attempt.purchaseId(), Status.OUT_OF_STOCK);
            default -> throw new IllegalStateException("purchase recovery stopped at non-terminal stage " + attempt.stage());
        };
    }

    private CanonicalShopStockRepository.DepletionResult depleteStock(
            FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt
    ) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalShopStockRepository.StockState current = stock.getOrCreate(
                    attempt.shopId(), attempt.offerId(), attempt.authoredStockLimit());
            CanonicalShopStockRepository.DepletionResult result = stock.deplete(
                    stockTransactionId(attempt),
                    attempt.shopId(),
                    attempt.offerId(),
                    attempt.quantity(),
                    attempt.authoredStockLimit(),
                    current.revision());
            if (result.status() != CanonicalShopStockRepository.DepletionStatus.STALE_REVISION) return result;
        }
        throw new IllegalStateException("shop stock purchase depletion retry exhausted");
    }

    private void refund(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        CanonicalWalletTransactionService.TransactionResult refund =
                new CanonicalWalletTransactionService(wallets).credit(
                        refundTransactionId(attempt),
                        attempt.playerId(),
                        attempt.totalPrice(),
                        refundSourceId(attempt));
        if (refund.status() != CanonicalWalletTransactionService.Status.APPLIED
                && refund.status() != CanonicalWalletTransactionService.Status.ALREADY_APPLIED) {
            throw new IllegalStateException("purchase refund failed: " + refund.status());
        }
    }

    private void grantItemExactlyOnce(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        String itemId = itemInstanceId(attempt);
        CanonicalItemInstance expected = new CanonicalItemInstance(
                itemId, attempt.playerId(), attempt.itemTemplateId(), attempt.quantity(), 0L);
        if (items.createItemIfAbsent(expected)) return;
        CanonicalItemInstance existing = items.findItem(itemId)
                .orElseThrow(() -> new IllegalStateException("deterministic purchase item disappeared"));
        if (!existing.ownerPlayerId().equals(expected.ownerPlayerId())
                || !existing.templateId().equals(expected.templateId())
                || existing.quantity() != expected.quantity()
                || existing.revision() != expected.revision()) {
            throw new IllegalStateException("deterministic purchase item identity conflict");
        }
    }

    private PurchaseResult project(String purchaseId, Status status) {
        FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt = requireAttempt(purchaseId);
        FileCanonicalWalletRepository.WalletState wallet = wallets.findOrCreate(attempt.playerId());
        CanonicalShopStockRepository.StockState remaining = stock.getOrCreate(
                attempt.shopId(), attempt.offerId(), attempt.authoredStockLimit());
        return new PurchaseResult(status, attempt, wallet.balance(), remaining.remainingStock());
    }

    private void advance(
            FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt,
            FileCanonicalShopPurchaseRepository.Stage next
    ) {
        if (!purchases.advance(attempt.purchaseId(), attempt.stage(), next)) {
            FileCanonicalShopPurchaseRepository.PurchaseAttempt current = requireAttempt(attempt.purchaseId());
            if (current.stage() != next) {
                throw new IllegalStateException("purchase journal stage changed unexpectedly");
            }
        }
    }

    private FileCanonicalShopPurchaseRepository.PurchaseAttempt requireAttempt(String purchaseId) {
        return purchases.find(purchaseId)
                .orElseThrow(() -> new IllegalStateException("purchase journal is missing"));
    }

    private static String walletTransactionId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return attempt.purchaseId() + ":wallet-debit";
    }

    private static String stockTransactionId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return attempt.purchaseId() + ":stock-deplete";
    }

    private static String refundTransactionId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return attempt.purchaseId() + ":wallet-refund";
    }

    private static String walletSourceId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return "shop:" + attempt.shopId() + ":" + attempt.offerId();
    }

    private static String refundSourceId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return "shop-refund:" + attempt.shopId() + ":" + attempt.offerId();
    }

    public static String itemInstanceId(FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt) {
        return "shop-purchase:" + attempt.purchaseId();
    }

    public enum Status {
        COMMITTED,
        INSUFFICIENT_FUNDS,
        OUT_OF_STOCK
    }

    public record PurchaseResult(
            Status status,
            FileCanonicalShopPurchaseRepository.PurchaseAttempt attempt,
            long walletBalance,
            int remainingStock
    ) {
        public PurchaseResult {
            status = Objects.requireNonNull(status, "status");
            attempt = Objects.requireNonNull(attempt, "attempt");
            if (walletBalance < 0 || remainingStock < 0) throw new IllegalArgumentException("projected state must not be negative");
        }

        public boolean committed() {
            return status == Status.COMMITTED;
        }
    }
}
