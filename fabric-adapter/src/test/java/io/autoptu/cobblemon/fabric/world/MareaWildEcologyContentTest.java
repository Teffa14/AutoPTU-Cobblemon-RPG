package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MareaWildEcologyContentTest {
    private static final String HOME_SITE = "ouros.marea.sendero_vidrio";
    private static final String STOPOVER_SITE = "ouros.marea.sendero_crossing";

    @Test
    void lowerShelfMigrationIsAuthoredDataConsumedByTheGlobalProjectionResolver() {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var resolver = WildPopulationContentRegistry.projectionResolver(MareaWildEcologyContent.projectionProfiles());

        assertEquals(HOME_SITE, resolver.projectedSiteId(population, 0L).orElseThrow());
        assertEquals(HOME_SITE, resolver.projectedSiteId(population, 84_000L).orElseThrow());
        assertTrue(resolver.projectedSiteId(population, 85_000L).isEmpty());
        assertEquals(STOPOVER_SITE, resolver.projectedSiteId(population, 100_000L).orElseThrow());
        assertEquals(HOME_SITE, resolver.projectedSiteId(population, 168_000L).orElseThrow());
    }

    @Test
    void repeatedResolutionIsStableAndResidentMareaPopulationFallsBackToItsAuthoredHome() {
        var migrating = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var resident = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID)
                .orElseThrow();
        var resolver = WildPopulationContentRegistry.projectionResolver(MareaWildEcologyContent.projectionProfiles());

        var first = resolver.projectedSiteId(migrating, 100_000L);
        var second = resolver.projectedSiteId(migrating, 100_000L);

        assertEquals(first, second);
        assertEquals(resident.siteId(), resolver.projectedSiteId(resident, 100_000L).orElseThrow());
    }

    @Test
    void contentRegistersProjectionAndBehaviorDataWithoutARegionalRuntime() {
        assertEquals(1, MareaWildEcologyContent.projectionProfiles().size());
        assertEquals(
                CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
                MareaWildEcologyContent.projectionProfiles().getFirst().populationId()
        );
        assertEquals(1, MareaWildEcologyContent.ecologyProjectionSources().size());

        var source = MareaWildEcologyContent.ecologyProjectionSources().getFirst();
        var marea = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        assertEquals("fixture.ouros.marea", source.sourceId());
        assertTrue(source.populationSelector().test(marea));
        assertFalse(source.behaviorProfile().equals(null));
        assertSame(source.behaviorProfile(), MareaWildEcologyContent.ecologyProjectionSources().getFirst().behaviorProfile());
    }

    @Test
    void recoveryAnchorUsesCanonicalHomeSiteAndAuthoredPresentationOffset() {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var encounter = CanonicalWildPopulationCatalogue.DEFAULT.members(population).getFirst();
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId()).orElseThrow();

        var anchor = MareaWildMigrationRuntime.canonicalHomeAnchor(encounter);

        assertEquals(site.x() + encounter.presentationOffsetX(), anchor.getX());
        assertEquals(site.y() + encounter.presentationOffsetY(), anchor.getY());
        assertEquals(site.z() + encounter.presentationOffsetZ(), anchor.getZ());
    }

    @Test
    void activeMigrationUsesRetentionFootprintToAvoidEdgeFlicker() {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();

        assertEquals(population.presenceFootprint(), MareaWildMigrationRuntime.activityFootprint(population, false));
        assertEquals(population.retentionFootprint(), MareaWildMigrationRuntime.activityFootprint(population, true));
    }

    @Test
    void unifiedPresenceReconcilerUsesProjectedStopoverAnchorWithoutChangingEncounterIdentity() {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var encounter = CanonicalWildPopulationCatalogue.DEFAULT.members(population).getFirst();
        var stopover = CanonicalWorldMapCatalogue.DEFAULT.site(STOPOVER_SITE).orElseThrow();

        var anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(encounter, STOPOVER_SITE);

        assertEquals(stopover.x() + encounter.presentationOffsetX(), anchor.getX());
        assertEquals(stopover.y() + encounter.presentationOffsetY(), anchor.getY());
        assertEquals(stopover.z() + encounter.presentationOffsetZ(), anchor.getZ());
        assertEquals(HOME_SITE, encounter.siteId());
    }
}
