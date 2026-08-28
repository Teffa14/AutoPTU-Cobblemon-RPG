package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/** Restart-safe server-authoritative sale of canonical bag stacks into an authored shop sink. */
public final class CanonicalShopSaleService {
    private final CanonicalShopSellCatalogue catalogue;
    private final FileCanonicalWalletRepository wallets;
    private final FileCanonicalItemReservationRepository items;
    private final FileCanonicalShopSaleRepository sales;

    public CanonicalShopSaleService(
            CanonicalShopSellCatalogue catalogue,
            FileCanonicalWalletRepository wallets,
            FileCanonicalItemReservationRepository items,
            FileCanonicalShopSaleRepository sales
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.wallets = Objects.requireNonNull(wallets, "wallets");
        this.items = Objects.requireNonNull(items, "items");
        this.sales = Objects.requireNonNull(sales, "sales");
    }

    public SaleResult sell(String saleId, String playerId, String shopId, String itemTemplateId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        FileCanonicalShopSaleRepository.SaleAttempt existing = sales.find(saleId).orElse(null);
        if (existing != null) {
            if (!existing.playerId().equals(playerId)
                    || !existing.shopId().equals(shopId)
                    || !existing.itemTemplateId().equals(itemTemplateId)
                    || existing.quantity() != quantity) {
                throw new IllegalStateException("saleId already belongs to a different immutable sale request");
            }
            return resume(existing);
        }

        CanonicalShopSellCatalogue.SellOffer offer = catalogue.offer(shopId, itemTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("shop does not author a sell price for that item"));
        CanonicalItemInstance selected = items.findReservableItems(playerId, offer.itemTemplateId()).stream()
                .filter(item -> item.quantity() >= quantity)
                .findFirst()
                .orElse(null);
        if (selected == null) return new SaleResult(Status.ITEM_UNAVAILABLE, null, wallets.findOrCreate(playerId).balance());
        long total;
        try { total = Math.multiplyExact(offer.unitPrice(), (long) quantity); }
        catch (ArithmeticException overflow) { throw new IllegalArgumentException("sale price overflow", overflow); }
        FileCanonicalShopSaleRepository.SaleAttempt attempt = sales.createIfAbsent(
                new FileCanonicalShopSaleRepository.SaleAttempt(
                        saleId, playerId, shopId, selected.itemInstanceId(), offer.itemTemplateId(), offer.currencyId(),
                        quantity, selected.revision(), offer.unitPrice(), total, FileCanonicalShopSaleRepository.Stage.CREATED));
        return resume(attempt);
    }

    public List<SaleResult> recoverPending() {
        return sales.findPending().stream().map(this::resume).toList();
    }

    private SaleResult resume(FileCanonicalShopSaleRepository.SaleAttempt original) {
        FileCanonicalShopSaleRepository.SaleAttempt attempt = requireAttempt(original.saleId());
        String reservationId = reservationId(attempt);

        if (attempt.stage() == FileCanonicalShopSaleRepository.Stage.CREATED) {
            ItemReservation reservation = new ItemReservation(
                    reservationId, attempt.playerId(), attempt.itemInstanceId(), attempt.itemTemplateId(),
                    attempt.quantity(), attempt.itemRevision());
            boolean reserved = items.tryReserveItem(reservation);
            if (!reserved) {
                ItemReservation active = items.findReservation(reservationId).orElse(null);
                if (active == null || !active.equals(reservation)) {
                    advance(attempt, FileCanonicalShopSaleRepository.Stage.FAILED_ITEM_UNAVAILABLE);
                    return project(attempt.saleId(), Status.ITEM_UNAVAILABLE);
                }
            }
            advance(attempt, FileCanonicalShopSaleRepository.Stage.ITEM_RESERVED);
            attempt = requireAttempt(attempt.saleId());
        }

        if (attempt.stage() == FileCanonicalShopSaleRepository.Stage.ITEM_RESERVED) {
            if (!items.consumeReservationRetainingLock(reservationId, attempt.playerId())) {
                throw new IllegalStateException("sale item reservation could not be consumed safely");
            }
            advance(attempt, FileCanonicalShopSaleRepository.Stage.ITEM_CONSUMED);
            attempt = requireAttempt(attempt.saleId());
        }

        if (attempt.stage() == FileCanonicalShopSaleRepository.Stage.ITEM_CONSUMED) {
            FileCanonicalWalletRepository.WalletState wallet = wallets.findOrCreate(attempt.playerId());
            if (!wallet.currencyId().equals(attempt.currencyId())) throw new IllegalStateException("wallet currency mismatch");
            CanonicalWalletTransactionService.TransactionResult credit = new CanonicalWalletTransactionService(wallets).credit(
                    walletTransactionId(attempt), attempt.playerId(), attempt.totalPrice(), walletSourceId(attempt));
            if (!credit.committed()) throw new IllegalStateException("shop sale wallet credit failed: " + credit.status());
            advance(attempt, FileCanonicalShopSaleRepository.Stage.WALLET_CREDITED);
            attempt = requireAttempt(attempt.saleId());
        }

        if (attempt.stage() == FileCanonicalShopSaleRepository.Stage.WALLET_CREDITED) {
            if (!items.releaseConsumedReservationLock(reservationId, attempt.playerId())) {
                throw new IllegalStateException("sale item lock could not be released safely");
            }
            advance(attempt, FileCanonicalShopSaleRepository.Stage.COMMITTED);
            attempt = requireAttempt(attempt.saleId());
        }

        return switch (attempt.stage()) {
            case COMMITTED -> project(attempt.saleId(), Status.COMMITTED);
            case FAILED_ITEM_UNAVAILABLE -> project(attempt.saleId(), Status.ITEM_UNAVAILABLE);
            default -> throw new IllegalStateException("sale recovery stopped at non-terminal stage " + attempt.stage());
        };
    }

    private SaleResult project(String saleId, Status status) {
        FileCanonicalShopSaleRepository.SaleAttempt attempt = requireAttempt(saleId);
        return new SaleResult(status, attempt, wallets.findOrCreate(attempt.playerId()).balance());
    }

    private void advance(FileCanonicalShopSaleRepository.SaleAttempt attempt, FileCanonicalShopSaleRepository.Stage next) {
        if (!sales.advance(attempt.saleId(), attempt.stage(), next)) {
            FileCanonicalShopSaleRepository.SaleAttempt current = requireAttempt(attempt.saleId());
            if (current.stage() != next) throw new IllegalStateException("sale journal stage changed unexpectedly");
        }
    }

    private FileCanonicalShopSaleRepository.SaleAttempt requireAttempt(String saleId) {
        return sales.find(saleId).orElseThrow(() -> new IllegalStateException("sale journal is missing"));
    }

    private static String reservationId(FileCanonicalShopSaleRepository.SaleAttempt attempt) { return attempt.saleId() + ":item"; }
    private static String walletTransactionId(FileCanonicalShopSaleRepository.SaleAttempt attempt) { return attempt.saleId() + ":wallet-credit"; }
    private static String walletSourceId(FileCanonicalShopSaleRepository.SaleAttempt attempt) { return "shop-sale:" + attempt.shopId() + ":" + attempt.itemTemplateId(); }

    public enum Status { COMMITTED, ITEM_UNAVAILABLE }

    public record SaleResult(Status status, FileCanonicalShopSaleRepository.SaleAttempt attempt, long walletBalance) {
        public SaleResult {
            status = Objects.requireNonNull(status, "status");
            if (walletBalance < 0) throw new IllegalArgumentException("walletBalance must not be negative");
        }
        public boolean committed() { return status == Status.COMMITTED; }
    }
}
