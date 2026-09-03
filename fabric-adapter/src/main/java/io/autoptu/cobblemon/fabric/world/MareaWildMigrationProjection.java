package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationCohortState;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import io.autoptu.cobblemon.ecology.MigrationRoute;
import io.autoptu.cobblemon.ecology.MigrationRuntime;
import io.autoptu.cobblemon.ecology.MigrationStopover;

import java.util.List;
import java.util.Optional;

/**
 * Server-authored world projection for a bounded Marea migration slice.
 *
 * <p>The migration changes only where an already-canonical roaming population is projected in
 * Minecraft. Encounter identity, species, PTU profile, stats, moves, HP and battle legality remain
 * untouched. Resolution is deterministic from the server world clock, so restart/reconnect cannot
 * create a second migration authority or duplicate population state.</p>
 */
final class MareaWildMigrationProjection {
    private static final long CYCLE_TICKS = 168_000L;
    private static final long DEPARTURE_TICK = 84_000L;
    private static final long OUTBOUND_TRANSIT_TICKS = 6_000L;
    private static final long FINAL_TRANSIT_TICKS = 1L;
    private static final String MIGRATING_POPULATION_ID =
            CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID;
    private static final String STOPOVER_SITE_ID = "ouros.marea.sendero_crossing";

    private static final MigrationRuntime RUNTIME = new MigrationRuntime();
    private static final MigrationRoute ROUTE = new MigrationRoute(
            "ouros.marea.migration.lower_shelf_to_crossing.v1",
            DEPARTURE_TICK,
            OUTBOUND_TRANSIT_TICKS,
            FINAL_TRANSIT_TICKS,
            List.of(new MigrationStopover(
                    STOPOVER_SITE_ID,
                    DEPARTURE_TICK + OUTBOUND_TRANSIT_TICKS,
                    CYCLE_TICKS - 1L,
                    8,
                    1.0D
            ))
    );

    private MareaWildMigrationProjection() {}

    static Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        if (population == null) throw new IllegalArgumentException("population is required");
        if (worldTick < 0L) throw new IllegalArgumentException("worldTick must be >= 0");
        if (!MIGRATING_POPULATION_ID.equals(population.populationId())) {
            return Optional.of(population.siteId());
        }

        MigrationCohortState state = resolve(worldTick);
        return switch (state.phase()) {
            case PREPARING, DEPARTING -> Optional.of(population.siteId());
            case IN_TRANSIT -> state.stopoverIndex() < 0
                    ? Optional.empty()
                    : Optional.of(ROUTE.stopovers().get(state.stopoverIndex()).id());
            case STOPOVER -> Optional.of(ROUTE.stopovers().get(state.stopoverIndex()).id());
            case ARRIVING, SEASONAL_RESIDENCE -> Optional.of(STOPOVER_SITE_ID);
            case RETURNING, COMPLETE -> Optional.of(population.siteId());
        };
    }

    static MigrationPhase phase(long worldTick) {
        return resolve(worldTick).phase();
    }

    private static MigrationCohortState resolve(long worldTick) {
        long cycleTick = Math.floorMod(worldTick, CYCLE_TICKS);
        MigrationCohortState state = new MigrationCohortState(
                "ouros.marea.cohort.lower_shelf.fletchling.v1",
                "fletchling",
                2,
                MigrationPhase.PREPARING,
                -1,
                0L,
                0.4D,
                false
        );
        if (cycleTick < DEPARTURE_TICK) return state;

        state = RUNTIME.advance(ROUTE, state, DEPARTURE_TICK);
        if (cycleTick < DEPARTURE_TICK + 1L) return state;

        state = RUNTIME.advance(ROUTE, state, DEPARTURE_TICK + 1L);
        long stopoverArrival = DEPARTURE_TICK + 1L + OUTBOUND_TRANSIT_TICKS;
        if (cycleTick < stopoverArrival) return state;

        state = RUNTIME.advance(ROUTE, state, stopoverArrival);
        if (cycleTick < stopoverArrival + 1L) return state;

        state = RUNTIME.advance(ROUTE, state, stopoverArrival + 1L);
        long finalArrival = stopoverArrival + 1L + FINAL_TRANSIT_TICKS;
        if (cycleTick < finalArrival) return state;

        state = RUNTIME.advance(ROUTE, state, finalArrival);
        if (cycleTick < finalArrival + 1L) return state;
        return RUNTIME.advance(ROUTE, state, finalArrival + 1L);
    }
}
