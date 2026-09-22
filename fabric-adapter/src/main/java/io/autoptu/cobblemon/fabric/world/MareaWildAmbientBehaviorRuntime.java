package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import java.util.UUID;

/**
 * Transitional Marea helper facade for callers that have not yet moved to the global wild
 * presentation primitives. This class intentionally has no Fabric initializer and owns no tick,
 * controller, actor discovery or RPG state. All presentation math delegates to
 * {@link WildAmbientBehaviorRuntime}; Marea tuning remains the already-authored 80/60 tick cadence.
 */
final class MareaWildAmbientBehaviorRuntime {
    private static final long CALM_WANDER_SEGMENT_TICKS = 80L;
    private static final long CALM_WANDER_ACTIVE_TICKS = 60L;

    private MareaWildAmbientBehaviorRuntime() {
    }

    static boolean calmWanderActive(long worldTime) {
        return Math.floorMod(worldTime, CALM_WANDER_SEGMENT_TICKS) < CALM_WANDER_ACTIVE_TICKS;
    }

    static double[] calmRoamingTarget(
            UUID actorId,
            long worldTime,
            double centerX,
            double centerZ,
            int leashRadiusBlocks
    ) {
        return WildAmbientBehaviorRuntime.calmRoamingTarget(
                actorId,
                worldTime,
                centerX,
                centerZ,
                leashRadiusBlocks,
                CALM_WANDER_SEGMENT_TICKS);
    }

    static double[] calmSeparationImpulse(
            UUID actorId,
            double actorX,
            double actorZ,
            UUID siblingId,
            double siblingX,
            double siblingZ,
            double separationDistance,
            double separationSpeed
    ) {
        return WildAmbientBehaviorRuntime.pairImpulse(
                actorId,
                actorX,
                actorZ,
                siblingId,
                siblingX,
                siblingZ,
                separationDistance,
                separationSpeed,
                false);
    }

    static double[] calmCohesionImpulse(
            UUID actorId,
            double actorX,
            double actorZ,
            UUID siblingId,
            double siblingX,
            double siblingZ,
            double cohesionDistance,
            double cohesionSpeed
    ) {
        return WildAmbientBehaviorRuntime.pairImpulse(
                actorId,
                actorX,
                actorZ,
                siblingId,
                siblingX,
                siblingZ,
                cohesionDistance,
                cohesionSpeed,
                true);
    }

    static double[] boundedHorizontalVelocity(double requestedX, double requestedZ, double maxSpeed) {
        return WildAmbientBehaviorRuntime.boundedHorizontalVelocity(requestedX, requestedZ, maxSpeed);
    }

    static double[] recoveryImpulse(
            double actorX,
            double actorZ,
            double centerX,
            double centerZ,
            double speed
    ) {
        return WildAmbientBehaviorRuntime.recoveryImpulse(actorX, actorZ, centerX, centerZ, speed);
    }

    static boolean insideHorizontalLeash(
            double actorX,
            double actorZ,
            double centerX,
            double centerZ,
            int leashRadiusBlocks
    ) {
        return WildAmbientBehaviorRuntime.insideHorizontalLeash(
                actorX,
                actorZ,
                centerX,
                centerZ,
                leashRadiusBlocks);
    }

    static boolean insideLeashAfterImpulse(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double velocityX,
            double velocityZ
    ) {
        if (actor == null) throw new IllegalArgumentException("actor is required");
        if (!Double.isFinite(velocityX) || !Double.isFinite(velocityZ)) {
            throw new IllegalArgumentException("habitat impulse requires finite velocity");
        }
        return WildAmbientBehaviorRuntime.insideHorizontalLeash(
                actor.getX() + velocityX,
                actor.getZ() + velocityZ,
                centerX,
                centerZ,
                leashRadiusBlocks);
    }
}
