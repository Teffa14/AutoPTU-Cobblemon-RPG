package io.autoptu.cobblemon.ecology;

import java.util.Objects;

public record MigrationCohortState(
        String cohortId,
        String speciesId,
        int populationCount,
        MigrationPhase phase,
        int stopoverIndex,
        long phaseEnteredTick,
        double condition,
        boolean routeDisrupted
) {
    public MigrationCohortState {
        Objects.requireNonNull(cohortId, "cohortId");
        Objects.requireNonNull(speciesId, "speciesId");
        Objects.requireNonNull(phase, "phase");
        if (cohortId.isBlank()) throw new IllegalArgumentException("cohortId must not be blank");
        if (speciesId.isBlank()) throw new IllegalArgumentException("speciesId must not be blank");
        if (populationCount <= 0) throw new IllegalArgumentException("populationCount must be > 0");
        if (stopoverIndex < -1) throw new IllegalArgumentException("stopoverIndex must be >= -1");
        if (phaseEnteredTick < 0) throw new IllegalArgumentException("phaseEnteredTick must be >= 0");
        if (condition < 0.0 || condition > 1.0) {
            throw new IllegalArgumentException("condition must be in [0,1]");
        }
    }

    public MigrationCohortState withPhase(MigrationPhase nextPhase, int nextStopoverIndex, long tick) {
        return new MigrationCohortState(
                cohortId,
                speciesId,
                populationCount,
                nextPhase,
                nextStopoverIndex,
                tick,
                condition,
                routeDisrupted
        );
    }

    public MigrationCohortState withCondition(double nextCondition) {
        double clamped = Math.max(0.0, Math.min(1.0, nextCondition));
        return new MigrationCohortState(
                cohortId,
                speciesId,
                populationCount,
                phase,
                stopoverIndex,
                phaseEnteredTick,
                clamped,
                routeDisrupted
        );
    }

    public MigrationCohortState withRouteDisrupted(boolean disrupted) {
        return new MigrationCohortState(
                cohortId,
                speciesId,
                populationCount,
                phase,
                stopoverIndex,
                phaseEnteredTick,
                condition,
                disrupted
        );
    }
}
