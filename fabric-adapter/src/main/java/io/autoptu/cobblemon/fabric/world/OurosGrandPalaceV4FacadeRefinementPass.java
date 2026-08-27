package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Front-elevation refinement for Palace V4.
 *
 * The original courtyard plan created real voids in plan, but its first exterior pass still drew a
 * continuous sixteen-block-high ribbon across both front wings. From an oblique browser view that
 * ribbon visually reassembled the palace into a box. This pass removes the ribbon and replaces it
 * with two compact outer pavilions plus open colonnaded forecourts around the projecting central
 * corps. The nineteen room footprints remain untouched behind z=-53.
 */
final class OurosGrandPalaceV4FacadeRefinementPass {
    private static final BlockState FOUNDATION = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState FOUNDATION_2 = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState PLINTH = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState TRIM = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();

    private OurosGrandPalaceV4FacadeRefinementPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        // Remove the two flat front wing ribbons authored by the first architecture pass. Keep all
        // foundations and the central projecting portico intact.
        clear(world, o, -50, 1, -57, -28, 18, -54);
        clear(world, o, 28, 1, -57, 50, 18, -54);

        buildOuterFrontPavilion(world, o, -50, -42);
        buildOuterFrontPavilion(world, o, 42, 50);
        buildRecessedForecourt(world, o, -40, -30);
        buildRecessedForecourt(world, o, 30, 40);
    }

    private static void buildOuterFrontPavilion(ServerWorld world, BlockPos o, int x1, int x2) {
        // The pavilion projects seven blocks beyond the room wall and reconnects at z=-54/-53.
        fill(world, o, x1, -3, -60, x2, -3, -54, FOUNDATION);
        fill(world, o, x1, -2, -60, x2, -2, -54, FOUNDATION_2);
        fill(world, o, x1, -1, -60, x2, 1, -54, PLINTH);

        for (int y = 2; y <= 17; y++) {
            BlockState wall = y == 13 ? TRIM : WALL;
            fill(world, o, x1, y, -60, x2, y, -60, wall);
            fill(world, o, x1, y, -54, x2, y, -54, wall);
            fill(world, o, x1, y, -60, x1, y, -54, wall);
            fill(world, o, x2, y, -60, x2, y, -54, wall);
        }

        // Strong corner quoins and one tall front window keep the pavilion readable at distance.
        for (int x : new int[]{x1, x2}) {
            fill(world, o, x, 2, -60, x, 17, -60, ASHLAR);
            fill(world, o, x, 2, -54, x, 17, -54, ASHLAR);
        }
        int cx = (x1 + x2) / 2;
        fill(world, o, cx - 2, 5, -60, cx + 2, 12, -60, ASHLAR);
        fill(world, o, cx - 1, 6, -60, cx + 1, 11, -60, GLASS);
        fill(world, o, x1, 16, -60, x2, 18, -54, ASHLAR);

        // Low independent hipped cap. The main wing mansard remains behind it at y=30, producing a
        // stepped palace elevation instead of one uninterrupted roof wall.
        fill(world, o, x1, 19, -61, x2, 19, -53, ROOF);
        fill(world, o, x1 + 1, 20, -60, x2 - 1, 20, -54, ROOF);
        fill(world, o, x1 + 2, 21, -59, x2 - 2, 21, -55, ROOF);
        fill(world, o, cx - 1, 22, -58, cx + 1, 22, -56, COPPER);
    }

    private static void buildRecessedForecourt(ServerWorld world, BlockPos o, int x1, int x2) {
        // Keep the court itself open: a shallow stone apron, three columns and a narrow entablature
        // are enough to frame the recess without recreating a solid façade.
        fill(world, o, x1, 0, -58, x2, 0, -54, Blocks.POLISHED_ANDESITE.getDefaultState());
        for (int x = x1 + 1; x <= x2 - 1; x += 4) {
            fill(world, o, x, 1, -55, x, 12, -55, ASHLAR);
            fill(world, o, x - 1, 12, -56, x + 1, 13, -54, TRIM);
        }
        fill(world, o, x1, 14, -56, x2, 15, -54, ASHLAR);
    }
}
