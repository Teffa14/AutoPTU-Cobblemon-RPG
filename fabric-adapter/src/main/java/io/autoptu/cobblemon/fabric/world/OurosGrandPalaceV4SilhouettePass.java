package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Final silhouette accents for Palace V4.
 *
 * Four outer corner towers break the otherwise regular pavilion grid without touching either open
 * court or consuming any of the nineteen authored rooms. Each tower overlaps an existing front/rear
 * threshold and one room-wall corner, so its masonry and roof are six-neighbor connected to the
 * palace rather than decorative floating scenery.
 */
final class OurosGrandPalaceV4SilhouettePass {
    private static final BlockState FOUNDATION = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState PLINTH = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState TRIM = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();

    private OurosGrandPalaceV4SilhouettePass() {}

    static void apply(ServerWorld world, BlockPos o) {
        cornerTower(world, o, -54, -48, -59, -53, true, true);
        cornerTower(world, o, 48, 54, -59, -53, false, true);
        cornerTower(world, o, -54, -48, 53, 59, true, false);
        cornerTower(world, o, 48, 54, 53, 59, false, false);
    }

    private static void cornerTower(ServerWorld world, BlockPos o,
                                    int x1, int x2, int z1, int z2,
                                    boolean west, boolean front) {
        // Deep footings overlap the existing threshold/wing foundation at the inner corner.
        fill(world, o, x1, -3, z1, x2, -3, z2, FOUNDATION);
        fill(world, o, x1, -2, z1, x2, -2, z2, Blocks.TUFF_BRICKS.getDefaultState());
        fill(world, o, x1, -1, z1, x2, 1, z2, PLINTH);

        // A seven-block tower body gives the corner real vertical mass but preserves a hollow core.
        for (int y = 2; y <= 31; y++) {
            BlockState wall = (y == 14 || y == 29) ? ASHLAR : WALL;
            fill(world, o, x1, y, z1, x2, y, z1, wall);
            fill(world, o, x1, y, z2, x2, y, z2, wall);
            fill(world, o, x1, y, z1, x1, y, z2, wall);
            fill(world, o, x2, y, z1, x2, y, z2, wall);
        }

        // Corner pilasters and broad cornices keep the tower legible from a long render distance.
        for (int x : new int[]{x1, x2}) {
            for (int z : new int[]{z1, z2}) {
                fill(world, o, x, 2, z, x, 31, z, ASHLAR);
            }
        }
        fill(world, o, x1, 13, z1, x2, 14, z2, TRIM);
        fill(world, o, x1, 29, z1, x2, 31, z2, ASHLAR);

        addOuterWindows(world, o, x1, x2, z1, z2, west, front, 7);
        addOuterWindows(world, o, x1, x2, z1, z2, west, front, 20);

        // Compact stepped copper/deepslate cap rises above the regular side-wing mansards.
        fill(world, o, x1, 32, z1, x2, 32, z2, ROOF);
        fill(world, o, x1 + 1, 33, z1 + 1, x2 - 1, 34, z2 - 1, ROOF);
        fill(world, o, x1 + 2, 35, z1 + 2, x2 - 2, 37, z2 - 2, COPPER);
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        fill(world, o, cx, 38, cz, cx, 42, cz, COPPER);
        world.setBlockState(o.add(cx, 43, cz), Blocks.LIGHTNING_ROD.getDefaultState());
    }

    private static void addOuterWindows(ServerWorld world, BlockPos o,
                                        int x1, int x2, int z1, int z2,
                                        boolean west, boolean front, int baseY) {
        int outerX = west ? x1 : x2;
        int outerZ = front ? z1 : z2;
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;

        fill(world, o, outerX, baseY, cz - 1, outerX, baseY + 5, cz + 1, ASHLAR);
        fill(world, o, outerX, baseY + 1, cz, outerX, baseY + 4, cz, GLASS);
        fill(world, o, cx - 1, baseY, outerZ, cx + 1, baseY + 5, outerZ, ASHLAR);
        fill(world, o, cx, baseY + 1, outerZ, cx, baseY + 4, outerZ, GLASS);
    }
}
