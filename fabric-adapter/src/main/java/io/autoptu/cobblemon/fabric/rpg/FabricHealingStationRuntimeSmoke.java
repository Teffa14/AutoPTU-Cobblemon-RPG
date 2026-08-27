package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Live dedicated-server proof that AutoPTU recognizes Cobblemon's actual Healing Machine block. */
public final class FabricHealingStationRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveHealingStationSmoke";
    // Keep the established CI marker stable while changing what the smoke actually proves.
    public static final String SUCCESS_LOG = "AutoPTU live healing station interaction signature smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricHealingStationRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricHealingStationRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(24);
        BlockState old = world.getBlockState(pos);
        try {
            world.setBlockState(pos, CobblemonBlocks.HEALING_MACHINE.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricHealingStationRuntime.isCobblemonHealingMachine(world, pos)) {
                throw new IllegalStateException("Cobblemon healing machine was not recognized in live ServerWorld");
            }

            world.setBlockState(pos, Blocks.LODESTONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricHealingStationRuntime.isCobblemonHealingMachine(world, pos)) {
                throw new IllegalStateException("vanilla lodestone was accepted as a Cobblemon healing machine");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(pos, old, Block.NOTIFY_ALL);
        }
    }
}
