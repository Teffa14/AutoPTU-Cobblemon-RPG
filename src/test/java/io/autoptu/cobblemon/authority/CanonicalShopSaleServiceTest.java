package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalShopSaleServiceTest {
    @TempDir Path temp;

    @Test
    void saleConsumesCanonicalItemAndCreditsWalletExactlyOnce() {
        Fixture fixture = new Fixture(temp);
        fixture.addItem("stack-1", "player-1", "field_ration", 3);

        var first = fixture.service.sell("sale-1", "player-1", "cedar-mart", "field_ration", 2);
        assertTrue(first.committed());
        assertEquals(80, first.walletBalance());
        assertEquals(1, fixture.items.findItem("stack-1").orElseThrow().quantity());

        var retry = fixture.service.sell("sale-1", "player-1", "cedar-mart", "field_ration", 2);
        assertTrue(retry.committed());
        assertEquals(80, retry.walletBalance());
        assertEquals(1, fixture.items.findItem("stack-1").orElseThrow().quantity());
    }

    @Test
    void restartRecoveryDoesNotDuplicateSaleMutation() {
        Fixture first = new Fixture(temp);
        first.addItem("stack-restart", "player-1", "basic_bandage", 2);
        assertTrue(first.service.sell("sale-restart", "player-1", "cedar-mart", "basic_bandage", 1).committed());

        Fixture reopened = new Fixture(temp);
        var recovered = reopened.service.recoverPending();
        assertTrue(recovered.isEmpty());
        assertEquals(60, reopened.wallets.findOrCreate("player-1").balance());
        assertEquals(1, reopened.items.findItem("stack-restart").orElseThrow().quantity());
    }

    @Test
    void unavailableItemFailsWithoutWalletCredit() {
        Fixture fixture = new Fixture(temp);
        var result = fixture.service.sell("sale-empty", "player-1", "cedar-mart", "revive_kit", 1);
        assertEquals(CanonicalShopSaleService.Status.ITEM_UNAVAILABLE, result.status());
        assertEquals(0, result.walletBalance());
    }

    @Test
    void pendingJournalResumesAcrossConsumedItemBoundary() {
        Fixture fixture = new Fixture(temp);
        fixture.addItem("stack-pending", "player-1", "field_ration", 2);
        var offer = CanonicalShopSellCatalogue.DEFAULT.offer("cedar-mart", "field_ration").orElseThrow();
        fixture.sales.createIfAbsent(new FileCanonicalShopSaleRepository.SaleAttempt(
                "sale-pending", "player-1", "cedar-mart", "stack-pending", "field_ration", offer.currencyId(),
                1, 0, offer.unitPrice(), offer.unitPrice(), FileCanonicalShopSaleRepository.Stage.CREATED));
        ItemReservation reservation = new ItemReservation(
                "sale-pending:item", "player-1", "stack-pending", "field_ration", 1, 0);
        assertTrue(fixture.items.tryReserveItem(reservation));
        assertTrue(fixture.items.consumeReservationRetainingLock("sale-pending:item", "player-1"));
        assertTrue(fixture.sales.advance("sale-pending", FileCanonicalShopSaleRepository.Stage.CREATED,
                FileCanonicalShopSaleRepository.Stage.ITEM_RESERVED));
        assertTrue(fixture.sales.advance("sale-pending", FileCanonicalShopSaleRepository.Stage.ITEM_RESERVED,
                FileCanonicalShopSaleRepository.Stage.ITEM_CONSUMED));

        var recovered = fixture.service.recoverPending();
        assertEquals(1, recovered.size());
        assertTrue(recovered.get(0).committed());
        assertEquals(40, recovered.get(0).walletBalance());
        assertEquals(1, fixture.items.findItem("stack-pending").orElseThrow().quantity());
        assertTrue(fixture.items.findReservation("sale-pending:item").isEmpty());
    }

    private static final class Fixture {
        final FileCanonicalPokemonRepository pokemon;
        final FileCanonicalItemReservationRepository items;
        final FileCanonicalWalletRepository wallets;
        final FileCanonicalShopSaleRepository sales;
        final CanonicalShopSaleService service;

        Fixture(Path root) {
            pokemon = new FileCanonicalPokemonRepository(root);
            items = new FileCanonicalItemReservationRepository(root, pokemon::findPokemon);
            wallets = new FileCanonicalWalletRepository(root);
            sales = new FileCanonicalShopSaleRepository(root);
            service = new CanonicalShopSaleService(CanonicalShopSellCatalogue.DEFAULT, wallets, items, sales);
        }

        void addItem(String id, String player, String template, int quantity) {
            assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(id, player, template, quantity, 0)));
        }
    }
}
