package io.autoptu.cobblemon.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

final class MigrationRuntimeTest {

    private final MigrationRuntime runtime = new MigrationRuntime();

    @Test
    void routeDisruptionBlocksDepartureWithoutChangingPopulation() {
        MigrationRoute route = route();
        MigrationCohortState state = cohort(MigrationPhase.PREPARING, -1, 0, 0.5, true);

        MigrationCohortState next = runtime.advance(route, state, 500);

        assertSame(state, next);
        assertEquals(120, next.populationCount());
    }

    @Test
    void cohortMovesThroughStopoverAndRecoversWithoutPopulationDuplication() {
        MigrationRoute route = route();
        MigrationCohortState state = cohort(MigrationPhase.IN_TRANSIT, -1, 100, 0.4, false);

        MigrationCohortState atStopover = runtime.advance(route, state, 300);
        assertEquals(MigrationPhase.STOPOVER, atStopover.phase());
        assertEquals(0, atStopover.stopoverIndex());
        assertEquals(120, atStopover.populationCount());

        MigrationCohortState recovered = runtime.advance(route, atStopover, 320);
        assertEquals(MigrationPhase.IN_TRANSIT, recovered.phase());
        assertEquals(0.8, recovered.condition(), 0.0001);
        assertEquals(120, recovered.populationCount());
    }

    @Test
    void cohortCannotEnterStopoverBeforeItsArrivalWindow() {
        MigrationRoute route = new MigrationRoute(
                "north-route",
                100,
                100,
                100,
                List.of(new MigrationStopover("ridge", 500, 900, 200, 0.01))
        );
        MigrationCohortState state = cohort(MigrationPhase.IN_TRANSIT, -1, 100, 0.5, false);

        MigrationCohortState next = runtime.advance(route, state, 300);

        assertSame(state, next);
    }

    @Test
    void finalTransitEndsAtArrivalInsteadOfCreatingAnotherStopover() {
        MigrationRoute route = route();
        MigrationCohortState state = cohort(MigrationPhase.IN_TRANSIT, 0, 400, 0.9, false);

        MigrationCohortState arriving = runtime.advance(route, state, 550);
        assertEquals(MigrationPhase.ARRIVING, arriving.phase());
        assertEquals(120, arriving.populationCount());

        MigrationCohortState resident = runtime.advance(route, arriving, 551);
        assertEquals(MigrationPhase.SEASONAL_RESIDENCE, resident.phase());
        assertEquals(120, resident.populationCount());
    }

    private static MigrationRoute route() {
        return new MigrationRoute(
                "sendero-mirador",
                100,
                200,
                150,
                List.of(new MigrationStopover("mirador-stopover", 250, 700, 160, 0.02))
        );
    }

    private static MigrationCohortState cohort(
            MigrationPhase phase,
            int stopoverIndex,
            long phaseEnteredTick,
            double condition,
            boolean disrupted
    ) {
        return new MigrationCohortState(
                "cohort-a",
                "species:test",
                120,
                phase,
                stopoverIndex,
                phaseEnteredTick,
                condition,
                disrupted
        );
    }
}
