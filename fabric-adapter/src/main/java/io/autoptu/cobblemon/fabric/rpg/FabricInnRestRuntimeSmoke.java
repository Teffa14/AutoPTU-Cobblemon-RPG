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

/** Dedicated-server proof that only authored vanilla beds opt into the Ouros inn/rest surface. */
public final class FabricInnRestRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveInnRestSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Minecraft inn rest surface smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricInnRestRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricInnRestRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos marker = world.getSpawnPos().up(30);
        BlockPos bed = marker.up();
        BlockState oldMarker = world.getBlockState(marker);
        BlockState oldBed = world.getBlockState(bed);
        try {
            world.setBlockState(marker, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(bed, Blocks.RED_BED.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricInnRestRuntime.isInnRestPoint(world, bed)) {
                throw new IllegalStateException("gold-marked vanilla bed was not recognized as inn rest point");
            }

            world.setBlockState(marker, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricInnRestRuntime.isInnRestPoint(world, bed)) {
                throw new IllegalStateException("ordinary vanilla bed was incorrectly captured as inn rest point");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(bed, oldBed, Block.NOTIFY_ALL);
            world.setBlockState(marker, oldMarker, Block.NOTIFY_ALL);
        }
    }
}
