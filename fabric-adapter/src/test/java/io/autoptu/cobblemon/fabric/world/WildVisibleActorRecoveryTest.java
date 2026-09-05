package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WildVisibleActorRecoveryTest {
    @Test
    void canonicalHomeAnchorResolvesDifferentPopulationSitesWithoutRegionSpecificRuntime() {
        assertAnchor(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID);
        assertAnchor(CanonicalWildEncounterCatalogue.MAREA_MIRADOR_FLETCHLING_ID);
        assertAnchor(CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_FLETCHLING_ID);
    }

    @Test
    void canonicalHomeAnchorRejectsMissingEncounterInsteadOfInventingWorldState() {
        assertThrows(IllegalArgumentException.class, () -> WildVisibleActorRecovery.canonicalHomeAnchor(null));
    }

    private static void assertAnchor(String encounterId) {
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(encounterId).orElseThrow();
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId()).orElseThrow();

        var anchor = WildVisibleActorRecovery.canonicalHomeAnchor(encounter);

        assertEquals(site.x() + encounter.presentationOffsetX(), anchor.getX());
        assertEquals(site.y() + encounter.presentationOffsetY(), anchor.getY());
        assertEquals(site.z() + encounter.presentationOffsetZ(), anchor.getZ());
    }
}
