package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonBlocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only bridge from Cobblemon 1.8 Habitat Blocks to AutoPTU ambient ecology presentation.
 *
 * <p>The block itself is treated only as a physical environmental point of interest. This class
 * deliberately does not inspect {@code HabitatBlockEntity} configuration, spawn pools, species,
 * levels, modifiers, weights or native spawn outcomes. Canonical WILD population and encounter
 * authority remains in AutoPTU-owned descriptors and blueprints.</p>
 */
final class CobblemonHabitatPointOfInterest {
    private static final int VERTICAL_SCAN_RADIUS_BLOCKS = 16;
    private static final int SURFACE_SCAN_BELOW_BLOCKS = 4;
    private static final int SURFACE_SCAN_ABOVE_BLOCKS = 2;
    private static final int MAX_HORIZONTAL_SCAN_RADIUS_BLOCKS = 24;
    private static final int CACHE_TICKS = 100;
    private static final Map<MinecraftServer, Map<String, CachedPoint>> CACHE = new IdentityHashMap<>();

    private CobblemonHabitatPointOfInterest() {}

    static Optional<BlockPos> nearest(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection
    ) {
        if (world == null || projection == null || world.getServer() == null) return Optional.empty();

        MinecraftServer server = world.getServer();
        String key = cacheKey(world, projection);
        long now = server.getTicks();
        synchronized (CACHE) {
            CachedPoint cached = CACHE.computeIfAbsent(server, ignored -> new LinkedHashMap<>()).get(key);
            if (cached != null && now < cached.refreshAtTick()) return cached.position();
        }

        Optional<BlockPos> resolved = scan(world, projection);
        synchronized (CACHE) {
            CACHE.computeIfAbsent(server, ignored -> new LinkedHashMap<>())
                    .put(key, new CachedPoint(resolved, now + CACHE_TICKS));
        }
        return resolved;
    }

    static void clear(MinecraftServer server) {
        if (server == null) return;
        synchronized (CACHE) {
            CACHE.remove(server);
        }
    }

    private static Optional<BlockPos> scan(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection
    ) {
        int horizontalRadius = Math.min(
                projection.habitatLeashRadiusBlocks(),
                MAX_HORIZONTAL_SCAN_RADIUS_BLOCKS);
        int centerX = (int) Math.floor(projection.habitatCenterX());
        int actorY = projection.actor().getBlockY();
        int centerZ = (int) Math.floor(projection.habitatCenterZ());
        Set<BlockPos> candidates = new LinkedHashSet<>();

        double leashSquared = (double) projection.habitatLeashRadiusBlocks()
                * projection.habitatLeashRadiusBlocks();
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                double worldX = x + 0.5D;
                double worldZ = z + 0.5D;
                double leashDx = worldX - projection.habitatCenterX();
                double leashDz = worldZ - projection.habitatCenterZ();
                if (leashDx * leashDx + leashDz * leashDz > leashSquared) continue;

                BlockPos loadedProbe = new BlockPos(x, actorY, z);
                if (!world.isChunkLoaded(loadedProbe)) continue;

                scanVerticalWindow(
                        world,
                        x,
                        z,
                        actorY - VERTICAL_SCAN_RADIUS_BLOCKS,
                        actorY + VERTICAL_SCAN_RADIUS_BLOCKS,
                        candidates);

                int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                scanVerticalWindow(
                        world,
                        x,
                        z,
                        surfaceY - SURFACE_SCAN_BELOW_BLOCKS,
                        surfaceY + SURFACE_SCAN_ABOVE_BLOCKS,
                        candidates);
            }
        }

        return selectNearest(
                new ArrayList<>(candidates),
                projection.actor().getX(),
                projection.actor().getY(),
                projection.actor().getZ(),
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks());
    }

    private static void scanVerticalWindow(
            ServerWorld world,
            int x,
            int z,
            int minY,
            int maxY,
            Set<BlockPos> candidates
    ) {
        for (int y = minY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).isOf(CobblemonBlocks.HABITAT_BLOCK)) {
                candidates.add(pos.toImmutable());
            }
        }
    }

    static Optional<BlockPos> selectNearest(
            List<BlockPos> candidates,
            double actorX,
            double actorY,
            double actorZ,
            double habitatCenterX,
            double habitatCenterZ,
            int habitatLeashRadiusBlocks
    ) {
        if (candidates == null
                || !Double.isFinite(actorX) || !Double.isFinite(actorY) || !Double.isFinite(actorZ)
                || !Double.isFinite(habitatCenterX) || !Double.isFinite(habitatCenterZ)
                || habitatLeashRadiusBlocks <= 0) {
            throw new IllegalArgumentException("habitat POI selection requires candidates, finite coordinates and positive leash");
        }

        double leashSquared = (double) habitatLeashRadiusBlocks * habitatLeashRadiusBlocks;
        BlockPos nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (BlockPos candidate : candidates) {
            if (candidate == null) continue;
            double candidateX = candidate.getX() + 0.5D;
            double candidateY = candidate.getY() + 0.5D;
            double candidateZ = candidate.getZ() + 0.5D;
            double leashDx = candidateX - habitatCenterX;
            double leashDz = candidateZ - habitatCenterZ;
            if (leashDx * leashDx + leashDz * leashDz > leashSquared) continue;

            double dx = candidateX - actorX;
            double dy = candidateY - actorY;
            double dz = candidateZ - actorZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = candidate.toImmutable();
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static String cacheKey(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection
    ) {
        return world.getRegistryKey().getValue()
                + "|" + projection.populationKey()
                + "|" + projection.habitatCenterX()
                + "|" + projection.habitatCenterZ();
    }

    private record CachedPoint(Optional<BlockPos> position, long refreshAtTick) {
        private CachedPoint {
            if (position == null) position = Optional.empty();
        }
    }
}
