package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWorldEventObjectService;
import io.autoptu.cobblemon.authority.FileCanonicalWorldEventObjectRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof that durable canonical shrine state projects idempotently into Minecraft. */
public final class FabricWorldEventProjectionRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveWorldEventProjectionSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live persistent world-event projection smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricWorldEventProjectionRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(FabricWorldEventProjectionRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos anchor = world.getSpawnPos().up(36);
        BlockState original = world.getBlockState(anchor);
        var activated = new FileCanonicalWorldEventObjectRepository.State(
                "minecraft:overworld:smoke-shrine",
                CanonicalWorldEventObjectService.SHRINE_EVENT_KEY,
                FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED,
                1L
        );
        try {
            world.setBlockState(
                    anchor,
                    Blocks.RESPAWN_ANCHOR.getDefaultState().with(RespawnAnchorBlock.CHARGES, 0),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, anchor, activated);
            if (world.getBlockState(anchor).get(RespawnAnchorBlock.CHARGES) != 4) {
                throw new IllegalStateException("activated canonical shrine did not project to charged Minecraft state");
            }

            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, anchor, activated);
            if (world.getBlockState(anchor).get(RespawnAnchorBlock.CHARGES) != 4) {
                throw new IllegalStateException("replayed canonical shrine projection was not idempotent");
            }

            world.setBlockState(anchor, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, anchor, activated);
            if (!world.getBlockState(anchor).isOf(Blocks.STONE)) {
                throw new IllegalStateException("world-event projection mutated a non-shrine block");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(anchor, original, Block.NOTIFY_ALL);
        }
    }
}
