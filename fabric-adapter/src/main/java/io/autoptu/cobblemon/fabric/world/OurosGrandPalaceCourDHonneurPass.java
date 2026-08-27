package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.stair;

/**
 * Forward ceremonial composition for the Grand Palace V2.
 *
 * The interior room grid intentionally remains stable. This pass uses the larger exact-viewer
 * envelope to project architecture beyond that grid: two low state wings frame a cour d'honneur,
 * a monumental gate marks the approach, terraces and water features create depth, and the original
 * rectangular footprint stops reading as the building's silhouette from above.
 */
final class OurosGrandPalaceCourDHonneurPass {
    private static final BlockState FOUNDATION = Blocks.DEEPSLATE_BRICKS.getDefaultState();
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState TRIM = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState DARK = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_COPPER.getDefaultState();
    private static final BlockState COPPER_STAIR = Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS_PANE.getDefaultState();

    private OurosGrandPalaceCourDHonneurPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        clearApproach(world, o);
        buildCourtTerrace(world, o);
        buildForwardWing(world, o, -1);
        buildForwardWing(world, o, 1);
        buildStateGate(world, o);
        buildReflectingBasins(world, o);
        buildProcessionalDetails(world, o);
    }

    private static void clearApproach(ServerWorld world, BlockPos o) {
        clear(world, o, -53, -3, -72, 53, 36, -58);
    }

    private static void buildCourtTerrace(ServerWorld world, BlockPos o) {
        // Deliberately not one full rectangle: three stepped terraces make the approach read as
        // landscape architecture rather than a giant platform.
        fill(world, o, -48, -1, -69, 48, 0, -58, FOUNDATION);
        fill(world, o, -42, 0, -66, 42, 0, -58, Blocks.STONE_BRICKS.getDefaultState());
        fill(world, o, -34, 1, -63, 34, 1, -58, Blocks.POLISHED_ANDESITE.getDefaultState());

        // Axial red-carpet promenade and pale border.
        fill(world, o, -4, 1, -69, 4, 1, -57, Blocks.RED_CARPET.getDefaultState());
        fill(world, o, -6, 1, -69, -5, 1, -57, ASHLAR);
        fill(world, o, 5, 1, -69, 6, 1, -57, ASHLAR);

        // Wide arrival steps rising into the entrance pavilion.
        for (int step = 0; step < 3; step++) {
            fill(world, o, -12 + step, 1 + step, -61 + step,
                    12 - step, 1 + step, -59 + step, Blocks.POLISHED_DIORITE.getDefaultState());
        }
    }

    private static void buildForwardWing(ServerWorld world, BlockPos o, int side) {
        int outerX1 = side < 0 ? -50 : 31;
        int outerX2 = side < 0 ? -31 : 50;
        int innerX = side < 0 ? -31 : 31;

        // Foundation ties each wing into both the court and original front pavilion.
        fill(world, o, outerX1, 0, -68, outerX2, 2, -55, FOUNDATION);
        fill(world, o, outerX1, 3, -68, outerX2, 16, -57, WALL);

        // Recess court-facing arcade bays.
        int faceX = side < 0 ? outerX2 : outerX1;
        for (int z = -65; z <= -59; z += 6) {
            int x1 = side < 0 ? faceX - 2 : faceX;
            int x2 = side < 0 ? faceX : faceX + 2;
            clear(world, o, x1, 5, z - 2, x2, 12, z + 2);
            fill(world, o, x1, 5, z - 2, x2, 12, z - 2, ASHLAR);
            fill(world, o, x1, 5, z + 2, x2, 12, z + 2, ASHLAR);
            fill(world, o, x1, 12, z - 2, x2, 13, z + 2, TRIM);
            fill(world, o, faceX, 6, z - 1, faceX, 11, z + 1, GLASS);
        }

        // Massive terminal pavilion at the gate end.
        int towerX = side < 0 ? -44 : 44;
        fill(world, o, towerX - 5, 2, -70, towerX + 5, 20, -60, WALL);
        fill(world, o, towerX - 6, 2, -71, towerX + 6, 4, -59, DARK);
        fill(world, o, towerX - 6, 15, -71, towerX + 6, 17, -59, ASHLAR);
        for (int z : new int[]{-68, -63}) {
            clear(world, o, towerX - 2, 6, z, towerX + 2, 12, z);
            fill(world, o, towerX - 2, 6, z, towerX + 2, 12, z, GLASS);
            fill(world, o, towerX, 6, z - 1, towerX, 12, z + 1, TRIM);
        }

        // Closed copper pavilion roof with full-block backing below every visible course.
        for (int layer = 0; layer <= 6; layer++) {
            int x1 = towerX - 7 + layer;
            int x2 = towerX + 7 - layer;
            int z1 = -72 + layer;
            int z2 = -58 - layer;
            int y = 21 + layer;
            if (x1 > x2 || z1 > z2) break;
            perimeter(world, o, x1, x2, z1, z2, y - 1, layer == 0 ? DARK : COPPER);
            for (int x = x1; x <= x2; x++) {
                world.setBlockState(o.add(x, y, z1), stair(COPPER_STAIR, Direction.SOUTH));
                world.setBlockState(o.add(x, y, z2), stair(COPPER_STAIR, Direction.NORTH));
            }
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                world.setBlockState(o.add(x1, y, z), stair(COPPER_STAIR, Direction.EAST));
                world.setBlockState(o.add(x2, y, z), stair(COPPER_STAIR, Direction.WEST));
            }
        }
        fill(world, o, towerX - 1, 27, -66, towerX + 1, 30, -64, COPPER);
        world.setBlockState(o.add(towerX, 31, -65), Blocks.LIGHTNING_ROD.getDefaultState());

        // The inner return physically joins the wing into the original palace facade.
        fill(world, o,
                side < 0 ? -40 : 31, 3, -59,
                side < 0 ? innerX : 40, 16, -55, WALL);
    }

    private static void buildStateGate(ServerWorld world, BlockPos o) {
        // Four masonry pylons and a pierced metal/bamboo screen create a ceremonial threshold while
        // keeping the central axis walkable.
        for (int x : new int[]{-27, -19, 19, 27}) {
            fill(world, o, x - 2, 0, -72, x + 2, 13, -68, ASHLAR);
            fill(world, o, x - 3, 0, -72, x + 3, 2, -67, DARK);
            fill(world, o, x - 3, 11, -72, x + 3, 13, -67, TRIM);
            fill(world, o, x - 1, 14, -71, x + 1, 18, -69, COPPER);
            world.setBlockState(o.add(x, 19, -70), Blocks.LIGHTNING_ROD.getDefaultState());
        }

        // Side screens stop short of the central approach opening.
        gateScreen(world, o, -46, -30);
        gateScreen(world, o, -16, -7);
        gateScreen(world, o, 7, 16);
        gateScreen(world, o, 30, 46);
    }

    private static void gateScreen(ServerWorld world, BlockPos o, int x1, int x2) {
        fill(world, o, x1, 2, -70, x2, 2, -70, DARK);
        fill(world, o, x1, 9, -70, x2, 9, -70, TRIM);
        for (int x = x1; x <= x2; x += 2) {
            fill(world, o, x, 3, -70, x, 8, -70, Blocks.COPPER_GRATE.getDefaultState());
        }
    }

    private static void buildReflectingBasins(ServerWorld world, BlockPos o) {
        for (int side : new int[]{-1, 1}) {
            int cx = side * 16;
            fill(world, o, cx - 7, 1, -66, cx + 7, 1, -60, DARK);
            fill(world, o, cx - 6, 2, -65, cx + 6, 2, -61, Blocks.WATER.getDefaultState());
            fill(world, o, cx - 1, 1, -64, cx + 1, 3, -62, ASHLAR);
            world.setBlockState(o.add(cx, 4, -63), Blocks.SEA_LANTERN.getDefaultState());
        }
    }

    private static void buildProcessionalDetails(ServerWorld world, BlockPos o) {
        // Lamp pylons and clipped hedges line the court without filling it with repetitive poles.
        for (int x : new int[]{-27, 27}) {
            for (int z : new int[]{-62, -58}) {
                fill(world, o, x - 1, 1, z - 1, x + 1, 3, z + 1, ASHLAR);
                fill(world, o, x, 4, z, x, 7, z, TRIM);
                world.setBlockState(o.add(x, 8, z), Blocks.LANTERN.getDefaultState());
            }
        }
        for (int x1 : new int[]{-49, 38}) {
            fill(world, o, x1, 1, -58, x1 + 11, 4, -57, Blocks.AZALEA_LEAVES.getDefaultState());
        }
    }

    private static void perimeter(ServerWorld world, BlockPos o,
                                  int x1, int x2, int z1, int z2, int y, BlockState state) {
        fill(world, o, x1, y, z1, x2, y, z1, state);
        fill(world, o, x1, y, z2, x2, y, z2, state);
        if (z2 - z1 > 1) {
            fill(world, o, x1, y, z1 + 1, x1, y, z2 - 1, state);
            fill(world, o, x2, y, z1 + 1, x2, y, z2 - 1, state);
        }
    }
}
