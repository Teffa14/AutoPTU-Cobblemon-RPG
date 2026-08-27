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

/** Live dedicated-server proof that the authored healing-station signature is recognized exactly. */
public final class FabricHealingStationRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveHealingStationSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live healing station interaction signature smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricHealingStationRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricHealingStationRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos head = world.getSpawnPos().up(24);
        BlockPos base = head.down();
        BlockPos power = head.down(2);

        BlockState oldHead = world.getBlockState(head);
        BlockState oldBase = world.getBlockState(base);
        BlockState oldPower = world.getBlockState(power);
        try {
            world.setBlockState(power, Blocks.SEA_LANTERN.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(base, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(head, Blocks.LODESTONE.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricHealingStationRuntime.isHealingStation(world, head)) {
                throw new IllegalStateException("healing station signature was not recognized in live ServerWorld");
            }

            world.setBlockState(power, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricHealingStationRuntime.isHealingStation(world, head)) {
                throw new IllegalStateException("incomplete healing station signature was accepted");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(head, oldHead, Block.NOTIFY_ALL);
            world.setBlockState(base, oldBase, Block.NOTIFY_ALL);
            world.setBlockState(power, oldPower, Block.NOTIFY_ALL);
        }
    }
}
