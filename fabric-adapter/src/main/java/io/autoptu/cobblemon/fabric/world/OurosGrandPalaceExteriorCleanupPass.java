package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/** Removes prototype leftovers and relocates exterior ornaments onto real supporting roof masses. */
final class OurosGrandPalaceExteriorCleanupPass {
    private OurosGrandPalaceExteriorCleanupPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        // These two one-block fragments were left by the prototype roof below the V2 roof-clear Y.
        clear(world, o, -14, 30, -48, -14, 30, -48);
        clear(world, o, 14, 30, -48, 14, 30, -48);

        // The first chimney study occupied the gap between the rear central and side roof masses.
        // Remove those complete components rather than bridging them with invisible supports.
        clear(world, o, -19, 37, 41, -15, 46, 45);
        clear(world, o, 15, 37, 41, 19, 46, 45);

        // Rebuild the pair on the actual rear side-pavilion slopes. At x=+-31,z=43 each stack
        // intersects a backed hipped-roof course, so the masonry reads and audits as a true flue.
        chimney(world, o, -31, 43);
        chimney(world, o, 31, 43);
    }

    private static void chimney(ServerWorld world, BlockPos o, int x, int z) {
        fill(world, o, x - 1, 36, z - 1, x + 1, 44, z + 1, Blocks.BRICKS.getDefaultState());
        fill(world, o, x - 2, 44, z - 2, x + 2, 45, z + 2, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        fill(world, o, x - 1, 46, z - 1, x + 1, 46, z + 1, Blocks.BRICK_WALL.getDefaultState());
    }
}
