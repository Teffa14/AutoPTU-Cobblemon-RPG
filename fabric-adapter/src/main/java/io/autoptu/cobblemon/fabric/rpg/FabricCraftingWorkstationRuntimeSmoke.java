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

/** Dedicated-server proof for the authored physical crafting workstation identity. */
public final class FabricCraftingWorkstationRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveCraftingWorkstationSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical crafting workstation smoke passed";
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricCraftingWorkstationRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCraftingWorkstationRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos station = world.getSpawnPos().up(34);
        BlockPos middle = station.down();
        BlockPos base = station.down(2);
        BlockState originalStation = world.getBlockState(station);
        BlockState originalMiddle = world.getBlockState(middle);
        BlockState originalBase = world.getBlockState(base);
        try {
            world.setBlockState(station, FabricRpgContent.CRAFTING_WORKSTATION.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricCraftingWorkstationRuntime.isCraftingWorkstation(world, station)) {
                throw new IllegalStateException("namespaced crafting workstation was not recognized");
            }

            world.setBlockState(base, Blocks.BARREL.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(middle, Blocks.CRAFTING_TABLE.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(station, Blocks.SMITHING_TABLE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricCraftingWorkstationRuntime.isCraftingWorkstation(world, station)) {
                throw new IllegalStateException("legacy smithing-over-crafting-over-barrel composite was still accepted");
            }

            world.setBlockState(station, Blocks.SMITHING_TABLE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricCraftingWorkstationRuntime.isCraftingWorkstation(world, station)) {
                throw new IllegalStateException("vanilla smithing table was accepted as canonical crafting workstation");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(base, originalBase, Block.NOTIFY_ALL);
            world.setBlockState(middle, originalMiddle, Block.NOTIFY_ALL);
            world.setBlockState(station, originalStation, Block.NOTIFY_ALL);
        }
    }
}
