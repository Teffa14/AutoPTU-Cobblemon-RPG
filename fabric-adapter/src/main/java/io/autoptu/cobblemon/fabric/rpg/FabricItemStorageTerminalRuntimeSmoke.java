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

/** Dedicated-server proof for the authored physical item-storage signature. */
public final class FabricItemStorageTerminalRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveItemStorageTerminalSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical item storage terminal smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricItemStorageTerminalRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(FabricItemStorageTerminalRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos head = world.getSpawnPos().up(30);
        BlockPos base = head.down();
        BlockState originalHead = world.getBlockState(head);
        BlockState originalBase = world.getBlockState(base);
        try {
            world.setBlockState(base, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(head, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, head)) {
                throw new IllegalStateException("authored iron-over-barrel item storage terminal was not recognized");
            }

            world.setBlockState(base, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, head)) {
                throw new IllegalStateException("non-authored item storage base was accepted");
            }

            world.setBlockState(base, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(head, Blocks.EMERALD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, head)) {
                throw new IllegalStateException("shop counter head was accepted as item storage terminal");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(base, originalBase, Block.NOTIFY_ALL);
            world.setBlockState(head, originalHead, Block.NOTIFY_ALL);
        }
    }
}
