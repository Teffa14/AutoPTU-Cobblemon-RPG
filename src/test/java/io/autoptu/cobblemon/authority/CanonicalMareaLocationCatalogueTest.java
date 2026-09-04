package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalMareaLocationCatalogueTest {
    @Test
    void everyAuthoredMareaWildPopulationSiteCanPersistLocationDiscovery() {
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var worldSite = CanonicalWorldMapCatalogue.DEFAULT.site(population.siteId())
                    .orElseThrow(() -> new AssertionError("missing world-map site for " + population.siteId()));
            var discoveryLocation = CanonicalLocationCatalogue.DEFAULT.location(population.siteId())
                    .orElseThrow(() -> new AssertionError("wild population site cannot be discovered: " + population.siteId()));

            assertEquals(worldSite.displayName(), discoveryLocation.displayName());
            assertEquals(worldSite.dimensionId(), discoveryLocation.dimensionId());
            assertEquals(worldSite.discoveryRadius(), discoveryLocation.triggerRadius(), 0.000001D);
        }
    }

    @Test
    void lomaWindbreakIsAConcretePersistentDiscoveryLocation() {
        var location = CanonicalLocationCatalogue.DEFAULT.location("ouros.marea.loma_windbreak");
        assertTrue(location.isPresent());
        assertEquals("Loma Clara Windbreak", location.orElseThrow().displayName());
        assertEquals(14.0D, location.orElseThrow().triggerRadius(), 0.000001D);
    }
}
