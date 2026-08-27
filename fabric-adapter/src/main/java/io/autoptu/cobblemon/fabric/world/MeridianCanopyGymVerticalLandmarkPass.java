package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.world.build.OurosVoxelGeometry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Vertical landmark pass for the zero-base Meridian Canopy Gym.
 *
 * This pass deliberately spends the additional height budget on structural hierarchy rather than
 * stacked decorative mass. The conservatory grows into a tall ventilated lantern, the battle bowl
 * gains supported high ribs, and the gatehouse receives a smaller arrival beacon. Every authored
 * element has an orthogonally connected load path into geometry that already reaches the ground so
 * the exact floating-component audit remains meaningful.
 */
public final class MeridianCanopyGymVerticalLandmarkPass {
    private static final BlockState TIMBER = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState OAK = Blocks.STRIPPED_OAK_LOG.getDefaultState();
    private static final BlockState COPPER = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState WEATHERED_COPPER = Blocks.WEATHERED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_GRATE = Blocks.OXIDIZED_COPPER_GRATE.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState DEEPSLATE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private MeridianCanopyGymVerticalLandmarkPass() {}

    public static void apply(ServerWorld world, BlockPos origin) {
        clearHighAirspace(world, origin);
        buildConservatoryLantern(world, origin);
        buildArenaHighCanopy(world, origin);
        buildGatehouseBeacon(world, origin);
    }

