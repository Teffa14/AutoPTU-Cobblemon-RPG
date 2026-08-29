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

/** Dedicated-server proof that only the namespaced PTU recovery bed activates recovery. */
public final class FabricInnRestRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveInnRestSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live namespaced PTU recovery bed smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricInnRestRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricInnRestRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(30);
        BlockState oldState = world.getBlockState(pos);
        try {
            world.setBlockState(pos, FabricRpgContent.PTU_RECOVERY_BED.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricInnRestRuntime.isInnRestPoint(world, pos)) {
                throw new IllegalStateException("namespaced PTU recovery bed was not recognized");
            }

            world.setBlockState(pos, Blocks.RED_BED.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricInnRestRuntime.isInnRestPoint(world, pos)) {
                throw new IllegalStateException("ordinary vanilla bed was incorrectly captured as PTU recovery bed");
            }

            world.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricInnRestRuntime.isInnRestPoint(world, pos)) {
                throw new IllegalStateException("legacy marker block was incorrectly captured as PTU recovery bed");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(pos, oldState, Block.NOTIFY_ALL);
        }
    }
}
