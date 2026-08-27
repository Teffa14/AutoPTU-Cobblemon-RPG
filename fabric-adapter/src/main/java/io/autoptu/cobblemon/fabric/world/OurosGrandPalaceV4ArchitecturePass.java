package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/** Exterior-first architectural frame for the courtyard-based Palace V4. */
final class OurosGrandPalaceV4ArchitecturePass {
    private static final BlockState FOUNDATION = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState FOUNDATION_2 = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState PLINTH = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState GOLD = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState TIMBER = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();

    private OurosGrandPalaceV4ArchitecturePass() {}

    static void apply(ServerWorld world, BlockPos o) {
        buildArticulatedFoundations(world, o);
        buildCourtyardGrounds(world, o);
        buildOuterWingFrames(world, o);
        buildInnerCourtArcades(world, o);
        buildTransverseBridges(world, o);
        buildFrontCeremonialArrival(world, o);
        buildRearGardenTerrace(world, o);
    }

    private static void buildArticulatedFoundations(ServerWorld world, BlockPos o) {
        for (OurosGrandPalaceBuildKit.Room room : groundSideRooms()) foundationUnder(world, o, room);
        for (OurosGrandPalaceBuildKit.Room room : ceremonialRooms()) foundationUnder(world, o, room);

        // Narrow external service/loggia strips join each side wing without filling the courts.
        foundationRect(world, o, -54, -53, -51, 53);
        foundationRect(world, o, 51, -53, 54, 53);

        // Three transverse connectors cross the courts. Most of each court remains true open space.
        for (int[] band : new int[][]{{-30, -27}, {-2, 1}, {26, 29}}) {
            foundationRect(world, o, WEST_COURT_MIN_X, band[0], WEST_COURT_MAX_X, band[1]);
            foundationRect(world, o, EAST_COURT_MIN_X, band[0], EAST_COURT_MAX_X, band[1]);
        }

        // Front and rear thresholds connect the three major masses without a universal plate.
        foundationRect(world, o, -50, -57, 50, -54);
        foundationRect(world, o, -50, 54, 50, 57);
    }

    private static void foundationUnder(ServerWorld world, BlockPos o, OurosGrandPalaceBuildKit.Room r) {
        foundationRect(world, o, r.minX(), r.minZ(), r.maxX(), r.maxZ());
    }

    private static void foundationRect(ServerWorld world, BlockPos o, int x1, int z1, int x2, int z2) {
        fill(world, o, x1, -3, z1, x2, -3, z2, FOUNDATION);
        fill(world, o, x1, -2, z1, x2, -2, z2, FOUNDATION_2);
        fill(world, o, x1, -1, z1, x2, -1, z2, PLINTH);
    }

    private static void buildCourtyardGrounds(ServerWorld world, BlockPos o) {
        for (int side : new int[]{-1, 1}) {
            int x1 = side < 0 ? WEST_COURT_MIN_X : EAST_COURT_MIN_X;
            int x2 = side < 0 ? WEST_COURT_MAX_X : EAST_COURT_MAX_X;

            // Four garden rooms separated by the bridge bands. They are open to the sky.
            for (int[] zRange : new int[][]{{-52, -31}, {-25, -3}, {3, 25}, {31, 52}}) {
                fill(world, o, x1, 0, zRange[0], x2, 0, zRange[1], Blocks.MOSS_BLOCK.getDefaultState());
                fill(world, o, x1, 0, zRange[0], x1 + 1, 0, zRange[1], Blocks.POLISHED_ANDESITE.getDefaultState());
                fill(world, o, x2 - 1, 0, zRange[0], x2, 0, zRange[1], Blocks.POLISHED_ANDESITE.getDefaultState());
                int centerX = (x1 + x2) / 2;
                fill(world, o, centerX, 0, zRange[0] + 2, centerX + 1, 0, zRange[1] - 2,
                        Blocks.POLISHED_DIORITE.getDefaultState());
                courtyardFountain(world, o, centerX, (zRange[0] + zRange[1]) / 2);
            }
        }
    }

