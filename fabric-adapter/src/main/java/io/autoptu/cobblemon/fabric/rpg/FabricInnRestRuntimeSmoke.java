package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
        BlockPos foot = marker.up();
        BlockPos head = foot.east();
        BlockState oldMarker = world.getBlockState(marker);
        BlockState oldFoot = world.getBlockState(foot);
        BlockState oldHead = world.getBlockState(head);
        try {
            BlockState footState = Blocks.RED_BED.getDefaultState()
                    .with(BedBlock.FACING, Direction.EAST)
                    .with(BedBlock.PART, BedPart.FOOT);
            BlockState headState = Blocks.RED_BED.getDefaultState()
                    .with(BedBlock.FACING, Direction.EAST)
                    .with(BedBlock.PART, BedPart.HEAD);

            world.setBlockState(marker, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(foot, footState, Block.NOTIFY_ALL);
            world.setBlockState(head, headState, Block.NOTIFY_ALL);
            if (!FabricInnRestRuntime.isInnRestPoint(world, foot)) {
                throw new IllegalStateException("gold-marked vanilla bed foot was not recognized as inn rest point");
            }
            if (!FabricInnRestRuntime.isInnRestPoint(world, head)) {
                throw new IllegalStateException("gold-marked vanilla bed head was not recognized through its paired foot");
            }

            world.setBlockState(marker, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricInnRestRuntime.isInnRestPoint(world, foot)
                    || FabricInnRestRuntime.isInnRestPoint(world, head)) {
                throw new IllegalStateException("ordinary vanilla bed was incorrectly captured as inn rest point");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(head, oldHead, Block.NOTIFY_ALL);
            world.setBlockState(foot, oldFoot, Block.NOTIFY_ALL);
            world.setBlockState(marker, oldMarker, Block.NOTIFY_ALL);
        }
    }
}
