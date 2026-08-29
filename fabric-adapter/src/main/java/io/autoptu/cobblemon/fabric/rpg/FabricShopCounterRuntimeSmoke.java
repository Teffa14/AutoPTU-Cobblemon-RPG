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

/** Dedicated-server proof that the shop runtime accepts only the namespaced authored counter. */
public final class FabricShopCounterRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveShopCounterSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical shop counter smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricShopCounterRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(FabricShopCounterRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos counter = world.getSpawnPos().up(28);
        BlockPos below = counter.down();
        BlockState originalCounter = world.getBlockState(counter);
        BlockState originalBelow = world.getBlockState(below);
        try {
            world.setBlockState(counter, FabricRpgContent.CEDAR_MART_COUNTER.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricShopCounterRuntime.isShopCounter(world, counter)) {
                throw new IllegalStateException("namespaced Cedar Mart counter was not recognized");
            }

            world.setBlockState(counter, Blocks.EMERALD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(below, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricShopCounterRuntime.isShopCounter(world, counter)) {
                throw new IllegalStateException("legacy emerald-over-barrel signature was still accepted as canonical shop identity");
            }

            world.setBlockState(counter, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricShopCounterRuntime.isShopCounter(world, counter)) {
                throw new IllegalStateException("vanilla barrel was accepted as canonical shop counter");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(below, originalBelow, Block.NOTIFY_ALL);
            world.setBlockState(counter, originalCounter, Block.NOTIFY_ALL);
        }
    }
}
