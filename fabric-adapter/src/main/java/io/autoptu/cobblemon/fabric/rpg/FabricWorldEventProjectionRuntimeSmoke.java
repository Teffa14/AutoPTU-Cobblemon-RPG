package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWorldEventObjectService;
import io.autoptu.cobblemon.authority.FileCanonicalWorldEventObjectRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
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
        BlockPos switchSupport = switchAnchor.down();
        BlockPos switchLamp = switchSupport.east();
        BlockPos doorAnchor = shrineAnchor.east(4);
        BlockPos trapdoorAnchor = shrineAnchor.east(6);
        BlockPos fenceGateAnchor = shrineAnchor.east(8);
        BlockState originalShrine = world.getBlockState(shrineAnchor);
        BlockState originalSwitch = world.getBlockState(switchAnchor);
        BlockState originalSwitchSupport = world.getBlockState(switchSupport);
        BlockState originalSwitchLamp = world.getBlockState(switchLamp);
        BlockState originalDoor = world.getBlockState(doorAnchor);
        BlockState originalTrapdoor = world.getBlockState(trapdoorAnchor);
        BlockState originalFenceGate = world.getBlockState(fenceGateAnchor);
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
        var activatedDoor = new FileCanonicalWorldEventObjectRepository.State(
                "minecraft:overworld:smoke-door",
                CanonicalWorldEventObjectService.DOOR_EVENT_KEY,
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

            world.setBlockState(switchSupport, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            world.setBlockState(
                    switchLamp,
                    Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, false),
                    Block.NOTIFY_ALL
            );
            world.setBlockState(
                    switchAnchor,
                    Blocks.LEVER.getDefaultState()
                            .with(Properties.BLOCK_FACE, BlockFace.FLOOR)
                            .with(LeverBlock.POWERED, false),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectSwitchState(world, switchAnchor, activatedSwitch);
            if (!world.getBlockState(switchAnchor).get(LeverBlock.POWERED)) {
                throw new IllegalStateException("activated canonical switch did not project to powered Minecraft state");
            }
            if (!world.getBlockState(switchLamp).get(RedstoneLampBlock.LIT)) {
                throw new IllegalStateException("persistent lever projection did not propagate strong power through its support block");
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

            world.setBlockState(
                    doorAnchor,
                    Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.OPEN, false),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, doorAnchor, activatedDoor);
            if (!world.getBlockState(doorAnchor).get(DoorBlock.OPEN)) {
                throw new IllegalStateException("activated canonical door did not project to open Minecraft state");
            }
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, doorAnchor, activatedDoor);
            if (!world.getBlockState(doorAnchor).get(DoorBlock.OPEN)) {
                throw new IllegalStateException("replayed canonical door projection was not idempotent");
            }

            world.setBlockState(
                    trapdoorAnchor,
                    Blocks.OAK_TRAPDOOR.getDefaultState().with(TrapdoorBlock.OPEN, false),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, trapdoorAnchor, activatedDoor);
            if (!world.getBlockState(trapdoorAnchor).get(TrapdoorBlock.OPEN)) {
                throw new IllegalStateException("activated canonical trapdoor did not project to open Minecraft state");
            }
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, trapdoorAnchor, activatedDoor);
            if (!world.getBlockState(trapdoorAnchor).get(TrapdoorBlock.OPEN)) {
                throw new IllegalStateException("replayed canonical trapdoor projection was not idempotent");
            }

            world.setBlockState(
                    fenceGateAnchor,
                    Blocks.OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.OPEN, false),
                    Block.NOTIFY_ALL
            );
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, fenceGateAnchor, activatedDoor);
            if (!world.getBlockState(fenceGateAnchor).get(FenceGateBlock.OPEN)) {
                throw new IllegalStateException("activated canonical fence gate did not project to open Minecraft state");
            }
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, fenceGateAnchor, activatedDoor);
            if (!world.getBlockState(fenceGateAnchor).get(FenceGateBlock.OPEN)) {
                throw new IllegalStateException("replayed canonical fence gate projection was not idempotent");
            }

            world.setBlockState(doorAnchor, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            FabricCanonicalWorldInteractionRuntime.projectDoorState(world, doorAnchor, activatedDoor);
            if (!world.getBlockState(doorAnchor).isOf(Blocks.STONE)) {
                throw new IllegalStateException("world-event projection mutated a non-door block");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(shrineAnchor, originalShrine, Block.NOTIFY_ALL);
            world.setBlockState(switchAnchor, originalSwitch, Block.NOTIFY_ALL);
            world.setBlockState(switchSupport, originalSwitchSupport, Block.NOTIFY_ALL);
            world.setBlockState(switchLamp, originalSwitchLamp, Block.NOTIFY_ALL);
            world.setBlockState(doorAnchor, originalDoor, Block.NOTIFY_ALL);
            world.setBlockState(trapdoorAnchor, originalTrapdoor, Block.NOTIFY_ALL);
            world.setBlockState(fenceGateAnchor, originalFenceGate, Block.NOTIFY_ALL);
        }
    }
}
