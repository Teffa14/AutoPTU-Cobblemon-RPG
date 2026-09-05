package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide continuity and revalidation for already-started Minecraft-native CALM routes.
 *
 * Actor, habitat and cadence policy come from {@link WildEcologyProjectionRegistry}; species and
 * region names are intentionally absent. This preserves presentation routes across the ambient
 * controller cadence and revokes them when the remaining world path becomes unsafe. PTU movement,
 * initiative, targeting, RNG, damage, statuses and outcomes remain outside this runtime.
 */
public final class WildCalmNavigationContinuityRuntime implements ModInitializer {
    private static final int AMBIENT_UPDATE_INTERVAL_TICKS = 10;
    private static final double NATIVE_NAVIGATION_SPEED = 0.08D;
    private static final int[][] CARDINAL_SURFACE_OFFSETS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}
    };
    private static final Map<MinecraftServer, Map<UUID, Long>> ACTIVE_SEGMENTS = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildCalmNavigationContinuityRuntime::update);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (ACTIVE_SEGMENTS) {
                ACTIVE_SEGMENTS.remove(server);
            }
        });
    }

    static void update(MinecraftServer server) {
        if (server == null) return;
        ServerWorld world = server.getOverworld();
        long worldTime = world.getTime();
        Map<UUID, Long> active = activeFor(server);
        HashSet<UUID> liveActors = new HashSet<>();

        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            PokemonEntity actor = projection.actor();
            if (actor.isRemoved() || actor.isInvisible()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

            liveActors.add(actor.getUuid());
            WildBehaviorProfile profile = projection.behaviorProfile();
            long currentSegment = calmSegment(worldTime, profile.calmSegmentTicks());
            boolean nearbyPlayer = hasNearbyPlayer(world, actor, profile.playerGuardRadius());
            boolean calmActive = profile.calmMovementActive(worldTime);
            if (!calmActive || nearbyPlayer) {
                active.remove(actor.getUuid());
                continue;
            }

            double centerX = projection.habitatCenterX();
            double centerZ = projection.habitatCenterZ();
            int leash = projection.habitatLeashRadiusBlocks();
            if (!WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(
                    centerX, centerZ, leash, actor.getX(), actor.getZ())) {
                actor.getNavigation().stop();
                active.remove(actor.getUuid());
                continue;
            }

            var navigation = actor.getNavigation();
            if (!navigation.isIdle()) {
                Path currentPath = navigation.getCurrentPath();
                if (!remainingPathStillSafe(world, actor, currentPath, centerX, centerZ, leash)) {
                    navigation.stop();
                    active.remove(actor.getUuid());
                    if (!startReplacementPath(actor, projection, worldTime)) {
                        clearHorizontalVelocity(actor);
                    } else {
                        active.put(actor.getUuid(), currentSegment);
                    }
                    continue;
                }
                active.put(actor.getUuid(), currentSegment);
                continue;
            }

            Long recordedSegment = active.get(actor.getUuid());
            if (!shouldRehydrate(recordedSegment, currentSegment, calmActive, nearbyPlayer)) {
                active.remove(actor.getUuid());
                continue;
            }
            if (server.getTicks() % AMBIENT_UPDATE_INTERVAL_TICKS != 0) continue;

            double[] target = roamingTarget(projection, worldTime);
            double dx = target[0] - actor.getX();
            double dz = target[1] - actor.getZ();
            double stop = profile.calmStopDistance();
            if (dx * dx + dz * dz <= stop * stop) {
                active.remove(actor.getUuid());
                continue;
            }

            Path path = WildCalmCollisionNavigationRuntime.findLeashSafeNativePath(
                    actor, centerX, centerZ, leash, target);
            if (path == null || !navigation.startMovingAlong(path, NATIVE_NAVIGATION_SPEED)) {
                active.remove(actor.getUuid());
            }
        }

        active.keySet().removeIf(uuid -> !liveActors.contains(uuid));
    }

    private static boolean startReplacementPath(
            PokemonEntity actor,
            WildEcologyProjectionRegistry.ProjectedActor projection,
            long worldTime
    ) {
        double[] target = roamingTarget(projection, worldTime);
        Path replacement = WildCalmCollisionNavigationRuntime.findLeashSafeNativePath(
                actor,
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                target);
        return replacement != null && actor.getNavigation().startMovingAlong(replacement, NATIVE_NAVIGATION_SPEED);
    }

    private static double[] roamingTarget(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            long worldTime
    ) {
        return WildAmbientBehaviorRuntime.calmRoamingTarget(
                projection.actor().getUuid(),
                worldTime,
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                projection.behaviorProfile().calmSegmentTicks());
    }

    static boolean remainingPathStillSafe(
            ServerWorld world,
            PokemonEntity actor,
            Path path,
            double centerX,
            double centerZ,
            int leashRadiusBlocks
    ) {
        if (world == null || actor == null || path == null || path.getLength() == 0) return false;
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ) || leashRadiusBlocks <= 0) return false;
        int currentNodeIndex = path.getCurrentNodeIndex();
        if (currentNodeIndex < 0 || currentNodeIndex >= path.getLength()) return false;

        int remainingNodeCount = path.getLength() - currentNodeIndex;
        int[] surfaceProfile = new int[remainingNodeCount];
        boolean[] collisionProfile = new boolean[remainingNodeCount];
        for (int pathIndex = currentNodeIndex, profileIndex = 0;
             pathIndex < path.getLength(); pathIndex++, profileIndex++) {
            BlockPos node = path.getNode(pathIndex).getBlockPos();
            if (!WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(
                    centerX, centerZ, leashRadiusBlocks,
                    node.getX() + 0.5D, node.getZ() + 0.5D)) return false;

            int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, node.getX(), node.getZ());
            int[] adjacentSurfaceY = new int[CARDINAL_SURFACE_OFFSETS.length];
            for (int index = 0; index < CARDINAL_SURFACE_OFFSETS.length; index++) {
                int[] offset = CARDINAL_SURFACE_OFFSETS[index];
                adjacentSurfaceY[index] = world.getTopY(
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        node.getX() + offset[0], node.getZ() + offset[1]);
            }
            if (!WildCalmCollisionNavigationRuntime.stableCalmSurfaceNeighborhood(surfaceY, adjacentSurfaceY)) {
                return false;
            }
            surfaceProfile[profileIndex] = surfaceY;
            collisionProfile[profileIndex] = pathNodeVolumeClear(world, actor, node);
        }
        return remainingSurfaceProfileContinuous(0, surfaceProfile)
                && remainingCollisionProfileClear(0, collisionProfile);
    }

    private static boolean pathNodeVolumeClear(ServerWorld world, PokemonEntity actor, BlockPos node) {
        double offsetX = node.getX() + 0.5D - actor.getX();
        double offsetY = node.getY() - actor.getY();
        double offsetZ = node.getZ() + 0.5D - actor.getZ();
        var projectedBox = actor.getBoundingBox().offset(offsetX, offsetY, offsetZ);
        boolean blockSpaceClear = world.isSpaceEmpty(actor, projectedBox);
        boolean activeWildOverlap = !world.getOtherEntities(
                actor,
                projectedBox,
                candidate -> candidate instanceof PokemonEntity
                        && VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.getUuid())).isEmpty();
        return presentationNodeClear(blockSpaceClear, activeWildOverlap);
    }

    static boolean presentationNodeClear(boolean blockSpaceClear, boolean activeWildOverlap) {
        return blockSpaceClear && !activeWildOverlap;
    }

    static boolean remainingSurfaceProfileContinuous(int currentNodeIndex, int... surfaceY) {
        if (surfaceY == null || surfaceY.length == 0
                || currentNodeIndex < 0 || currentNodeIndex >= surfaceY.length) return false;
        int previous = surfaceY[currentNodeIndex];
        for (int index = currentNodeIndex + 1; index < surfaceY.length; index++) {
            if (Math.abs((long) surfaceY[index] - previous) > 1L) return false;
            previous = surfaceY[index];
        }
        return true;
    }

    static boolean remainingCollisionProfileClear(int currentNodeIndex, boolean... collisionFree) {
        if (collisionFree == null || collisionFree.length == 0
                || currentNodeIndex < 0 || currentNodeIndex >= collisionFree.length) return false;
        for (int index = currentNodeIndex; index < collisionFree.length; index++) {
            if (!collisionFree[index]) return false;
        }
        return true;
    }

    static long calmSegment(long worldTime, long segmentTicks) {
        if (segmentTicks <= 0L) throw new IllegalArgumentException("segmentTicks must be positive");
        return Math.floorDiv(worldTime, segmentTicks);
    }

    static boolean shouldRehydrate(
            Long recordedSegment,
            long currentSegment,
            boolean calmActive,
            boolean nearbyPlayer
    ) {
        return recordedSegment != null
                && recordedSegment == currentSegment
                && calmActive
                && !nearbyPlayer;
    }

    private static boolean hasNearbyPlayer(
            ServerWorld world,
            PokemonEntity actor,
            double playerGuardRadius
    ) {
        double limitSquared = playerGuardRadius * playerGuardRadius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (actor.squaredDistanceTo(player) <= limitSquared) return true;
        }
        return false;
    }

    private static void clearHorizontalVelocity(PokemonEntity actor) {
        var velocity = actor.getVelocity();
        actor.setVelocity(0.0D, velocity.y, 0.0D);
        actor.velocityModified = true;
    }

    private static Map<UUID, Long> activeFor(MinecraftServer server) {
        synchronized (ACTIVE_SEGMENTS) {
            return ACTIVE_SEGMENTS.computeIfAbsent(server, ignored -> new HashMap<>());
        }
    }
}
