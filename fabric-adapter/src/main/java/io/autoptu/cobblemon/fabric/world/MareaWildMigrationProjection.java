package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;

import java.util.List;
import java.util.Optional;

/**
 * Authored Marea projection data for the lower-shelf migration cohort.
 *
 * <p>The schedule is content. Resolution belongs to {@link WildPopulationProjectionProfile}, so a
 * second region or population can author another profile without adding another lifecycle algorithm.</p>
 */
final class MareaWildMigrationProjection {
    private static final long CYCLE_TICKS = 168_000L;
    private static final long DEPARTURE_TICK = 84_000L;
    private static final long OUTBOUND_TRANSIT_END_TICK = 90_001L;
    private static final long STOPOVER_END_TICK = 90_002L;
    private static final long FINAL_TRANSIT_END_TICK = 90_003L;
    private static final long ARRIVAL_END_TICK = 90_004L;
    private static final String STOPOVER_SITE_ID = "ouros.marea.sendero_crossing";

    private static final WildPopulationProjectionProfile PROFILE = new WildPopulationProjectionProfile(
            "ouros.marea.migration.lower_shelf_to_crossing.v1",
            CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
            CYCLE_TICKS,
            List.of(
                    WildPopulationProjectionProfile.Window.home(
                            0L, DEPARTURE_TICK, MigrationPhase.PREPARING),
                    WildPopulationProjectionProfile.Window.home(
                            DEPARTURE_TICK, DEPARTURE_TICK + 1L, MigrationPhase.DEPARTING),
                    WildPopulationProjectionProfile.Window.hidden(
                            DEPARTURE_TICK + 1L, OUTBOUND_TRANSIT_END_TICK, MigrationPhase.IN_TRANSIT),
                    WildPopulationProjectionProfile.Window.site(
                            OUTBOUND_TRANSIT_END_TICK, STOPOVER_END_TICK, MigrationPhase.STOPOVER, STOPOVER_SITE_ID),
                    WildPopulationProjectionProfile.Window.site(
                            STOPOVER_END_TICK, FINAL_TRANSIT_END_TICK, MigrationPhase.IN_TRANSIT, STOPOVER_SITE_ID),
                    WildPopulationProjectionProfile.Window.site(
                            FINAL_TRANSIT_END_TICK, ARRIVAL_END_TICK, MigrationPhase.ARRIVING, STOPOVER_SITE_ID),
                    WildPopulationProjectionProfile.Window.site(
                            ARRIVAL_END_TICK, CYCLE_TICKS, MigrationPhase.SEASONAL_RESIDENCE, STOPOVER_SITE_ID)
            )
    );

    private MareaWildMigrationProjection() {}

    static Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        return PROFILE.projectedSiteId(population, worldTick);
    }

    static MigrationPhase phase(long worldTick) {
        var population = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow(() -> new IllegalStateException("missing authored Marea lower-shelf population"));
        return PROFILE.resolve(population, worldTick).phase();
    }

    static WildPopulationProjectionProfile profile() {
        return PROFILE;
    }
}
