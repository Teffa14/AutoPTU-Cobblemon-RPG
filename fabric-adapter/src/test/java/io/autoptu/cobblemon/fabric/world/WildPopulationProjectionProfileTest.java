package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildPopulationProjectionProfileTest {
    private static final CanonicalWildPopulationCatalogue.PopulationDefinition POPULATION =
            new CanonicalWildPopulationCatalogue.PopulationDefinition(
                    "ouros.test.wild.ridge.v1",
                    "ouros.test.ridge_home",
                    "ouros.test.ridge",
                    List.of());

    @Test
    void authoredWindowsDriveHomeTransitAndSeasonalProjectionWithoutRegionCode() {
        var profile = profile();

        assertEquals("ouros.test.ridge_home", profile.projectedSiteId(POPULATION, 0L).orElseThrow());
        assertTrue(profile.projectedSiteId(POPULATION, 50L).isEmpty());
        assertEquals("ouros.test.marsh", profile.projectedSiteId(POPULATION, 75L).orElseThrow());
        assertEquals("ouros.test.ridge_home", profile.projectedSiteId(POPULATION, 100L).orElseThrow());
        assertEquals(MigrationPhase.IN_TRANSIT, profile.resolve(POPULATION, 50L).phase());
    }

    @Test
    void fixtureResolverComposesAuthoredProfilesAndKeepsUnprofiledPopulationsAtHome() {
        var resolver = projectionResolver(List.of(profile()));
        var resident = new CanonicalWildPopulationCatalogue.PopulationDefinition(
                "ouros.other.wild.resident.v1",
                "ouros.other.home",
                "ouros.other.zone",
                List.of());

        assertTrue(resolver.projectedSiteId(POPULATION, 50L).isEmpty());
        assertEquals("ouros.test.marsh", resolver.projectedSiteId(POPULATION, 75L).orElseThrow());
        assertEquals("ouros.other.home", resolver.projectedSiteId(resident, 75L).orElseThrow());
    }

    @Test
    void fixtureResolverRejectsMultipleProfilesForTheSamePopulation() {
        assertThrows(IllegalArgumentException.class, () ->
                projectionResolver(List.of(profile(), profile())));
    }

    @Test
    void profileLeavesUnmatchedPopulationAtItsAuthoredHome() {
        var profile = new WildPopulationProjectionProfile(
                "ouros.test.migration.ridge_to_marsh.v1",
                POPULATION.populationId(),
                10L,
                List.of(WildPopulationProjectionProfile.Window.hidden(0L, 10L, MigrationPhase.IN_TRANSIT)));
        var resident = new CanonicalWildPopulationCatalogue.PopulationDefinition(
                "ouros.other.wild.resident.v1",
                "ouros.other.home",
                "ouros.other.zone",
                List.of());

        var projection = profile.resolve(resident, 5L);

        assertEquals(MigrationPhase.PREPARING, projection.phase());
        assertEquals("ouros.other.home", projection.siteId().orElseThrow());
    }

    @Test
    void profileRejectsGapsAndIncompleteCycles() {
        assertThrows(IllegalArgumentException.class, () -> new WildPopulationProjectionProfile(
                "ouros.test.bad.v1",
                POPULATION.populationId(),
                100L,
                List.of(
                        WildPopulationProjectionProfile.Window.home(0L, 40L, MigrationPhase.PREPARING),
                        WildPopulationProjectionProfile.Window.site(
                                50L, 100L, MigrationPhase.SEASONAL_RESIDENCE, "ouros.test.marsh"))));

        assertThrows(IllegalArgumentException.class, () -> new WildPopulationProjectionProfile(
                "ouros.test.short.v1",
                POPULATION.populationId(),
                100L,
                List.of(WildPopulationProjectionProfile.Window.home(0L, 99L, MigrationPhase.PREPARING))));
    }

    private static ProjectedSiteResolver projectionResolver(List<WildPopulationProjectionProfile> profiles) {
        if (profiles == null) throw new IllegalArgumentException("profiles are required");
        Map<String, WildPopulationProjectionProfile> byPopulation = new LinkedHashMap<>();
        for (WildPopulationProjectionProfile profile : profiles) {
            if (profile == null) throw new IllegalArgumentException("projection profile is required");
            WildPopulationProjectionProfile previous = byPopulation.putIfAbsent(profile.populationId(), profile);
            if (previous != null) {
                throw new IllegalArgumentException("multiple projection profiles for population: " + profile.populationId());
            }
        }
        Map<String, WildPopulationProjectionProfile> immutableProfiles = Map.copyOf(byPopulation);
        return (population, worldTick) -> {
            if (population == null) throw new IllegalArgumentException("population is required");
            WildPopulationProjectionProfile profile = immutableProfiles.get(population.populationId());
            return profile == null
                    ? Optional.of(population.siteId())
                    : profile.projectedSiteId(population, worldTick);
        };
    }

    @FunctionalInterface
    private interface ProjectedSiteResolver {
        Optional<String> projectedSiteId(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        );
    }

    private static WildPopulationProjectionProfile profile() {
        return new WildPopulationProjectionProfile(
                "ouros.test.migration.ridge_to_marsh.v1",
                POPULATION.populationId(),
                100L,
                List.of(
                        WildPopulationProjectionProfile.Window.home(0L, 40L, MigrationPhase.PREPARING),
                        WildPopulationProjectionProfile.Window.hidden(40L, 60L, MigrationPhase.IN_TRANSIT),
                        WildPopulationProjectionProfile.Window.site(
                                60L, 100L, MigrationPhase.SEASONAL_RESIDENCE, "ouros.test.marsh")));
    }
}
