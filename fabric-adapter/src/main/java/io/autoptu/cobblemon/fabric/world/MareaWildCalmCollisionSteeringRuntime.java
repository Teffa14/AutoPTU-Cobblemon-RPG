package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

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
    private static final int MAX_CALM_NEIGHBOR_SURFACE_DELTA = 1;
    private static final double[] TURN_ANGLES_DEGREES = {45.0D, 90.0D, 135.0D};
    private static final int[][] CARDINAL_SURFACE_OFFSETS = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
    };

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
        Path path = findLeashSafeNativePath(actor, centerX, centerZ, leashRadiusBlocks, target);
        return path != null && actor.getNavigation().startMovingAlong(path, NATIVE_NAVIGATION_SPEED);
    }

    /**
     * Finds a Minecraft-native path toward the already-authored CALM X/Z destination.
     *
     * The actor's current Y remains the first attempt for flat terrain. If Minecraft reports a
     * different motion-blocking surface at the exact target column, a second attempt uses that
     * surface height. The target column must also have locally continuous cardinal surface support;
     * isolated pillars and abrupt ledge targets fall back to the existing deterministic steering
     * instead of becoming ambient destinations. This only supplies Minecraft presentation geometry;
     * X/Z destination and leash authority remain unchanged.
     */
    static Path findLeashSafeNativePath(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double[] target
    ) {
        if (actor == null) throw new IllegalArgumentException("actor is required");
        if (target == null || target.length < 2 || !Double.isFinite(target[0]) || !Double.isFinite(target[1])) {
            throw new IllegalArgumentException("native navigation target requires finite X/Z");
        }
        if (!navigationTargetInsideLeash(centerX, centerZ, leashRadiusBlocks, actor.getX(), actor.getZ())) return null;
        if (!navigationTargetInsideLeash(centerX, centerZ, leashRadiusBlocks, target[0], target[1])) return null;
        if (!(actor.getWorld() instanceof ServerWorld world)) return null;

        int targetX = MathHelper.floor(target[0]);
        int targetZ = MathHelper.floor(target[1]);
        int actorY = MathHelper.floor(actor.getY());
        int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        if (!stableCalmTargetSurface(world, targetX, targetZ, surfaceY)) return null;

        var navigation = actor.getNavigation();
        for (int targetY : navigationTargetYCandidates(actorY, surfaceY)) {
            Path path = navigation.findPathTo(target[0], targetY, target[1], 0);
            if (path == null || !path.reachesTarget()) continue;
            if (!navigationPathInsideLeash(centerX, centerZ, leashRadiusBlocks, path)) continue;
            return path;
        }
        return null;
    }

    private static boolean stableCalmTargetSurface(ServerWorld world, int targetX, int targetZ, int surfaceY) {
        int[] adjacentSurfaceY = new int[CARDINAL_SURFACE_OFFSETS.length];
        for (int index = 0; index < CARDINAL_SURFACE_OFFSETS.length; index++) {
            int[] offset = CARDINAL_SURFACE_OFFSETS[index];
            adjacentSurfaceY[index] = world.getTopY(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    targetX + offset[0],
                    targetZ + offset[1]);
        }
        return stableCalmSurfaceNeighborhood(surfaceY, adjacentSurfaceY);
    }

    static boolean stableCalmSurfaceNeighborhood(int surfaceY, int... adjacentSurfaceY) {
        if (adjacentSurfaceY == null || adjacentSurfaceY.length != CARDINAL_SURFACE_OFFSETS.length) {
            throw new IllegalArgumentException("CALM surface neighborhood requires four cardinal heights");
        }
        for (int adjacentY : adjacentSurfaceY) {
            if (Math.abs((long) adjacentY - surfaceY) > MAX_CALM_NEIGHBOR_SURFACE_DELTA) return false;
        }
        return true;
    }

    static int[] navigationTargetYCandidates(int actorY, int surfaceY) {
        if (actorY == surfaceY) return new int[] {actorY};
        return new int[] {actorY, surfaceY};
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

    static boolean navigationPathInsideLeash(
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            Path path
    ) {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ) || leashRadiusBlocks <= 0) {
            throw new IllegalArgumentException("native navigation path requires finite center and positive leash");
        }
        if (path == null || path.getLength() == 0) return false;
        for (int index = 0; index < path.getLength(); index++) {
            BlockPos node = path.getNode(index).getBlockPos();
            if (!navigationTargetInsideLeash(
                    centerX,
                    centerZ,
                    leashRadiusBlocks,
                    node.getX() + 0.5D,
                    node.getZ() + 0.5D)) {
                return false;
            }
        }
        return true;
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