    private static void clearHighAirspace(ServerWorld world, BlockPos o) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -33; x <= 33; x++) {
            for (int y = 23; y <= 46; y++) {
                for (int z = -33; z <= 33; z++) {
                    world.setBlockState(o.add(x, y, z), air);
                }
            }
        }
    }

    private static void buildConservatoryLantern(ServerWorld world, BlockPos o) {
        int[][] lowerPylons = {
                {-14, -16}, {14, -16}, {-14, 9}, {14, 9}
        };
        for (int[] p : lowerPylons) {
            column(world, o, p[0], 13, p[1], 12, TIMBER);
            for (int y = 16; y <= 24; y += 4) {
                world.setBlockState(o.add(p[0], y, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
            }
        }

        beamX(world, o, -14, 14, 24, -16, TIMBER);
        beamX(world, o, -14, 14, 24, 9, TIMBER);
        beamZ(world, o, -16, 9, 24, -14, TIMBER);
        beamZ(world, o, -16, 9, 24, 14, TIMBER);

        connectedBrace(world, o, new BlockPos(-14, 24, -16), new BlockPos(-6, 29, -9), OAK);
        connectedBrace(world, o, new BlockPos(14, 24, -16), new BlockPos(6, 29, -9), OAK);
        connectedBrace(world, o, new BlockPos(-14, 24, 9), new BlockPos(-6, 29, 3), OAK);
        connectedBrace(world, o, new BlockPos(14, 24, 9), new BlockPos(6, 29, 3), OAK);

        int[][] lanternPosts = {
                {-6, -6}, {-4, -9}, {4, -9}, {6, -6},
                {6, 0}, {4, 3}, {-4, 3}, {-6, 0}
        };
        for (int[] p : lanternPosts) {
            column(world, o, p[0], 29, p[1], 7, TIMBER);
        }
        connectLoop(world, o, lanternPosts, 29, TIMBER);
        connectLoop(world, o, lanternPosts, 35, TIMBER);
        beamX(world, o, -6, 6, 29, -3, TIMBER);
        beamZ(world, o, -9, 3, 29, 0, TIMBER);

        for (int y = 30; y <= 34; y++) {
            BlockState skin = y == 32 ? COPPER_GRATE : GLASS;
            for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(7, 6, 6, 5, y)) {
                world.setBlockState(o.add(voxel.x(), voxel.y(), voxel.z() - 3), skin);
            }
        }

        placeEllipseRing(world, o.add(0, 0, -3), 9, 7, 7, 5, 35, DEEPSLATE_SLAB);
        placeEllipseRing(world, o.add(0, 0, -3), 8, 6, 6, 4, 36, COPPER);
        placeEllipseRing(world, o.add(0, 0, -3), 7, 5, 5, 3, 37, DEEPSLATE);
        placeEllipseRing(world, o.add(0, 0, -3), 6, 4, 4, 2, 38, WEATHERED_COPPER);
        placeFilledEllipse(world, o.add(0, 0, -3), 4, 3, 39, COPPER);
        placeEllipseRing(world, o.add(0, 0, -3), 3, 2, 1, 1, 40, DEEPSLATE_SLAB);
        column(world, o, 0, 40, -3, 5, OAK);
        world.setBlockState(o.add(0, 45, -3), Blocks.LIGHTNING_ROD.getDefaultState());

        for (int[] p : new int[][]{{-3, -3}, {3, -3}, {0, 0}}) {
            for (int y = 29; y >= 25; y--) {
                world.setBlockState(o.add(p[0], y, p[1]), Blocks.CHAIN.getDefaultState());
            }
            world.setBlockState(o.add(p[0], 24, p[1]), LANTERN);
        }
    }

    private static void buildArenaHighCanopy(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);
        int[][] pylons = {
                {-22, -5}, {0, -10}, {22, -5},
                {-22, 5}, {0, 10}, {22, 5}
        };
        for (int[] p : pylons) {
            column(world, c, p[0], 13, p[1], 12, TIMBER);
            world.setBlockState(c.add(p[0], 18, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
            world.setBlockState(c.add(p[0], 24, p[1]), COPPER);
        }

        placeEllipseRing(world, c, 23, 10, 22, 9, 24, TIMBER);
        placeEllipseRing(world, c, 22, 9, 21, 8, 25, COPPER_GRATE);

        for (int z : new int[]{-6, 0, 6}) {
            parabolicRibX(world, c, z, 22, 9, 24, COPPER);
        }

        beamZ(world, c, -7, 7, 33, 0, TIMBER);
        for (int z : new int[]{-6, 0, 6}) {
            connectedBrace(world, c, new BlockPos(-10, 29, z), new BlockPos(0, 33, z), OAK);
            connectedBrace(world, c, new BlockPos(10, 29, z), new BlockPos(0, 33, z), OAK);
        }

        for (int x : new int[]{-15, 15}) {
            for (int z : new int[]{-6, 6}) {
                column(world, c, x, 25, z, 4, Blocks.CHAIN.getDefaultState());
                world.setBlockState(c.add(x, 24, z), LANTERN);
                world.setBlockState(c.add(x + (x < 0 ? 1 : -1), 25, z), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
                world.setBlockState(c.add(x + (x < 0 ? 2 : -2), 25, z), Blocks.AZALEA_LEAVES.getDefaultState());
            }
        }
    }

    private static void buildGatehouseBeacon(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-23, -19}) {
                column(world, o, x, 14, z, 10, TIMBER);
            }
        }
        beamX(world, o, -3, 3, 23, -23, TIMBER);
        beamX(world, o, -3, 3, 23, -19, TIMBER);
        beamZ(world, o, -23, -19, 23, -3, TIMBER);
        beamZ(world, o, -23, -19, 23, 3, TIMBER);

        for (int y = 17; y <= 21; y++) {
            for (int z = -22; z <= -20; z++) {
                world.setBlockState(o.add(-3, y, z), y == 19 ? COPPER_GRATE : GLASS);
                world.setBlockState(o.add(3, y, z), y == 19 ? COPPER_GRATE : GLASS);
            }
        }

        fillLayer(world, o, -5, 24, -25, 5, -17, DEEPSLATE_SLAB);
        fillLayer(world, o, -4, 25, -24, 4, -18, DEEPSLATE);
        fillLayer(world, o, -3, 26, -23, 3, -19, COPPER);
        fillLayer(world, o, -2, 27, -22, 2, -20, DEEPSLATE_SLAB);
        column(world, o, 0, 28, -21, 2, OAK);
        world.setBlockState(o.add(0, 30, -21), Blocks.LIGHTNING_ROD.getDefaultState());
    }

    private static void parabolicRibX(
            ServerWorld world,
            BlockPos center,
            int z,
            int halfSpan,
            int rise,
            int baseY,
            BlockState state
    ) {
        BlockPos previous = null;
        double span2 = (double) halfSpan * halfSpan;
        for (int x = -halfSpan; x <= halfSpan; x++) {
            int y = baseY + (int) Math.round(rise * (1.0D - x * x / span2));
            BlockPos next = new BlockPos(x, y, z);
            if (previous != null) {
                connectedBrace(world, center, previous, next, state);
            } else {
                world.setBlockState(center.add(next), state);
            }
            previous = next;
        }
    }

    private static void connectedBrace(
            ServerWorld world,
            BlockPos origin,
            BlockPos start,
            BlockPos end,
            BlockState state
    ) {
        int x = start.getX();
        int y = start.getY();
        int z = start.getZ();
        int dx = Math.abs(end.getX() - x);
        int dy = Math.abs(end.getY() - y);
        int dz = Math.abs(end.getZ() - z);
        int sx = Integer.signum(end.getX() - x);
        int sy = Integer.signum(end.getY() - y);
        int sz = Integer.signum(end.getZ() - z);
        int total = dx + dy + dz;
        int movedX = 0;
        int movedY = 0;
        int movedZ = 0;

        Direction.Axis initialAxis = dx >= dz && dx >= dy
                ? Direction.Axis.X
                : dz >= dy ? Direction.Axis.Z : Direction.Axis.Y;
        world.setBlockState(origin.add(x, y, z), orient(state, initialAxis));

        for (int step = 1; step <= total; step++) {
            double targetX = total == 0 ? 0 : (double) step * dx / total;
            double targetY = total == 0 ? 0 : (double) step * dy / total;
            double targetZ = total == 0 ? 0 : (double) step * dz / total;
            double lagX = movedX < dx ? targetX - movedX : Double.NEGATIVE_INFINITY;
            double lagY = movedY < dy ? targetY - movedY : Double.NEGATIVE_INFINITY;
            double lagZ = movedZ < dz ? targetZ - movedZ : Double.NEGATIVE_INFINITY;

            Direction.Axis axis;
            if (lagX >= lagY && lagX >= lagZ) {
                x += sx;
                movedX++;
                axis = Direction.Axis.X;
            } else if (lagZ >= lagY) {
                z += sz;
                movedZ++;
                axis = Direction.Axis.Z;
            } else {
                y += sy;
                movedY++;
                axis = Direction.Axis.Y;
            }
            world.setBlockState(origin.add(x, y, z), orient(state, axis));
        }
    }

    private static void connectLoop(ServerWorld world, BlockPos o, int[][] points, int y, BlockState state) {
        for (int i = 0; i < points.length; i++) {
            int[] a = points[i];
            int[] b = points[(i + 1) % points.length];
            connectedBrace(world, o, new BlockPos(a[0], y, a[1]), new BlockPos(b[0], y, b[1]), state);
        }
    }

    private static void placeEllipseRing(
            ServerWorld world,
            BlockPos center,
            int outerX,
            int outerZ,
            int innerX,
            int innerZ,
            int y,
            BlockState state
    ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(outerX, outerZ, innerX, innerZ, y)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }

    private static void placeFilledEllipse(
            ServerWorld world,
            BlockPos center,
            int radiusX,
            int radiusZ,
            int y,
            BlockState state
    ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipse(radiusX, radiusZ, y)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }

    private static void column(ServerWorld world, BlockPos o, int x, int y, int z, int height, BlockState state) {
        BlockState oriented = orient(state, Direction.Axis.Y);
        for (int dy = 0; dy < height; dy++) {
            world.setBlockState(o.add(x, y + dy, z), oriented);
        }
    }

    private static void beamX(ServerWorld world, BlockPos o, int minX, int maxX, int y, int z, BlockState state) {
        BlockState oriented = orient(state, Direction.Axis.X);
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, z), oriented);
        }
    }

    private static void beamZ(ServerWorld world, BlockPos o, int minZ, int maxZ, int y, int x, BlockState state) {
        BlockState oriented = orient(state, Direction.Axis.Z);
        for (int z = minZ; z <= maxZ; z++) {
            world.setBlockState(o.add(x, y, z), oriented);
        }
    }

    private static BlockState orient(BlockState state, Direction.Axis axis) {
        return state.contains(Properties.AXIS) ? state.with(Properties.AXIS, axis) : state;
    }

    private static void fillLayer(
            ServerWorld world,
            BlockPos o,
            int minX,
            int y,
            int minZ,
            int maxX,
            int maxZ,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(o.add(x, y, z), state);
            }
        }
    }
}