package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof that progression gating uses only the explicit namespaced gate identity. */
public final class FabricBadgeGateRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveBadgeGateSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical badge gate smoke passed";
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricBadgeGateRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricBadgeGateRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos gate = world.getSpawnPos().up(36);
        BlockState original = world.getBlockState(gate);
        try {
            world.setBlockState(gate, FabricRpgContent.CEDAR_LEAGUE_BADGE_GATE.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricBadgeGateRuntime.isBadgeGate(world, gate)) {
                throw new IllegalStateException("namespaced Cedar League badge gate was not recognized");
            }
            world.setBlockState(gate, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricBadgeGateRuntime.isBadgeGate(world, gate)) {
                throw new IllegalStateException("vanilla iron block was accepted as canonical badge gate");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(gate, original, Block.NOTIFY_ALL);
        }
    }
}
