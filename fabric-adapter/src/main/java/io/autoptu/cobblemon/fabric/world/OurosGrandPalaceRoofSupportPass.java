package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Gives every stepped Palace roof a real wall plate and continuous backing.
 *
 * A visually continuous Minecraft stair roof can otherwise contain 6-neighbour-disconnected rows
 * because each next course moves one block inward and one block upward. The exact structural audit
 * intentionally treats that as floating. These full-block backers tie every visible stair course to
 * the course below and also give the roofs the thickness expected by the Ouros build doctrine.
 */
final class OurosGrandPalaceRoofSupportPass {
    private static final BlockState PLATE = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState BACKING = Blocks.DEEPSLATE_TILES.getDefaultState();

    private OurosGrandPalaceRoofSupportPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        support(-39, -19, -53, -31, 31, 10, world, o);
        support(19, 39, -53, -31, 31, 10, world, o);
        support(-39, -19, -25, -3, 31, 9, world, o);
        support(19, 39, -25, -3, 31, 9, world, o);
        support(-39, -19, 3, 25, 31, 9, world, o);
        support(19, 39, 3, 25, 31, 9, world, o);
        support(-39, -19, 31, 53, 31, 10, world, o);
        support(19, 39, 31, 53, 31, 10, world, o);

        support(-13, 13, -29, -1, 31, 12, world, o);
        support(-13, 13, 1, 29, 31, 13, world, o);
        support(-13, 13, 29, 55, 31, 12, world, o);
        support(-19, 19, -55, -29, 31, 14, world, o);

        for (int x : new int[]{-38, 38}) {
            for (int z : new int[]{-52, 52}) {
                support(x - 6, x + 6, z - 6, z + 6, 33, 10, world, o);
            }
        }

        // Closed central glazed lantern roof. The finial at its ridge is then connected through this
        // backed roof into the clerestory walls and the main Themis Hall roof mass.
        support(-9, 9, 5, 23, 57, 8, world, o);
    }

    private static void support(int x1, int x2, int z1, int z2, int baseY, int maxRise,
                                ServerWorld world, BlockPos o) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int layers = Math.min(maxRise, Math.min((maxX - minX) / 2, (maxZ - minZ) / 2));

        perimeter(world, o, minX, maxX, minZ, maxZ, baseY - 1, PLATE);

        for (int layer = 1; layer <= layers; layer++) {
            int lx1 = minX + layer;
            int lx2 = maxX - layer;
            int lz1 = minZ + layer;
            int lz2 = maxZ - layer;
            perimeter(world, o, lx1, lx2, lz1, lz2, baseY + layer - 1, BACKING);
        }
    }

    private static void perimeter(ServerWorld world, BlockPos o,
                                  int x1, int x2, int z1, int z2, int y, BlockState state) {
        if (x1 > x2 || z1 > z2) return;
        fill(world, o, x1, y, z1, x2, y, z1, state);
        fill(world, o, x1, y, z2, x2, y, z2, state);
        if (z2 - z1 > 1) {
            fill(world, o, x1, y, z1 + 1, x1, y, z2 - 1, state);
            fill(world, o, x2, y, z1 + 1, x2, y, z2 - 1, state);
        }
    }
}
