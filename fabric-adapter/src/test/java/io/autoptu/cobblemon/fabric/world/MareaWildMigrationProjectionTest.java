package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MareaWildMigrationProjectionTest {
    private static final String HOME_SITE = "ouros.marea.sendero_vidrio";
    private static final String STOPOVER_SITE = "ouros.marea.sendero_crossing";

    @Test
    void lowerShelfPopulationMovesThroughDeterministicServerClockPhases() {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();

        assertEquals(HOME_SITE, MareaWildMigrationProjection.projectedSiteId(population, 0L).orElseThrow());
        assertEquals(HOME_SITE, MareaWildMigrationProjection.projectedSiteId(population, 84_000L).orElseThrow());
        assertTrue(MareaWildMigrationProjection.projectedSiteId(population, 85_000L).isEmpty());
        assertEquals(STOPOVER_SITE, MareaWildMigrationProjection.projectedSiteId(population, 100_000L).orElseThrow());
        assertEquals(HOME_SITE, MareaWildMigrationProjection.projectedSiteId(population, 168_000L).orElseThrow());
    }

    @Test
    void projectionIsStableAcrossRepeatedResolutionAndLeavesOtherPopulationsAtTheirAuthoredSite() {
        var migrating = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var resident = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID)
                .orElseThrow();

        var first = MareaWildMigrationProjection.projectedSiteId(migrating, 100_000L);
        var second = MareaWildMigrationProjection.projectedSiteId(migrating, 100_000L);
        assertEquals(first, second);
        assertEquals(resident.siteId(), MareaWildMigrationProjection.projectedSiteId(resident, 100_000L).orElseThrow());
    }
}
