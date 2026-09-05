package io.autoptu.cobblemon.fabric.world;

/**
 * Server-authored Minecraft presentation policy for a visible wild population.
 *
 * These values govern ambient world presentation only. They do not describe PTU movement,
 * initiative, targeting, stats, moves, abilities, RNG, legality or battle outcomes.
 */
public record WildBehaviorProfile(
        double watchDistance,
        double alarmDistance,
        int recoveryQuietUpdates,
        int abandonedAlarmQuietUpdates,
        long calmSegmentTicks,
        long calmActiveTicks,
        double maxIdleHorizontalSpeed,
        double playerGuardRadius,
        float idleScanDegrees,
        double calmRoamSpeed,
        double calmStopDistance,
        double separationDistance,
        double separationSpeed,
        double cohesionDistance,
        double cohesionSpeed,
        double fleeSpeed,
        double recoverySpeed,
        double recoveryStopDistance
) {
    public WildBehaviorProfile {
        requirePositive(watchDistance, "watchDistance");
        if (!Double.isFinite(alarmDistance) || alarmDistance <= 0.0D || alarmDistance > watchDistance) {
            throw new IllegalArgumentException("alarmDistance must be finite, positive and <= watchDistance");
        }
        if (recoveryQuietUpdates <= 0 || abandonedAlarmQuietUpdates <= 0) {
            throw new IllegalArgumentException("quiet update thresholds must be positive");
        }
        if (calmSegmentTicks <= 0L || calmActiveTicks < 0L || calmActiveTicks >= calmSegmentTicks) {
            throw new IllegalArgumentException("calm cadence requires 0 <= active ticks < segment ticks");
        }
        if (!Double.isFinite(maxIdleHorizontalSpeed) || maxIdleHorizontalSpeed < 0.0D) {
            throw new IllegalArgumentException("maxIdleHorizontalSpeed must be finite and non-negative");
        }
        requirePositive(playerGuardRadius, "playerGuardRadius");
        if (!Float.isFinite(idleScanDegrees) || idleScanDegrees < 0.0F || idleScanDegrees > 180.0F) {
            throw new IllegalArgumentException("idleScanDegrees must be finite and between 0 and 180");
        }
        requirePositive(calmRoamSpeed, "calmRoamSpeed");
        requirePositive(calmStopDistance, "calmStopDistance");
        requirePositive(separationDistance, "separationDistance");
        requirePositive(separationSpeed, "separationSpeed");
        requirePositive(cohesionDistance, "cohesionDistance");
        requirePositive(cohesionSpeed, "cohesionSpeed");
        requirePositive(fleeSpeed, "fleeSpeed");
        requirePositive(recoverySpeed, "recoverySpeed");
        requirePositive(recoveryStopDistance, "recoveryStopDistance");
        if (cohesionDistance <= separationDistance) {
            throw new IllegalArgumentException("cohesionDistance must be greater than separationDistance");
        }
    }

    /**
     * Compatibility constructor for existing authored profiles that predate global roaming policy.
     * The defaults preserve the already-shipped ambient motion values while callers migrate their
     * tuning into explicit profile data.
     */
    public WildBehaviorProfile(
            double watchDistance,
            double alarmDistance,
            int recoveryQuietUpdates,
            int abandonedAlarmQuietUpdates,
            long calmSegmentTicks,
            long calmActiveTicks,
            double maxIdleHorizontalSpeed,
            double playerGuardRadius,
            float idleScanDegrees
    ) {
        this(
                watchDistance,
                alarmDistance,
                recoveryQuietUpdates,
                abandonedAlarmQuietUpdates,
                calmSegmentTicks,
                calmActiveTicks,
                maxIdleHorizontalSpeed,
                playerGuardRadius,
                idleScanDegrees,
                0.025D,
                1.0D,
                2.5D,
                0.018D,
                6.0D,
                0.012D,
                0.08D,
                0.04D,
                1.5D);
    }

    public AmbientPokemonBehaviorController.Profile proximityProfile() {
        return new AmbientPokemonBehaviorController.Profile(
                watchDistance,
                alarmDistance,
                recoveryQuietUpdates,
                abandonedAlarmQuietUpdates);
    }

    public boolean calmMovementActive(long worldTime) {
        return Math.floorMod(worldTime, calmSegmentTicks) < calmActiveTicks;
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
