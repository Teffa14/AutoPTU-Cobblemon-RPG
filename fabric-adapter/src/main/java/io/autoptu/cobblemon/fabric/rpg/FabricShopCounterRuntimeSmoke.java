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

/** Dedicated-server proof that the final physical shop tree accepts only the authored counter signature. */
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
        BlockPos head = world.getSpawnPos().up(28);
        BlockPos base = head.down();
        BlockState originalHead = world.getBlockState(head);
        BlockState originalBase = world.getBlockState(base);
        try {
            world.setBlockState(base, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(head, Blocks.EMERALD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricShopCounterRuntime.isShopCounter(world, head)) {
                throw new IllegalStateException("authored emerald-over-barrel shop counter was not recognized");
            }

            world.setBlockState(base, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricShopCounterRuntime.isShopCounter(world, head)) {
                throw new IllegalStateException("non-authored counter base was accepted as canonical shop counter");
            }

            world.setBlockState(base, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(head, Blocks.DIAMOND_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricShopCounterRuntime.isShopCounter(world, head)) {
                throw new IllegalStateException("non-authored counter head was accepted as canonical shop counter");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(base, originalBase, Block.NOTIFY_ALL);
            world.setBlockState(head, originalHead, Block.NOTIFY_ALL);
        }
    }
}
