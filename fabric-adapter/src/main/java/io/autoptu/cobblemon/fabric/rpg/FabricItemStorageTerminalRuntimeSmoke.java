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

/** Dedicated-server proof for the authored physical item-storage identity. */
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
        BlockPos terminal = world.getSpawnPos().up(30);
        BlockPos below = terminal.down();
        BlockState originalTerminal = world.getBlockState(terminal);
        BlockState originalBelow = world.getBlockState(below);
        try {
            world.setBlockState(terminal, FabricRpgContent.ITEM_STORAGE_TERMINAL.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, terminal)) {
                throw new IllegalStateException("namespaced item storage terminal was not recognized");
            }

            world.setBlockState(below, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(terminal, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, terminal)) {
                throw new IllegalStateException("legacy iron-over-barrel composite was still accepted");
            }

            world.setBlockState(terminal, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(below, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricItemStorageTerminalRuntime.isItemStorageTerminal(world, terminal)) {
                throw new IllegalStateException("vanilla iron block was accepted as item storage terminal");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(below, originalBelow, Block.NOTIFY_ALL);
            world.setBlockState(terminal, originalTerminal, Block.NOTIFY_ALL);
        }
    }
}
