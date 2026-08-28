package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalShopPurchaseServiceTest {
    @TempDir Path temp;

    @Test
    void purchaseDebitsWalletDepletesStockAndGrantsCanonicalItemExactlyOnce() {
        Fixture fixture = new Fixture(temp);
        fixture.credit("player-1", 1000);

        var first = fixture.service.purchase("purchase-1", "player-1", "cedar-mart", "field-ration", 2);
        assertTrue(first.committed());
        assertEquals(840, first.walletBalance());
        assertEquals(10, first.remainingStock());

        CanonicalItemInstance item = fixture.items.findItem("shop-purchase:purchase-1").orElseThrow();
        assertEquals("player-1", item.ownerPlayerId());
        assertEquals("field_ration", item.templateId());
        assertEquals(2, item.quantity());
        assertEquals(0, item.revision());

        var repeated = fixture.service.purchase("purchase-1", "player-1", "cedar-mart", "field-ration", 2);
        assertTrue(repeated.committed());
        assertEquals(840, repeated.walletBalance());
        assertEquals(10, repeated.remainingStock());
        assertEquals(2, fixture.items.findItem("shop-purchase:purchase-1").orElseThrow().quantity());
    }

    @Test
    void committedPurchaseSurvivesRepositoryReopenWithoutDoubleMutation() {
        Fixture first = new Fixture(temp);
        first.credit("player-1", 1000);
        assertTrue(first.service.purchase("purchase-restart", "player-1", "cedar-mart", "basic-bandage", 3).committed());

        Fixture reopened = new Fixture(temp);
        var recovered = reopened.service.purchase("purchase-restart", "player-1", "cedar-mart", "basic-bandage", 3);
        assertTrue(recovered.committed());
        assertEquals(640, recovered.walletBalance());
        assertEquals(5, recovered.remainingStock());
        assertEquals(3, reopened.items.findItem("shop-purchase:purchase-restart").orElseThrow().quantity());
    }

    @Test
    void insufficientFundsLeavesStockAndInventoryUntouched() {
        Fixture fixture = new Fixture(temp);
        fixture.credit("player-1", 100);

        var result = fixture.service.purchase("purchase-poor", "player-1", "cedar-mart", "basic-bandage", 1);
        assertEquals(CanonicalShopPurchaseService.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(100, result.walletBalance());
        assertEquals(8, result.remainingStock());
        assertTrue(fixture.items.findItem("shop-purchase:purchase-poor").isEmpty());
    }

    @Test
    void outOfStockRefundsAnyCommittedDebitExactlyOnce() {
        Fixture fixture = new Fixture(temp);
        fixture.credit("player-1", 1000);
        CanonicalShopStockRepository.StockState stock = fixture.stock.getOrCreate("cedar-mart", "revive-kit", 4);
        var exhausted = fixture.stock.deplete(
                "admin-exhaust",
                "cedar-mart",
                "revive-kit",
                4,
                4,
                stock.revision());
        assertTrue(exhausted.committed());

        var first = fixture.service.purchase("purchase-empty", "player-1", "cedar-mart", "revive-kit", 1);
        assertEquals(CanonicalShopPurchaseService.Status.OUT_OF_STOCK, first.status());
        assertEquals(1000, first.walletBalance());
        assertEquals(0, first.remainingStock());
        assertTrue(fixture.items.findItem("shop-purchase:purchase-empty").isEmpty());

        var retry = fixture.service.purchase("purchase-empty", "player-1", "cedar-mart", "revive-kit", 1);
        assertEquals(CanonicalShopPurchaseService.Status.OUT_OF_STOCK, retry.status());
        assertEquals(1000, retry.walletBalance());
    }

    @Test
    void pendingJournalResumesAfterCrashBoundaries() {
        Fixture fixture = new Fixture(temp);
        fixture.credit("player-1", 1000);
        CanonicalShopOffer offer = CanonicalShopCatalogue.DEFAULT.offer("cedar-mart", "field-ration").orElseThrow();
        fixture.purchases.createIfAbsent(new FileCanonicalShopPurchaseRepository.PurchaseAttempt(
                "purchase-pending",
                "player-1",
                "cedar-mart",
                offer.offerId(),
                offer.itemTemplateId(),
                offer.currencyId(),
                1,
                offer.unitPrice(),
                offer.unitPrice(),
                offer.stockLimit(),
                FileCanonicalShopPurchaseRepository.Stage.CREATED));

        var recovered = fixture.service.recoverPending();
        assertEquals(1, recovered.size());
        assertTrue(recovered.get(0).committed());
        assertEquals(920, recovered.get(0).walletBalance());
        assertEquals(11, recovered.get(0).remainingStock());
        assertTrue(fixture.items.findItem("shop-purchase:purchase-pending").isPresent());
        assertTrue(fixture.purchases.findPending().isEmpty());
    }

    private static final class Fixture {
        final FileCanonicalPokemonRepository pokemon;
        final FileCanonicalItemReservationRepository items;
        final FileCanonicalWalletRepository wallets;
        final FileCanonicalShopStockRepository stock;
        final FileCanonicalShopPurchaseRepository purchases;
        final CanonicalShopPurchaseService service;

        Fixture(Path root) {
            pokemon = new FileCanonicalPokemonRepository(root);
            items = new FileCanonicalItemReservationRepository(root, pokemon::findPokemon);
            wallets = new FileCanonicalWalletRepository(root);
            stock = new FileCanonicalShopStockRepository(root);
            purchases = new FileCanonicalShopPurchaseRepository(root);
            service = new CanonicalShopPurchaseService(
                    CanonicalShopCatalogue.DEFAULT, wallets, stock, items, purchases);
        }

        void credit(String playerId, long amount) {
            var result = new CanonicalWalletTransactionService(wallets)
                    .credit("seed:" + playerId, playerId, amount, "test-seed");
            assertTrue(result.committed());
        }
    }
}
