package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.world.build.OurosVoxelGeometry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Vertical architectural pass for Meridian Canopy Gym.
 *
 * The previous vertical experiment proved the expanded review envelope but produced too much exposed
 * scaffolding and several competing roof objects. This pass keeps the additional height while using
 * it as architectural volume: one compact conservatory clerestory/cupola and one arena canopy that
 * rises directly from the battle bowl. Minecraft blocks remain presentation only and do not assign
 * PTU terrain or battle semantics.
 */
public final class MeridianCanopyGymVerticalLandmarkPass {
    private static final BlockState TIMBER = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState OAK = Blocks.STRIPPED_OAK_LOG.getDefaultState();
    private static final BlockState COPPER = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState WEATHERED_COPPER = Blocks.WEATHERED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_GRATE = Blocks.OXIDIZED_COPPER_GRATE.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState TINTED_GLASS = Blocks.TINTED_GLASS.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState DEEPSLATE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private MeridianCanopyGymVerticalLandmarkPass() {}

    public static void apply(ServerWorld world, BlockPos origin) {
        clearHighAirspace(world, origin);
        buildConservatoryClerestory(world, origin);
        buildArenaCanopy(world, origin);
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

    private static void buildConservatoryClerestory(ServerWorld world, BlockPos o) {
        int centerZ = -3;
        int[][] posts = {
                {-6, centerZ - 5}, {6, centerZ - 5},
                {-6, centerZ + 5}, {6, centerZ + 5}
        };
        for (int[] p : posts) {
            column(world, o, p[0], 13, p[1], 15, TIMBER);
            world.setBlockState(o.add(p[0], 19, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
            world.setBlockState(o.add(p[0], 26, p[1]), WEATHERED_COPPER);
        }

        rectangleRing(world, o, -8, centerZ - 7, 8, centerZ + 7, 20, DEEPSLATE_SLAB);
        rectangleRing(world, o, -7, centerZ - 6, 7, centerZ + 6, 21, TIMBER);
        beamX(world, o, -6, 6, 27, centerZ - 5, TIMBER);
        beamX(world, o, -6, 6, 27, centerZ + 5, TIMBER);
        beamZ(world, o, centerZ - 5, centerZ + 5, 27, -6, TIMBER);
        beamZ(world, o, centerZ - 5, centerZ + 5, 27, 6, TIMBER);

        for (int y = 21; y <= 26; y++) {
            BlockState wall = y == 24 ? COPPER_GRATE : GLASS;
            for (int x = -5; x <= 5; x++) {
                if (x == -5 || x == 5 || Math.floorMod(x, 4) != 0) {
                    world.setBlockState(o.add(x, y, centerZ - 5), wall);
                    world.setBlockState(o.add(x, y, centerZ + 5), wall);
                }
            }
            for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                world.setBlockState(o.add(-6, y, z), wall);
                world.setBlockState(o.add(6, y, z), wall);
            }
        }

        for (int x : new int[]{-2, 2}) {
            column(world, o, x, 21, centerZ - 5, 6, COPPER_GRATE);
            column(world, o, x, 21, centerZ + 5, 6, COPPER_GRATE);
        }
        for (int z : new int[]{centerZ - 2, centerZ + 2}) {
            column(world, o, -6, 21, z, 6, COPPER_GRATE);
            column(world, o, 6, 21, z, 6, COPPER_GRATE);
        }
        for (int[] p : new int[][]{{-4, centerZ}, {4, centerZ}}) {
            world.setBlockState(o.add(p[0], 23, p[1]), Blocks.OXIDIZED_COPPER_BULB.getDefaultState());
        }

        // Four visible roof rafters extend the y=27 wall plate into the broad y=28 eave. They make
        // the roof both structurally connected and visually believable instead of a hovering cap.
        beamX(world, o, -8, 8, 28, centerZ - 5, TIMBER);
        beamX(world, o, -8, 8, 28, centerZ + 5, TIMBER);
        beamZ(world, o, centerZ - 7, centerZ + 7, 28, -6, TIMBER);
        beamZ(world, o, centerZ - 7, centerZ + 7, 28, 6, TIMBER);

        roofPlate(world, o, -8, centerZ - 7, 8, centerZ + 7, 28, DEEPSLATE_SLAB);
        roofPlate(world, o, -7, centerZ - 6, 7, centerZ + 6, 29, DEEPSLATE);
        roofPlate(world, o, -6, centerZ - 5, 6, centerZ + 5, 30, WEATHERED_COPPER);
        roofPlate(world, o, -5, centerZ - 4, 5, centerZ + 4, 31, DEEPSLATE);
        roofPlate(world, o, -4, centerZ - 3, 4, centerZ + 3, 32, COPPER);

        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{centerZ - 2, centerZ + 2}) {
                column(world, o, x, 33, z, 3, TIMBER);
            }
        }
        for (int y = 33; y <= 35; y++) {
            for (int x = -2; x <= 2; x++) {
                world.setBlockState(o.add(x, y, centerZ - 2), y == 34 ? COPPER_GRATE : TINTED_GLASS);
                world.setBlockState(o.add(x, y, centerZ + 2), y == 34 ? COPPER_GRATE : TINTED_GLASS);
            }
        }
        rectangleRing(world, o, -4, centerZ - 3, 4, centerZ + 3, 36, DEEPSLATE_SLAB);
        roofPlate(world, o, -3, centerZ - 2, 3, centerZ + 2, 37, WEATHERED_COPPER);
        column(world, o, 0, 38, centerZ, 3, OAK);
        world.setBlockState(o.add(0, 41, centerZ), Blocks.LIGHTNING_ROD.getDefaultState());

        for (int x : new int[]{-3, 0, 3}) {
            for (int y = 26; y >= 23; y--) {
                world.setBlockState(o.add(x, y, centerZ), Blocks.CHAIN.getDefaultState());
            }
            world.setBlockState(o.add(x, 22, centerZ), LANTERN);
        }
    }

