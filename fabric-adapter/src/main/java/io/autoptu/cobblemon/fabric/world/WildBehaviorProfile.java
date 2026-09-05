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
        float idleScanDegrees
) {
    public WildBehaviorProfile {
        if (!Double.isFinite(watchDistance) || watchDistance <= 0.0D) {
            throw new IllegalArgumentException("watchDistance must be finite and positive");
        }
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
        if (!Double.isFinite(playerGuardRadius) || playerGuardRadius <= 0.0D) {
            throw new IllegalArgumentException("playerGuardRadius must be finite and positive");
        }
        if (!Float.isFinite(idleScanDegrees) || idleScanDegrees < 0.0F || idleScanDegrees > 180.0F) {
            throw new IllegalArgumentException("idleScanDegrees must be finite and between 0 and 180");
        }
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
}
