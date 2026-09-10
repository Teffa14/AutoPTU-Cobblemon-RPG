package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonBlockEntities;
import com.cobblemon.mod.common.CobblemonBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server probe for the Cobblemon 1.8 Habitat Block presentation bridge.
 *
 * <p>The probe places one default Habitat Block only for the duration of this synchronous method,
 * verifies its real 1.8 block-entity type and AutoPTU POI discovery, then restores the exact
 * previous block state before a server tick can let Cobblemon's native spawner execute. It never
 * reads or writes habitat spawn configuration.</p>
 */
public final class CobblemonHabitatPointOfInterestRuntimeSmoke implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String ENABLE_PROPERTY = "autoptu.liveMareaWildPresenceSmoke";
    private static final int VERTICAL_PROBE_RADIUS_BLOCKS = 16;
    private static final int MAX_HORIZONTAL_PROBE_RADIUS_BLOCKS = 24;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonHabitatPointOfInterestRuntimeSmoke::verify);
    }

    static void verify(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is required");
        ServerWorld world = server.getOverworld();
        var projections = WildEcologyProjectionRegistry.collect(world);
        if (projections.isEmpty()) {
            throw new IllegalStateException("Cobblemon 1.8 habitat POI smoke requires one projected canonical wild actor");
        }

        var projection = projections.getFirst();
        BlockPos probePos = findProbePosition(world, projection);
        BlockState original = world.getBlockState(probePos);
        if (world.getBlockEntity(probePos) != null) {
            throw new IllegalStateException("Cobblemon 1.8 habitat POI smoke must not replace a block entity");
        }

        CobblemonHabitatPointOfInterest.clear(server);
        try {
            if (!world.setBlockState(probePos, CobblemonBlocks.HABITAT_BLOCK.getDefaultState(), 3)) {
                throw new IllegalStateException("Cobblemon 1.8 Habitat Block could not be placed for POI smoke");
            }
            if (!world.getBlockState(probePos).isOf(CobblemonBlocks.HABITAT_BLOCK)) {
                throw new IllegalStateException("Cobblemon 1.8 Habitat Block state was not observable after placement");
            }
            var blockEntity = world.getBlockEntity(probePos);
            if (blockEntity == null || blockEntity.getType() != CobblemonBlockEntities.HABITAT_BLOCK) {
                throw new IllegalStateException("Cobblemon 1.8 Habitat Block did not expose its expected block entity");
            }

            BlockPos located = CobblemonHabitatPointOfInterest.nearest(world, projection)
                    .orElseThrow(() -> new IllegalStateException(
                            "AutoPTU did not discover the physical Cobblemon 1.8 Habitat Block inside the canonical leash"));
            if (!located.equals(probePos)) {
                throw new IllegalStateException("AutoPTU selected an unexpected Habitat Block POI: " + located);
            }

            LOGGER.info("AutoPTU live Cobblemon 1.8 Habitat Block POI smoke passed: population={} block={}",
                    projection.populationKey(), probePos);
        } finally {
            world.setBlockState(probePos, original, 3);
            CobblemonHabitatPointOfInterest.clear(server);
        }
    }

    private static BlockPos findProbePosition(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection
    ) {
        BlockPos actorAnchor = projection.actor().getBlockPos();
        BlockPos nearActor = findLoadedProbePosition(
                world, projection, actorAnchor.getX(), actorAnchor.getY(), actorAnchor.getZ(), true);
        if (nearActor != null) return nearActor;

        int centerX = (int) Math.floor(projection.habitatCenterX());
        int centerZ = (int) Math.floor(projection.habitatCenterZ());
        BlockPos nearCenter = findLoadedProbePosition(
                world, projection, centerX, actorAnchor.getY(), centerZ, true);
        if (nearCenter != null) return nearCenter;

        nearActor = findLoadedProbePosition(
                world, projection, actorAnchor.getX(), actorAnchor.getY(), actorAnchor.getZ(), false);
        if (nearActor != null) return nearActor;
        nearCenter = findLoadedProbePosition(
                world, projection, centerX, actorAnchor.getY(), centerZ, false);
        if (nearCenter != null) return nearCenter;

        throw new IllegalStateException("Cobblemon 1.8 habitat POI smoke found no safe loaded probe position inside canonical leash");
    }

    private static BlockPos findLoadedProbePosition(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection,
            int anchorX,
            int baseY,
            int anchorZ,
            boolean airOnly
    ) {
        int horizontalRadius = Math.min(
                projection.habitatLeashRadiusBlocks(),
                MAX_HORIZONTAL_PROBE_RADIUS_BLOCKS);
        for (int dy = -VERTICAL_PROBE_RADIUS_BLOCKS; dy <= VERTICAL_PROBE_RADIUS_BLOCKS; dy++) {
            for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    BlockPos candidate = new BlockPos(anchorX + dx, baseY + dy, anchorZ + dz);
                    if (!world.isChunkLoaded(candidate)) continue;
                    if (!WildAmbientBehaviorRuntime.insideHorizontalLeash(
                            candidate.getX() + 0.5D,
                            candidate.getZ() + 0.5D,
                            projection.habitatCenterX(),
                            projection.habitatCenterZ(),
                            projection.habitatLeashRadiusBlocks())) continue;
                    BlockState state = world.getBlockState(candidate);
                    if (airOnly && !state.isAir()) continue;
                    if (world.getBlockEntity(candidate) != null) continue;
                    if (state.isOf(CobblemonBlocks.HABITAT_BLOCK)) continue;
                    return candidate;
                }
            }
        }
        return null;
    }
}