    private static void buildArenaCanopy(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);
        int[][] piers = {
                {-22, -5}, {0, -10}, {22, -5},
                {-22, 5}, {0, 10}, {22, 5}
        };
        for (int[] p : piers) {
            column(world, c, p[0], 13, p[1], 6, TIMBER);
            world.setBlockState(c.add(p[0], 18, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
        }

        placeEllipseRing(world, c, 25, 11, 23, 9, 17, DEEPSLATE_SLAB);
        placeEllipseRing(world, c, 24, 10, 23, 9, 18, COPPER_GRATE);

        for (int z : new int[]{-8, -4, 0, 4, 8}) {
            parabolicRibX(world, c, z, 22, 11, 17, COPPER);
        }
        for (int z : new int[]{-7, -5, -3, -1, 1, 3, 5, 7}) {
            parabolicInfillX(world, c, z, 21, 10, 18, GLASS);
        }

        beamZ(world, c, -9, 9, 28, 0, TIMBER);
        beamZ(world, c, -8, 8, 24, -14, TIMBER);
        beamZ(world, c, -8, 8, 24, 14, TIMBER);

        for (int z = -7; z <= 7; z++) {
            world.setBlockState(c.add(-22, 19, z), Math.floorMod(z, 3) == 0 ? COPPER_GRATE : GLASS);
            world.setBlockState(c.add(22, 19, z), Math.floorMod(z, 3) == 0 ? COPPER_GRATE : GLASS);
        }

        for (int x : new int[]{-14, 14}) {
            for (int z : new int[]{-4, 4}) {
                for (int y = 23; y >= 21; y--) {
                    world.setBlockState(c.add(x, y, z), Blocks.CHAIN.getDefaultState());
                }
                world.setBlockState(c.add(x, 20, z), LANTERN);
                world.setBlockState(c.add(x + (x < 0 ? 1 : -1), 22, z), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            }
        }
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
                faceConnectedLine(world, center, previous, next, state);
            } else {
                world.setBlockState(center.add(next), state);
            }
            previous = next;
        }
    }

    private static void parabolicInfillX(
            ServerWorld world,
            BlockPos center,
            int z,
            int halfSpan,
            int rise,
            int baseY,
            BlockState state
    ) {
        double span2 = (double) halfSpan * halfSpan;
        for (int x = -halfSpan; x <= halfSpan; x++) {
            int y = baseY + (int) Math.round(rise * (1.0D - x * x / span2));
            world.setBlockState(center.add(x, y, z), state);
            if (x % 3 != 0 && y > baseY + 2) {
                world.setBlockState(center.add(x, y - 1, z), state);
            }
        }
    }

    private static void faceConnectedLine(
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

    private static void rectangleRing(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            int y,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, minZ), state);
            world.setBlockState(o.add(x, y, maxZ), state);
        }
        for (int z = minZ; z <= maxZ; z++) {
            world.setBlockState(o.add(minX, y, z), state);
            world.setBlockState(o.add(maxX, y, z), state);
        }
    }

    private static void roofPlate(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            int y,
            BlockState state
    ) {
        rectangleRing(world, o, minX, minZ, maxX, maxZ, y, state);
        if (maxX - minX <= 7 || maxZ - minZ <= 5) {
            for (int x = minX + 1; x < maxX; x++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    world.setBlockState(o.add(x, y, z), state);
                }
            }
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
}