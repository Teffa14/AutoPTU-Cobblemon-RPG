package io.autoptu.cobblemon.ecology;

import java.util.Objects;

public final class MigrationRuntime {

    public MigrationCohortState advance(MigrationRoute route, MigrationCohortState state, long worldTick) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(state, "state");
        if (worldTick < 0) throw new IllegalArgumentException("worldTick must be >= 0");
        if (worldTick < state.phaseEnteredTick()) return state;

        return switch (state.phase()) {
            case PREPARING -> advancePreparing(route, state, worldTick);
            case DEPARTING -> state.withPhase(MigrationPhase.IN_TRANSIT, -1, worldTick);
            case IN_TRANSIT -> advanceTransit(route, state, worldTick);
            case STOPOVER -> advanceStopover(route, state, worldTick);
            case ARRIVING -> state.withPhase(MigrationPhase.SEASONAL_RESIDENCE, state.stopoverIndex(), worldTick);
            case SEASONAL_RESIDENCE, RETURNING, COMPLETE -> state;
        };
    }

    private MigrationCohortState advancePreparing(MigrationRoute route, MigrationCohortState state, long worldTick) {
        if (state.routeDisrupted() || worldTick < route.departureTick()) return state;
        return state.withPhase(MigrationPhase.DEPARTING, -1, worldTick);
    }

    private MigrationCohortState advanceTransit(MigrationRoute route, MigrationCohortState state, long worldTick) {
        long requiredTicks = state.stopoverIndex() < 0
                ? route.transitTicksBetweenStops()
                : (state.stopoverIndex() + 1 < route.stopovers().size()
                    ? route.transitTicksBetweenStops()
                    : route.finalTransitTicks());

        if (worldTick - state.phaseEnteredTick() < requiredTicks) return state;

        int nextStopoverIndex = state.stopoverIndex() + 1;
        if (nextStopoverIndex < route.stopovers().size()) {
            MigrationStopover stopover = route.stopovers().get(nextStopoverIndex);
            if (worldTick < stopover.minimumArrivalTick()) return state;
            return state.withPhase(MigrationPhase.STOPOVER, nextStopoverIndex, worldTick);
        }

        return state.withPhase(MigrationPhase.ARRIVING, state.stopoverIndex(), worldTick);
    }

    private MigrationCohortState advanceStopover(MigrationRoute route, MigrationCohortState state, long worldTick) {
        MigrationStopover stopover = route.stopovers().get(state.stopoverIndex());
        long elapsed = worldTick - state.phaseEnteredTick();
        MigrationCohortState recovered = state.withCondition(
                state.condition() + elapsed * stopover.resourceRecoveryPerTick()
        );

        boolean mustLeave = worldTick >= stopover.maximumDepartureTick();
        boolean recoveredEnough = recovered.condition() >= 0.8;
        if (!mustLeave && !recoveredEnough) return recovered;

        return recovered.withPhase(MigrationPhase.IN_TRANSIT, state.stopoverIndex(), worldTick);
    }
}
