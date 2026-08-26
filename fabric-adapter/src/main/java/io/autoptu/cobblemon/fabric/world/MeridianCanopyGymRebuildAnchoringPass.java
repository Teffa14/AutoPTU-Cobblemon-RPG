package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.world.build.OurosVoxelGeometry;
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
 * physical joinery where decorative geometry would otherwise terminate in air and re-authors edge
 * specimen trees so their crowns lean into the site instead of escaping the exact review envelope.
 */
public final class MeridianCanopyGymRebuildAnchoringPass {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState FRAME_X = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState FRAME_Y = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Y);
    private static final BlockState FRAME_Z = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);
    private static final BlockState WOOD_X = Blocks.DARK_OAK_WOOD.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState WOOD_Y = Blocks.DARK_OAK_WOOD.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Y);
    private static final BlockState WOOD_Z = Blocks.DARK_OAK_WOOD.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);
    private static final BlockState TREE_LOG_Y = Blocks.DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Y);

    private MeridianCanopyGymRebuildAnchoringPass() {}

    public static void apply(ServerWorld world, BlockPos o) {
        anchorServiceLights(world, o);
        anchorArenaRailNodes(world, o);
        anchorGateWindowJoinery(world, o);
        anchorCentralTreeCrotch(world, o);
        anchorBotanicalGalleryLights(world, o);
        replantApproachBoundaryTrees(world, o);
        replantArenaBoundaryTrees(world, o);
    }

    private static void anchorServiceLights(ServerWorld world, BlockPos o) {
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
            for (int y = 1; y <= 6; y++) {
                world.setBlockState(c.add(p[0], y, p[1]),
                        y <= 4 ? Blocks.POLISHED_TUFF_WALL.getDefaultState() : Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
    }

    private static void anchorGateWindowJoinery(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-11, -8, 8, 11}) {
            for (int y = 1; y <= 4; y++) {
                world.setBlockState(o.add(x, y, -25), FRAME_Y);
            }
            world.setBlockState(o.add(x, 5, -25), Blocks.DARK_OAK_SLAB.getDefaultState());
        }
    }

    private static void anchorCentralTreeCrotch(ServerWorld world, BlockPos o) {
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
        world.setBlockState(o.add(-2, 12, -1), FRAME_X);
        world.setBlockState(o.add(-3, 12, -1), FRAME_X);
        world.setBlockState(o.add(-3, 13, -2), FRAME_Y);
        world.setBlockState(o.add(2, 12, -2), FRAME_X);
        world.setBlockState(o.add(3, 12, -2), FRAME_X);
        world.setBlockState(o.add(3, 13, -3), FRAME_Y);
    }

    private static void anchorBotanicalGalleryLights(ServerWorld world, BlockPos o) {
        for (int z : new int[]{-7, 0, 7}) {
            for (int y = 2; y <= 10; y++) {
                world.setBlockState(o.add(-21, y, z), FRAME_Y);
            }
            beamX(world, o, -21, -19, 10, z, FRAME_X);
            world.setBlockState(o.add(-19, 9, z), Blocks.CHAIN.getDefaultState());
        }
    }

    private static void replantApproachBoundaryTrees(ServerWorld world, BlockPos o) {
        // The two front specimens were rooted at z=-29. Their south-facing branch lobes reached
        // z=-34 by a single leaf block. Replant one block north so the full crown remains visible
        // while preserving the paired approach composition and all nearby architecture.
        clearOldApproachTree(world, o, -1);
        clearOldApproachTree(world, o, 1);
        buildContainedSpecimenTree(world, o.add(-23, 1, -28), 8, -1);
        buildContainedSpecimenTree(world, o.add(23, 1, -28), 9, 1);
    }

    private static void clearOldApproachTree(ServerWorld world, BlockPos o, int side) {
        int minX = side < 0 ? -32 : 14;
        int maxX = side < 0 ? -14 : 32;
        for (int x = minX; x <= maxX; x++) {
            for (int y = 1; y <= 15; y++) {
                for (int z = -36; z <= -22; z++) {
                    clearTreeBlock(world, o.add(x, y, z));
                }
            }
        }
    }

    private static void replantArenaBoundaryTrees(ServerWorld world, BlockPos o) {
        // The original arena-side trees were rooted at x=+-29. Their symmetric crowns extended to
        // x=+-37. Regrow them two blocks inward with the lean reversed toward the building.
        clearOldArenaTree(world, o, -29);
        clearOldArenaTree(world, o, 29);
        buildContainedSpecimenTree(world, o.add(-27, 1, 9), 9, 1);
        buildContainedSpecimenTree(world, o.add(27, 1, 9), 8, -1);
    }

    private static void clearOldArenaTree(ServerWorld world, BlockPos o, int rootX) {
        int minX = rootX < 0 ? -40 : 20;
        int maxX = rootX < 0 ? -20 : 40;
        for (int x = minX; x <= maxX; x++) {
            for (int y = 1; y <= 15; y++) {
                for (int z = 2; z <= 16; z++) {
                    clearTreeBlock(world, o.add(x, y, z));
                }
            }
        }
    }

    private static void clearTreeBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.DARK_OAK_LOG)
                || state.isOf(Blocks.DARK_OAK_WOOD)
                || state.isOf(Blocks.MANGROVE_ROOTS)
                || state.isOf(Blocks.AZALEA_LEAVES)
                || state.isOf(Blocks.FLOWERING_AZALEA_LEAVES)) {
            world.setBlockState(pos, AIR);
        }
    }

    private static void buildContainedSpecimenTree(ServerWorld world, BlockPos root, int height, int lean) {
        world.setBlockState(root, Blocks.MANGROVE_ROOTS.getDefaultState());
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            world.setBlockState(root.add(d[0], 0, d[1]), Blocks.MANGROVE_ROOTS.getDefaultState());
        }

        int trunkX = 0;
        for (int y = 0; y < height; y++) {
            if (y == height / 2) {
                trunkX += lean;
            }
            world.setBlockState(root.add(trunkX, y, 0), TREE_LOG_Y);
            if (y < 3) {
                world.setBlockState(root.add(trunkX + (lean < 0 ? -1 : 1), y, 0), TREE_LOG_Y);
            }
        }

        int branchY = height - 3;
        int[][] ends = {
                {trunkX - 4, branchY + 2, -2}, {trunkX + 4, branchY + 1, -2},
                {trunkX - 3, branchY + 3, 3}, {trunkX + 3, branchY + 2, 3}
        };
        for (int[] end : ends) {
            placeSupportedBranch(world, root, trunkX, branchY, end[0], end[1], end[2]);
            canopyMass(world, root.add(end[0], end[1] + 1, end[2]), 3, 2, 3);
        }
        canopyMass(world, root.add(trunkX, height + 2, 0), 4, 2, 3);
    }

    private static void placeSupportedBranch(
            ServerWorld world,
            BlockPos root,
            int startX,
            int startY,
            int endX,
            int endY,
            int endZ
    ) {
        int xStep = Integer.compare(endX, startX);
        for (int x = startX; x != endX + xStep; x += xStep) {
            world.setBlockState(root.add(x, startY, 0), WOOD_X);
        }

        int zStep = Integer.compare(endZ, 0);
        if (zStep != 0) {
            for (int z = 0; z != endZ + zStep; z += zStep) {
                world.setBlockState(root.add(endX, startY, z), WOOD_Z);
            }
        }

        for (int y = startY; y <= endY; y++) {
            world.setBlockState(root.add(endX, y, endZ), WOOD_Y);
        }
    }

    private static void canopyMass(ServerWorld world, BlockPos center, int rx, int ry, int rz) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipsoid(rx, ry, rz)) {
            BlockPos pos = center.add(voxel.x(), voxel.y(), voxel.z());
            BlockState existing = world.getBlockState(pos);
            if (!existing.isAir()
                    && !existing.isOf(Blocks.AZALEA_LEAVES)
                    && !existing.isOf(Blocks.FLOWERING_AZALEA_LEAVES)) {
                continue;
            }
            int pattern = Math.floorMod(voxel.x() * 31 + voxel.y() * 17 + voxel.z() * 13, 9);
            world.setBlockState(pos, pattern == 0 || pattern == 5
                    ? Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState()
                    : Blocks.AZALEA_LEAVES.getDefaultState());
        }
    }

    private static void beamX(ServerWorld world, BlockPos o, int minX, int maxX, int y, int z, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }
}
