package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWorldEventObjectService;
import io.autoptu.cobblemon.authority.FileCanonicalWorldEventObjectRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof that durable canonical world-event state projects idempotently into Minecraft. */
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
        BlockPos shrineAnchor = world.getSpawnPos().up(36);
        BlockPos switchAnchor = shrineAnchor.east(2);
        BlockState originalShrine = world.getBlockState(shrineAnchor);
        BlockState originalSwitch = world.getBlockState(switchAnchor);
        var activatedShrine = new FileCanonicalWorldEventObjectRepository.State(
                "minecraft:overworld:smoke-shrine",
                CanonicalWorldEventObjectService.SHRINE_EVENT_KEY,
                FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED,
                1L
        );
        var activatedSwitch = new FileCanonicalWorldEventObjectRepository.State(
                "minecraft:overworld:smoke-switch",
                CanonicalWorldEventObjectService.SWITCH_EVENT_KEY,
                FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED,
                1L
        );
        try {
            world.setBlockState(
                    shrineAnchor,
                    Blocks.RESPAWN_ANCHOR.getDefaultState().with(RespawnAnchorBlock.CHARGES, 0),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, shrineAnchor, activatedShrine);
            if (world.getBlockState(shrineAnchor).get(RespawnAnchorBlock.CHARGES) != 4) {
                throw new IllegalStateException("activated canonical shrine did not project to charged Minecraft state");
            }

            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, shrineAnchor, activatedShrine);
            if (world.getBlockState(shrineAnchor).get(RespawnAnchorBlock.CHARGES) != 4) {
                throw new IllegalStateException("replayed canonical shrine projection was not idempotent");
            }

            world.setBlockState(shrineAnchor, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            FabricCanonicalWorldInteractionRuntime.projectShrineState(world, shrineAnchor, activatedShrine);
            if (!world.getBlockState(shrineAnchor).isOf(Blocks.STONE)) {
                throw new IllegalStateException("world-event projection mutated a non-shrine block");
            }

            world.setBlockState(
                    switchAnchor,
                    Blocks.LEVER.getDefaultState().with(LeverBlock.POWERED, false),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectSwitchState(world, switchAnchor, activatedSwitch);
            if (!world.getBlockState(switchAnchor).get(LeverBlock.POWERED)) {
                throw new IllegalStateException("activated canonical switch did not project to powered Minecraft state");
            }

            FabricCanonicalWorldInteractionRuntime.projectSwitchState(world, switchAnchor, activatedSwitch);
            if (!world.getBlockState(switchAnchor).get(LeverBlock.POWERED)) {
                throw new IllegalStateException("replayed canonical switch projection was not idempotent");
            }

            world.setBlockState(switchAnchor, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            FabricCanonicalWorldInteractionRuntime.projectSwitchState(world, switchAnchor, activatedSwitch);
            if (!world.getBlockState(switchAnchor).isOf(Blocks.STONE)) {
                throw new IllegalStateException("world-event projection mutated a non-switch block");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(shrineAnchor, originalShrine, Block.NOTIFY_ALL);
            world.setBlockState(switchAnchor, originalSwitch, Block.NOTIFY_ALL);
        }
    }
}
