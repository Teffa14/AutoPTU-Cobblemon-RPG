package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Final authored support pass for the Grand Palace.
 *
 * The pass closes small visual/support gaps that are easy to miss when dense furniture and
 * ornament are composed room-by-room. It adds only visible Minecraft structure: furniture feet,
 * gallery pilasters and trellis brackets. The floating-component audit remains strict afterwards.
 */
final class OurosGrandPalaceStructuralCompletionPass {
    private OurosGrandPalaceStructuralCompletionPass() {}

    static void apply(ServerWorld world, BlockPos origin) {
        groundFurnitureFeet(world, origin, 2, 0);
        groundFurnitureFeet(world, origin, 17, 15);
        supportGalleryRibs(world, origin);
        supportBloomingSalonTrellis(world, origin);
    }

    private static void groundFurnitureFeet(ServerWorld world, BlockPos o, int furnitureBaseY, int floorY) {
        BlockState foot = Blocks.DARK_OAK_FENCE.getDefaultState();
        int supportY = furnitureBaseY - 1;
        for (int x = OurosGrandPalace.MIN_X + 4; x <= OurosGrandPalace.MAX_X - 4; x++) {
            for (int z = OurosGrandPalace.MIN_Z + 4; z <= OurosGrandPalace.MAX_Z - 4; z++) {
                BlockState furniture = world.getBlockState(o.add(x, furnitureBaseY, z));
                if (furniture.isAir()) continue;
                if (!world.getBlockState(o.add(x, supportY, z)).isAir()) continue;
                if (world.getBlockState(o.add(x, floorY, z)).isAir()) continue;
                world.setBlockState(o.add(x, supportY, z), foot);
            }
        }
    }

    private static void supportGalleryRibs(ServerWorld world, BlockPos o) {
        BlockState pier = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState capital = Blocks.BAMBOO_MOSAIC.getDefaultState();
        for (int z : new int[]{-29, -1, 27}) {
            for (int x : new int[]{-16, -12, 12, 16}) {
                fill(world, o, x, 1, z, x, 11, z, pier);
                world.setBlockState(o.add(x, 11, z), capital);
            }
        }
    }

    private static void supportBloomingSalonTrellis(ServerWorld world, BlockPos o) {
        BlockState bracket = Blocks.BAMBOO_MOSAIC.getDefaultState();
        for (int z : new int[]{-21, -17, -13, -9}) {
            // West foliage plane sits two blocks in from the shell. These brackets make the
            // trellis visibly structural instead of six-block leaf columns suspended in air.
            world.setBlockState(o.add(-38, 5, z), bracket);
            world.setBlockState(o.add(-18, 5, z), bracket);
        }
    }
}
