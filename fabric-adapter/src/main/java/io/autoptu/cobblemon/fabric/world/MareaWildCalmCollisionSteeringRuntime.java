package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Minecraft-native collision steering for already-authoritative Marea ambient presentation.
 *
 * This runtime only adjusts low-speed CALM presentation movement after the ambient controller has
 * selected it. It reads Minecraft collision/navigation geometry, server-owned canonical population
 * bindings and the authored habitat leash. It never supplies PTU movement legality, targets, RNG,
 * combat state or outcomes, and it never reads Cobblemon Pokemon gameplay payload/state.
 */
public final class MareaWildCalmCollisionSteeringRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MAX_CALM_SPEED = 0.025001D;
    private static final double MIN_HORIZONTAL_SPEED = 0.000001D;
    private static final double COLLISION_PROBE_DISTANCE = 0.75D;
    private static final double NATIVE_NAVIGATION_SPEED = 0.08D;
    private static final double[] TURN_ANGLES_DEGREES = {45.0D, 90.0D, 135.0D};

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            steer(server.getOverworld());
        });
    }

    static void steer(ServerWorld world) {
        if (world == null) return;

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

                var velocity = actor.getVelocity();
                double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (speed <= MIN_HORIZONTAL_SPEED || speed > MAX_CALM_SPEED) continue;

                BlockPos anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(
                        encounter,
                        projectedSiteId.get());
                double centerX = anchor.getX() + 0.5D;
                double centerZ = anchor.getZ() + 0.5D;

                if (isCollisionFree(world, actor, velocity.x, velocity.z)) continue;

                double[] target = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(
                        actor.getUuid(),
                        world.getTime(),
                        centerX,
                        centerZ,
                        population.habitatLeashRadiusBlocks());
                if (startNativeNavigation(actor, centerX, centerZ, population.habitatLeashRadiusBlocks(), target)) {
                    // The native navigator now owns presentation locomotion toward the same authored CALM target.
                    // Remove the blocked horizontal impulse so it cannot keep pushing into the obstacle meanwhile.
                    actor.setVelocity(0.0D, velocity.y, 0.0D);
                    actor.velocityModified = true;
                    continue;
                }

                double[] safe = firstCollisionFreeVelocity(
                        world,
                        actor,
                        centerX,
                        centerZ,
                        population.habitatLeashRadiusBlocks(),
                        velocity.x,
                        velocity.z);
                actor.setVelocity(safe[0], velocity.y, safe[1]);
                actor.velocityModified = true;
                if (Math.abs(safe[0]) > MIN_HORIZONTAL_SPEED || Math.abs(safe[1]) > MIN_HORIZONTAL_SPEED) {
                    actor.setYaw((float) Math.toDegrees(Math.atan2(-safe[0], safe[1])));
                }
            }
        }
    }

    private static boolean startNativeNavigation(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double[] target
    ) {
        if (!navigationTargetInsideLeash(centerX, centerZ, leashRadiusBlocks, target[0], target[1])) return false;
        return actor.getNavigation().startMovingTo(target[0], actor.getY(), target[1], NATIVE_NAVIGATION_SPEED);
    }

    static boolean navigationTargetInsideLeash(
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double targetX,
            double targetZ
    ) {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(targetX) || !Double.isFinite(targetZ)
                || leashRadiusBlocks <= 0) {
            throw new IllegalArgumentException("native navigation target requires finite coordinates and positive leash");
        }
        double dx = targetX - centerX;
        double dz = targetZ - centerZ;
        return dx * dx + dz * dz <= (double) leashRadiusBlocks * leashRadiusBlocks;
    }

    private static double[] firstCollisionFreeVelocity(
            ServerWorld world,
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double requestedX,
            double requestedZ
    ) {
        boolean clockwiseFirst = clockwiseFirst(actor.getUuid());
        for (double angle : TURN_ANGLES_DEGREES) {
            double firstAngle = clockwiseFirst ? -angle : angle;
            double[] first = rotate(requestedX, requestedZ, firstAngle);
            if (candidateAllowed(world, actor, centerX, centerZ, leashRadiusBlocks, first)) return first;

            double[] second = rotate(requestedX, requestedZ, -firstAngle);
            if (candidateAllowed(world, actor, centerX, centerZ, leashRadiusBlocks, second)) return second;
        }
        return new double[] {0.0D, 0.0D};
    }

    private static boolean candidateAllowed(
            ServerWorld world,
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double[] velocity
    ) {
        return MareaWildAmbientBehaviorRuntime.insideLeashAfterImpulse(
                actor,
                centerX,
                centerZ,
                leashRadiusBlocks,
                velocity[0],
                velocity[1])
                && isCollisionFree(world, actor, velocity[0], velocity[1]);
    }

    private static boolean isCollisionFree(ServerWorld world, PokemonEntity actor, double velocityX, double velocityZ) {
        double speed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (speed <= MIN_HORIZONTAL_SPEED) return true;
        double scale = COLLISION_PROBE_DISTANCE / speed;
        return world.isSpaceEmpty(actor, actor.getBoundingBox().offset(velocityX * scale, 0.0D, velocityZ * scale));
    }

    static boolean clockwiseFirst(UUID actorId) {
        if (actorId == null) throw new IllegalArgumentException("actorId is required");
        return ((actorId.getMostSignificantBits() ^ actorId.getLeastSignificantBits()) & 1L) == 0L;
    }

    static double[] rotate(double x, double z, double angleDegrees) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(angleDegrees)) {
            throw new IllegalArgumentException("rotation requires finite velocity and angle");
        }
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new double[] {x * cos - z * sin, x * sin + z * cos};
    }
}
