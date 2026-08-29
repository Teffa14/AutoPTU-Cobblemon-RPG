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

/** Live dedicated-server proof that the real Cobblemon Healing Machine is the canonical heal surface. */
public final class FabricHealingStationRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveHealingStationSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cobblemon healing machine reuse smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricHealingStationRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricHealingStationRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(24);
        BlockState oldState = world.getBlockState(pos);
        try {
            world.setBlockState(pos, CobblemonBlocks.HEALING_MACHINE.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricHealingStationRuntime.isHealingStation(world, pos)) {
                throw new IllegalStateException("real Cobblemon Healing Machine was not recognized");
            }

            world.setBlockState(pos, Blocks.LODESTONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricHealingStationRuntime.isHealingStation(world, pos)) {
                throw new IllegalStateException("legacy custom lodestone healer was still accepted");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(pos, oldState, Block.NOTIFY_ALL);
        }
    }
}