    private static void courtyardFountain(ServerWorld world, BlockPos o, int x, int z) {
        fill(world, o, x - 3, 0, z - 3, x + 4, 0, z + 3, ASHLAR);
        fill(world, o, x - 2, 1, z - 2, x + 3, 1, z + 2, Blocks.WATER.getDefaultState());
        fill(world, o, x, 1, z, x + 1, 3, z, COPPER);
        world.setBlockState(o.add(x, 4, z), Blocks.SEA_LANTERN.getDefaultState());
        world.setBlockState(o.add(x + 1, 4, z), Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildOuterWingFrames(ServerWorld world, BlockPos o) {
        for (int side : new int[]{-1, 1}) {
            int outer = side < 0 ? -52 : 52;
            int roomEdge = side < 0 ? WEST_WING_MIN_X : EAST_WING_MAX_X;
            Direction inward = side < 0 ? Direction.EAST : Direction.WEST;

            // Separate pavilion orders for each room row. Gaps remain visible between them.
            for (int[] zr : new int[][]{{-53, -31}, {-25, -3}, {3, 25}, {31, 53}}) {
                fill(world, o, Math.min(outer, roomEdge), 1, zr[0], Math.max(outer, roomEdge), 3, zr[1], PLINTH);
                for (int z = zr[0] + 2; z <= zr[1] - 2; z += 5) {
                    exteriorColumn(world, o, outer, z, 29);
                }
                fill(world, o, outer, 13, zr[0] + 1, outer, 14, zr[1] - 1, GOLD);
                fill(world, o, outer, 28, zr[0] + 1, outer, 29, zr[1] - 1, ASHLAR);
                for (int z = zr[0] + 3; z <= zr[1] - 3; z += 6) {
                    world.setBlockState(o.add(outer - (side < 0 ? -1 : 1), 9, z), litLantern(false));
                }
            }

            // A narrow exterior gallery provides alternate circulation along the garden-facing wings.
            fill(world, o, Math.min(outer, roomEdge), 15, -53, Math.max(outer, roomEdge), 15, 53,
                    Blocks.DARK_OAK_PLANKS.getDefaultState());
            int railX = side < 0 ? -54 : 54;
            for (int z = -52; z <= 52; z += 2) {
                world.setBlockState(o.add(railX, 16, z), Blocks.DARK_OAK_FENCE.getDefaultState());
            }
            for (int z = -50; z <= 50; z += 10) {
                world.setBlockState(o.add(railX, 17, z), stair(Blocks.DARK_OAK_STAIRS.getDefaultState(), inward));
            }
        }
    }

    private static void exteriorColumn(ServerWorld world, BlockPos o, int x, int z, int topY) {
        fill(world, o, x - 1, 1, z - 1, x + 1, 2, z + 1, PLINTH);
        fill(world, o, x, 3, z, x, topY - 2, z, ASHLAR);
        fill(world, o, x - 1, topY - 1, z - 1, x + 1, topY, z + 1, GOLD);
    }

    private static void buildInnerCourtArcades(ServerWorld world, BlockPos o) {
        // Four parallel colonnades define the two courts without roofing their centers.
        for (int x : new int[]{WEST_WING_MAX_X, CENTRAL_MIN_X, CENTRAL_MAX_X, EAST_WING_MIN_X}) {
            for (int z = -50; z <= 50; z += 6) {
                fill(world, o, x, 1, z, x, 12, z, ASHLAR);
                fill(world, o, x - 1, 12, z - 1, x + 1, 13, z + 1, GOLD);
            }
            fill(world, o, x, 13, -53, x, 13, 53, TIMBER);
        }

        // Ground and upper loggia strips hug the buildings, leaving twelve blocks of open garden.
        fill(world, o, WEST_WING_MAX_X, 0, -53, WEST_WING_MAX_X + 2, 0, 53, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(world, o, CENTRAL_MIN_X - 2, 0, -53, CENTRAL_MIN_X, 0, 53, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(world, o, CENTRAL_MAX_X, 0, -53, CENTRAL_MAX_X + 2, 0, 53, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(world, o, EAST_WING_MIN_X - 2, 0, -53, EAST_WING_MIN_X, 0, 53, Blocks.POLISHED_DIORITE.getDefaultState());

        fill(world, o, WEST_WING_MAX_X, 15, -53, WEST_WING_MAX_X + 2, 15, 53, Blocks.DARK_OAK_PLANKS.getDefaultState());
        fill(world, o, CENTRAL_MIN_X - 2, 15, -53, CENTRAL_MIN_X, 15, 53, Blocks.DARK_OAK_PLANKS.getDefaultState());
        fill(world, o, CENTRAL_MAX_X, 15, -53, CENTRAL_MAX_X + 2, 15, 53, Blocks.DARK_OAK_PLANKS.getDefaultState());
        fill(world, o, EAST_WING_MIN_X - 2, 15, -53, EAST_WING_MIN_X, 15, 53, Blocks.DARK_OAK_PLANKS.getDefaultState());
    }

    private static void buildTransverseBridges(ServerWorld world, BlockPos o) {
        for (int[] band : new int[][]{{-30, -27}, {-2, 1}, {26, 29}}) {
            buildBridgeAcrossCourt(world, o, WEST_COURT_MIN_X, WEST_COURT_MAX_X, band[0], band[1]);
            buildBridgeAcrossCourt(world, o, EAST_COURT_MIN_X, EAST_COURT_MAX_X, band[0], band[1]);
        }
    }

    private static void buildBridgeAcrossCourt(ServerWorld world, BlockPos o, int x1, int x2, int z1, int z2) {
        fill(world, o, x1, 0, z1, x2, 0, z2, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(world, o, x1, 15, z1, x2, 15, z2, Blocks.DARK_OAK_PLANKS.getDefaultState());
        for (int x = x1; x <= x2; x += 4) {
            fill(world, o, x, 1, z1, x, 13, z1, ASHLAR);
            fill(world, o, x, 1, z2, x, 13, z2, ASHLAR);
            fill(world, o, x, 16, z1, x, 28, z1, ASHLAR);
            fill(world, o, x, 16, z2, x, 28, z2, ASHLAR);
        }
        fill(world, o, x1, 13, z1, x2, 14, z2, TIMBER);
        fill(world, o, x1, 28, z1, x2, 29, z2, GOLD);
    }

    private static void buildFrontCeremonialArrival(ServerWorld world, BlockPos o) {
        // Three separate front bodies read around recessed forecourts. The central portico projects.
        fill(world, o, -50, 1, -57, -28, 16, -54, WALL);
        fill(world, o, 28, 1, -57, 50, 16, -54, WALL);
        fill(world, o, -13, 1, -61, 13, 20, -54, WALL);

        for (int x : new int[]{-10, -5, 5, 10}) exteriorColumn(world, o, x, -62, 21);
        fill(world, o, -14, 20, -63, 14, 22, -54, ASHLAR);
        for (int layer = 0; layer < 6; layer++) {
            int half = 12 - layer * 2;
            if (half < 2) break;
            fill(world, o, -half, 23 + layer, -61, half, 23 + layer, -55, GOLD);
        }

        for (int step = 0; step < 5; step++) {
            fill(world, o, -16 + step, step - 1, -68 + step,
                    16 - step, step - 1, -64 + step, Blocks.POLISHED_DIORITE.getDefaultState());
        }
    }

    private static void buildRearGardenTerrace(ServerWorld world, BlockPos o) {
        foundationRect(world, o, -22, 54, 22, 64);
        fill(world, o, -22, 0, 54, 22, 0, 64, Blocks.POLISHED_ANDESITE.getDefaultState());
        for (int x = -20; x <= 20; x += 5) {
            fill(world, o, x, 1, 61, x, 13, 61, ASHLAR);
            fill(world, o, x - 1, 13, 60, x + 1, 14, 62, GOLD);
        }
        fill(world, o, -21, 14, 55, 21, 14, 62, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int x : new int[]{-15, -5, 5, 15}) {
            world.setBlockState(o.add(x, 3, 63), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            world.setBlockState(o.add(x, 2, 63), Blocks.DECORATED_POT.getDefaultState());
        }
    }
}
