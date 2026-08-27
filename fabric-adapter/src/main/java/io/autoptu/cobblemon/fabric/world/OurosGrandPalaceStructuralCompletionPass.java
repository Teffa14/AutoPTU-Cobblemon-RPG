package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Final authored support pass for the Grand Palace.
 *
 * The pass first replaces the prototype exterior shell with the authored Palace V2 architecture,
 * keeps that architecture honest to the current exact-viewer envelope, then closes small visual/
 * support gaps that are easy to miss when dense furniture and ornament are composed room-by-room.
 * The floating-component audit remains strict afterwards.
 */
final class OurosGrandPalaceStructuralCompletionPass {
    private OurosGrandPalaceStructuralCompletionPass() {}

    static void apply(ServerWorld world, BlockPos origin) {
        OurosGrandPalaceExteriorRebuildPass.apply(world, origin);
        OurosGrandPalaceEnvelopeSafetyPass.apply(world, origin);
        groundFurnitureFeet(world, origin, 2, 0);
        groundFurnitureFeet(world, origin, 17, 15);
        supportGalleryRibs(world, origin);
        supportBloomingSalonTrellis(world, origin);
        supportHarpsichordBench(world, origin);
        supportHuntingTrophyAntlers(world, origin);
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
            world.setBlockState(o.add(-38, 5, z), bracket);
            world.setBlockState(o.add(-18, 5, z), bracket);
        }
    }

    private static void supportHarpsichordBench(ServerWorld world, BlockPos o) {
        OurosGrandPalaceBuildKit.Room room = OurosGrandPalace.MUSIC_CHAMBER;
        int x = room.centerX();
        int z = room.minZ() + 3;
        int y = room.floorY() + 3;
        world.setBlockState(o.add(x, y, z), Blocks.DARK_OAK_FENCE.getDefaultState());
    }

    private static void supportHuntingTrophyAntlers(ServerWorld world, BlockPos o) {
        OurosGrandPalaceBuildKit.Room room = OurosGrandPalace.HUNTING_SALON;
        int x = room.maxX() - 2;
        BlockState joinery = Blocks.SPRUCE_FENCE.getDefaultState();
        for (int centerZ = room.minZ() + 4; centerZ <= room.maxZ() - 4; centerZ += 6) {
            world.setBlockState(o.add(x, 10, centerZ - 2), joinery);
            world.setBlockState(o.add(x, 10, centerZ + 2), joinery);
        }
    }
}
