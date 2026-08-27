package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalShopCatalogueTest {
    @Test
    void defaultCataloguePublishesServerOwnedStockAndPrices() {
        CanonicalShopCatalogue catalogue = new CanonicalShopCatalogue();

        assertEquals(3, catalogue.offers().size());
        CanonicalShopOffer potion = catalogue.findOffer("basic-potion").orElseThrow();
        assertEquals("autoptu:potion", potion.itemTemplateId());
        assertEquals("autoptu:credits", potion.currencyId());
        assertEquals(200, potion.unitPrice());
        assertEquals(20, potion.availableStock());
        assertTrue(catalogue.findOffer("client-forged-offer").isEmpty());
    }

    @Test
    void duplicateOfferIdsFailClosed() {
        CanonicalShopOffer offer = new CanonicalShopOffer(
                "potion", "Potion", "autoptu:potion", "autoptu:credits", 200, 5);

        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopCatalogue(List.of(offer, offer)));
    }

    @Test
    void invalidEconomicTruthFailsAtTheServerCatalogueBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopOffer("bad", "Bad", "autoptu:item", "autoptu:credits", -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopOffer("bad", "Bad", "autoptu:item", "autoptu:credits", 1, -1));
    }
}
