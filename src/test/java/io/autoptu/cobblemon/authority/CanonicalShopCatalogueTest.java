package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalShopCatalogueTest {
    @Test
    void defaultCatalogueIsServerAuthoredAndDeterministic() {
        List<CanonicalShopOffer> offers = CanonicalShopCatalogue.DEFAULT.offers("cedar-mart");

        assertEquals(3, offers.size());
        assertEquals("field-ration", offers.get(0).offerId());
        assertEquals("field_ration", offers.get(0).itemTemplateId());
        assertEquals("ouros_credit", offers.get(0).currencyId());
        assertEquals(80, offers.get(0).unitPrice());
        assertEquals(12, offers.get(0).stockLimit());
        assertTrue(CanonicalShopCatalogue.DEFAULT.offer("cedar-mart", "revive-kit").isPresent());
    }

    @Test
    void unknownShopAndOfferDoNotManufactureCatalogEntries() {
        assertTrue(CanonicalShopCatalogue.DEFAULT.offers("missing-shop").isEmpty());
        assertTrue(CanonicalShopCatalogue.DEFAULT.offer("cedar-mart", "missing-offer").isEmpty());
    }

    @Test
    void duplicateOfferIdsFailClosed() {
        CanonicalShopOffer offer = new CanonicalShopOffer("same", "field_ration", "ouros_credit", 10, 1);
        assertThrows(IllegalArgumentException.class, () -> new CanonicalShopCatalogue(
                Map.of("shop", List.of(offer, offer))));
    }

    @Test
    void invalidEconomicMetadataFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopOffer("offer", "item", "ouros_credit", 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopOffer("offer", "item", "ouros_credit", 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalShopOffer("offer", "item", " ", 1, 1));
    }
}
