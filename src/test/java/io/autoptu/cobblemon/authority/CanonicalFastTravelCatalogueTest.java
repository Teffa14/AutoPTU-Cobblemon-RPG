package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalFastTravelCatalogueTest {
    @Test
    void exposesOnlyServerAuthoredDestinationsInStableOrder() {
        var destinations = CanonicalFastTravelCatalogue.destinations();

        assertEquals(1, destinations.size());
        assertEquals(CanonicalFastTravelCatalogue.OVERWORLD_SPAWN_ID, destinations.getFirst().id());
        assertEquals("Overworld Spawn", destinations.getFirst().displayName());
    }

    @Test
    void refusesUnknownOrMissingDestinationIds() {
        assertTrue(CanonicalFastTravelCatalogue.find("overworld_spawn").isPresent());
        assertFalse(CanonicalFastTravelCatalogue.find("client_coords").isPresent());
        assertFalse(CanonicalFastTravelCatalogue.find("").isPresent());
        assertFalse(CanonicalFastTravelCatalogue.find(null).isPresent());
    }
}
