package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Structural QA for the zero-based Meridian rebuild.
 *
 * The base rebuild intentionally replaces the legacy structure wholesale. This second step only
 * corrects support/readability-sensitive details that are easy to judge incorrectly in code: roof
 * pitch inside the fixed viewer envelope, beam-to-chain continuity and arena roof ties. It never
 * adds unrelated decorative mass or PTU mechanics.
 */
public final class MeridianCanopyGymRebuildStructuralPass {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState FRAME_X = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.X);
    private static final BlockState FRAME_Z = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Z);
    private static final BlockState FRAME_Y = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()
            .with(Properties.AXIS, Direction.Axis.Y);
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private MeridianCanopyGymRebuildStructuralPass() {}

    public static void apply(ServerWorld world, BlockPos o) {
        rebuildGatehouseRoofWithinEnvelope(world, o);
        rebuildApproachLampArms(world, o);
        supportGateLanterns(world, o);
        supportConservatoryLanterns(world, o);
        alignArenaRoofTies(world, o);
        supportLeaderDaisLanterns(world, o);
        replaceFragileHydroControls(world, o);
        consolidateServiceProps(world, o);
    }

    private static void rebuildGatehouseRoofWithinEnvelope(ServerWorld world, BlockPos o) {
        // Remove only the first-pass roof skin/ridge. Keep every pier, lintel and wall below y=9.
        for (int x = -16; x <= 16; x++) {
            for (int y = 9; y <= 22; y++) {
                for (int z = -26; z <= -16; z++) {
                    BlockPos pos = o.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.DEEPSLATE_TILE_STAIRS)
                            || state.isOf(Blocks.DEEPSLATE_TILE_SLAB)
                            || (state.isOf(Blocks.STRIPPED_DARK_OAK_LOG) && y >= 14)) {
                        world.setBlockState(pos, AIR);
                    }
                }
            }
        }

        BlockState west = Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.EAST);
        BlockState east = Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.WEST);

        // Continuous 2:1 stepped pitch. Every x-column receives roof skin; no isolated stair strips.
        for (int x = -15; x <= 15; x++) {
            int distanceFromRidge = Math.abs(x);
            int y = 9 + (15 - distanceFromRidge) / 2;
            BlockState roof = x < 0 ? west : x > 0 ? east : Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
            for (int z = -26; z <= -16; z++) {
                world.setBlockState(o.add(x, y, z), roof);
            }
        }
        for (int z = -26; z <= -16; z++) {
            world.setBlockState(o.add(0, 17, z), FRAME_Z);
        }

        // Three real roof trusses connect eaves to ridge and explain the span from below.
        for (int z : new int[]{-24, -21, -18}) {
            beamX(world, o, -14, 14, 9, z, FRAME_X);
            column(world, o, 0, 10, z, 7, FRAME_Y);
            for (int x : new int[]{-10, -5, 5, 10}) {
                int topY = 9 + (15 - Math.abs(x)) / 2 - 1;
                column(world, o, x, 9, z, Math.max(1, topY - 8), Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
    }

    private static void rebuildApproachLampArms(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-8, 8}) {
            int inward = x < 0 ? 1 : -1;
            column(world, o, x, 1, -31, 5, FRAME_Y);
            world.setBlockState(o.add(x + inward, 5, -31), FRAME_X);
            world.setBlockState(o.add(x + inward * 2, 5, -31), FRAME_X);
            world.setBlockState(o.add(x + inward * 2, 4, -31), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x + inward * 2, 3, -31), LANTERN);
        }
    }

    private static void supportGateLanterns(ServerWorld world, BlockPos o) {
        beamX(world, o, -5, 5, 9, -20, FRAME_X);
        for (int x : new int[]{-3, 3}) {
            world.setBlockState(o.add(x, 8, -20), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x, 7, -20), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x, 6, -20), LANTERN);
        }
    }

    private static void supportConservatoryLanterns(ServerWorld world, BlockPos o) {
        for (int z : new int[]{-8, 4}) {
            beamX(world, o, -14, 14, 13, z, FRAME_X);
            for (int x : new int[]{-8, 8}) {
                for (int y = 12; y >= 10; y--) {
                    world.setBlockState(o.add(x, y, z), Blocks.CHAIN.getDefaultState());
                }
                world.setBlockState(o.add(x, 9, z), LANTERN);
            }
        }
    }

    private static void alignArenaRoofTies(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);

        // Remove the two off-grid ties from the first pass. They did not terminate on piers.
        for (int x : new int[]{-9, 9}) {
            for (int z = -8; z <= 8; z++) {
                BlockPos pos = c.add(x, 12, z);
                if (world.getBlockState(pos).isOf(Blocks.STRIPPED_DARK_OAK_LOG)) {
                    world.setBlockState(pos, AIR);
                }
            }
        }

        // Every tie now terminates directly on paired perimeter piers.
        for (int x : new int[]{-18, 18}) {
            beamZ(world, c, -8, 8, 12, x, FRAME_Z);
        }
        for (int x : new int[]{-10, 0, 10}) {
            beamZ(world, c, -10, 10, 12, x, FRAME_Z);
        }

        // Supported hanging lamps mark the spectator ring without floating chains.
        for (int[] p : new int[][]{{-18, 0}, {18, 0}, {-10, -7}, {10, -7}}) {
            for (int y = 11; y >= 9; y--) {
                world.setBlockState(c.add(p[0], y, p[1]), Blocks.CHAIN.getDefaultState());
            }
            world.setBlockState(c.add(p[0], 8, p[1]), LANTERN);
        }
    }

    private static void supportLeaderDaisLanterns(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);
        for (int x : new int[]{-4, 4}) {
            for (int y = 10; y >= 6; y--) {
                world.setBlockState(c.add(x, y, 9), Blocks.CHAIN.getDefaultState());
            }
            world.setBlockState(c.add(x, 5, 9), LANTERN);
        }
    }

    private static void replaceFragileHydroControls(ServerWorld world, BlockPos o) {
        for (int z : new int[]{-9, -3, 3, 9}) {
            BlockPos control = o.add(24, 4, z);
            if (world.getBlockState(control).isOf(Blocks.LEVER) || world.getBlockState(control).isAir()) {
                world.setBlockState(control, Blocks.COPPER_BULB.getDefaultState());
            }
        }
    }

    private static void consolidateServiceProps(ServerWorld world, BlockPos o) {
        // Move institution-use clues onto the actual service floor, away from the arena edge.
        for (int z : new int[]{17, 20, 23, 26}) {
            BlockPos legacy = o.add(-24, 3, z);
            if (!world.getBlockState(legacy).isAir()) {
                world.setBlockState(legacy, AIR);
            }
        }
        world.setBlockState(o.add(-28, 3, 18), Blocks.BELL.getDefaultState());
        world.setBlockState(o.add(-28, 3, 21), Blocks.SMITHING_TABLE.getDefaultState());
        world.setBlockState(o.add(-28, 3, 24), Blocks.LOOM.getDefaultState());
        world.setBlockState(o.add(-28, 3, 27), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
    }

    private static void column(
            ServerWorld world,
            BlockPos o,
            int x,
            int startY,
            int z,
            int height,
            BlockState state
    ) {
        for (int y = startY; y < startY + height; y++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }

    private static void beamX(ServerWorld world, BlockPos o, int minX, int maxX, int y, int z, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }

    private static void beamZ(ServerWorld world, BlockPos o, int minZ, int maxZ, int y, int x, BlockState state) {
        for (int z = minZ; z <= maxZ; z++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }
}
