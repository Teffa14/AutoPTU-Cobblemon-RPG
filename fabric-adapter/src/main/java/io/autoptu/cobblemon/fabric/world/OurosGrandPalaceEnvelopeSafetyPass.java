package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Transitional exact-viewer safety pass for the Palace exterior rebuild.
 *
 * OI-107 V2 deliberately removes the old flat box silhouette before the capture envelope itself is
 * expanded in the next vertical iteration. This pass guarantees the current exact-server exporter
 * cannot hide geometry above MAX_Y: it removes the temporary tall lantern study and caps the
 * lantern as a fully enclosed clerestory inside the current Y=48 envelope.
 */
final class OurosGrandPalaceEnvelopeSafetyPass {
    private OurosGrandPalaceEnvelopeSafetyPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        clear(world, o,
                OurosGrandPalace.MIN_X, OurosGrandPalace.MAX_Y + 1, OurosGrandPalace.MIN_Z,
                OurosGrandPalace.MAX_X, OurosGrandPalace.MAX_Y + 32, OurosGrandPalace.MAX_Z);

        // Re-cut only the central lantern crown. The exterior pass already anchors its lower walls
        // into the Themis roof mass at Y=43.
        clear(world, o, -10, 44, 4, 10, 48, 24);

        // Two-storey-looking glazed clerestory compressed into the current envelope.
        fill(world, o, -8, 43, 6, 8, 43, 22, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        for (int x : new int[]{-8, 8}) {
            fill(world, o, x, 44, 6, x, 46, 22, Blocks.POLISHED_DIORITE.getDefaultState());
            fill(world, o, x, 44, 8, x, 46, 20, Blocks.GLASS_PANE.getDefaultState());
            for (int z = 8; z <= 20; z += 4) {
                fill(world, o, x, 44, z, x, 46, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            }
        }
        for (int z : new int[]{6, 22}) {
            fill(world, o, -8, 44, z, 8, 46, z, Blocks.POLISHED_DIORITE.getDefaultState());
            fill(world, o, -6, 44, z, 6, 46, z, Blocks.GLASS_PANE.getDefaultState());
            for (int x = -6; x <= 6; x += 4) {
                fill(world, o, x, 44, z, x, 46, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            }
        }

        // Closed, stepped copper cap. No open cupola and no invisible geometry above the viewer.
        fill(world, o, -9, 47, 5, 9, 47, 23, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
        fill(world, o, -7, 48, 7, 7, 48, 21, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
    }
}
