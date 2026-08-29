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

/** Live dedicated-server proof that vanilla lodestones are the fast-travel presentation surface. */
public final class FabricFastTravelRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveFastTravelSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Minecraft lodestone fast travel surface smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricFastTravelRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricFastTravelRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(26);
        BlockState oldState = world.getBlockState(pos);
        try {
            world.setBlockState(pos, Blocks.LODESTONE.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricFastTravelRuntime.isFastTravelPoint(world, pos)) {
                throw new IllegalStateException("vanilla lodestone was not recognized as fast-travel surface");
            }
            String sourceId = FabricFastTravelRuntime.sourceId(world, pos);
            if (!sourceId.contains("minecraft:overworld")) {
                throw new IllegalStateException("fast-travel source identity did not preserve server dimension");
            }

            world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricFastTravelRuntime.isFastTravelPoint(world, pos)) {
                throw new IllegalStateException("ordinary stone was accepted as fast-travel surface");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(pos, oldState, Block.NOTIFY_ALL);
        }
    }
}
