package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Validation-driven structural anchoring for the zero-base Meridian rebuild.
 *
 * This pass exists because the exact server audit rejects disconnected authored components. It adds
 * physical joinery where decorative geometry would otherwise terminate in air: roof-hung service
 * lights, arena rail posts, gate window mullions and the crotch of the central specimen tree.
 */
public final class MeridianCanopyGymRebuildAnchoringPass {
    private static final BlockState FRAME_X = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState FRAME_Y = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Y);
    private static final BlockState FRAME_Z = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);
    private static final BlockState WOOD_X = Blocks.DARK_OAK_WOOD.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState WOOD_Z = Blocks.DARK_OAK_WOOD.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);

    private MeridianCanopyGymRebuildAnchoringPass() {}

    public static void apply(ServerWorld world, BlockPos o) {
        anchorServiceLights(world, o);
        anchorArenaRailNodes(world, o);
        anchorGateWindowJoinery(world, o);
        anchorCentralTreeCrotch(world, o);
        anchorBotanicalGalleryLights(world, o);
    }

    private static void anchorServiceLights(ServerWorld world, BlockPos o) {
        // The lean-to roof sits at y=10 above x=-28. Extend every hanging chain to that roof skin.
        for (int z : new int[]{17, 23, 29}) {
            world.setBlockState(o.add(-28, 9, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-28, 8, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-28, 7, z), Blocks.CHAIN.getDefaultState());
        }
    }

    private static void anchorArenaRailNodes(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);
        int[][] railNodes = {
                {-21, -5}, {-19, -7}, {-14, -9}, {-6, -10}, {6, -10}, {14, -9}, {19, -7}, {21, -5},
                {-21, 5}, {-19, 7}, {-14, 9}, {-6, 10}, {6, 10}, {14, 9}, {19, 7}, {21, 5}
        };
        for (int[] p : railNodes) {
            // Narrow posts read as spectator-rail stanchions and connect the fence/grate detail to the bowl.
            for (int y = 3; y <= 6; y++) {
                world.setBlockState(c.add(p[0], y, p[1]),
                        y <= 4 ? Blocks.POLISHED_TUFF_WALL.getDefaultState() : Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
    }

    private static void anchorGateWindowJoinery(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-11, -8, 8, 11}) {
            // The mullion connects sill, shutters and head cap as one framed window bay.
            for (int y = 2; y <= 4; y++) {
                world.setBlockState(o.add(x, y, -25), FRAME_Y);
            }
            world.setBlockState(o.add(x, 5, -25), Blocks.DARK_OAK_SLAB.getDefaultState());
        }
    }

    private static void anchorCentralTreeCrotch(ServerWorld world, BlockPos o) {
        // Central specimen trunk is rooted at (0,2,-2). A thick branch crotch prevents 3-axis
        // Bresenham branches from touching the trunk only at a corner before opening into the crown.
        for (int x = -4; x <= 4; x++) {
            world.setBlockState(o.add(x, 11, -2), WOOD_X);
        }
        for (int z = -5; z <= 3; z++) {
            world.setBlockState(o.add(0, 11, z), WOOD_Z);
        }
        for (int x = -2; x <= 2; x++) {
            world.setBlockState(o.add(x, 12, -2), WOOD_X);
        }
        for (int z = -4; z <= 1; z++) {
            world.setBlockState(o.add(0, 12, z), WOOD_Z);
        }
        // Two short diagonal-looking forks are built as adjacent orthogonal steps, never corner-only.
        world.setBlockState(o.add(-2, 12, -1), FRAME_X);
        world.setBlockState(o.add(-3, 12, -1), FRAME_X);
        world.setBlockState(o.add(-3, 13, -2), FRAME_Y);
        world.setBlockState(o.add(2, 12, -2), FRAME_X);
        world.setBlockState(o.add(3, 12, -2), FRAME_X);
        world.setBlockState(o.add(3, 13, -3), FRAME_Y);
    }

    private static void anchorBotanicalGalleryLights(ServerWorld world, BlockPos o) {
        // The three gallery lamps now hang from short beams tied back into terrace frames/roofs.
        for (int z : new int[]{-7, 0, 7}) {
            beamX(world, o, -21, -19, 10, z, FRAME_X);
            world.setBlockState(o.add(-19, 9, z), Blocks.CHAIN.getDefaultState());
        }
    }

    private static void beamX(ServerWorld world, BlockPos o, int minX, int maxX, int y, int z, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }
}
