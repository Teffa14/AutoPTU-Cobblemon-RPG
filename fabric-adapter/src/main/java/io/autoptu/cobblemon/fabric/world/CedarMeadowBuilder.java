package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Builds a small authored meadow used to validate living-world presentation in Minecraft. */
public final class CedarMeadowBuilder {
    private CedarMeadowBuilder() {}

    public static BuildResult build(ServerWorld world, BlockPos origin) {
        int baseY = origin.getY();

        for (int x = -14; x <= 14; x++) {
            for (int z = -12; z <= 12; z++) {
                BlockPos ground = origin.add(x, 0, z);
                world.setBlockState(ground, Blocks.GRASS_BLOCK.getDefaultState());
                world.setBlockState(ground.down(), Blocks.DIRT.getDefaultState());
                for (int y = 1; y <= 4; y++) {
                    world.setBlockState(ground.up(y), Blocks.AIR.getDefaultState());
                }
            }
        }

        for (int x = -14; x <= 14; x++) {
            world.setBlockState(origin.add(x, 0, 0), Blocks.DIRT_PATH.getDefaultState());
        }
        for (int z = -12; z <= 4; z++) {
            world.setBlockState(origin.add(-7, 0, z), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 5; x <= 10; x++) {
            for (int z = 5; z <= 9; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.MOSS_BLOCK.getDefaultState());
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = 6; z <= 9; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.WATER.getDefaultState());
            }
        }

        tree(world, origin.add(10, 1, -7));
        tree(world, origin.add(7, 1, -9));
        tree(world, origin.add(-11, 1, 8));

        for (int x = 5; x <= 10; x++) {
            world.setBlockState(origin.add(x, 1, 4), Blocks.OAK_FENCE.getDefaultState());
        }
        for (int z = 4; z <= 9; z++) {
            world.setBlockState(origin.add(4, 1, z), Blocks.OAK_FENCE.getDefaultState());
        }

        BlockPos overlook = origin.add(-7, 1, -8);
        world.setBlockState(overlook.down(), Blocks.COBBLESTONE.getDefaultState());
        world.setBlockState(overlook, Blocks.LANTERN.getDefaultState());
        world.setBlockState(origin.add(-5, 1, -8), Blocks.OAK_SLAB.getDefaultState());
        world.setBlockState(origin.add(-4, 1, -8), Blocks.OAK_SLAB.getDefaultState());

        BlockPos shelter = origin.add(8, 1, 7);
        world.setBlockState(shelter.down(), Blocks.HAY_BLOCK.getDefaultState());

        return new BuildResult(origin, overlook, shelter, origin.add(0, 1, -3));
    }

    private static void tree(ServerWorld world, BlockPos root) {
        for (int y = 0; y < 4; y++) {
            world.setBlockState(root.up(y), Blocks.OAK_LOG.getDefaultState());
        }
        BlockPos crown = root.up(3);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) {
                    world.setBlockState(crown.add(x, 1, z), Blocks.OAK_LEAVES.getDefaultState());
                }
            }
        }
        world.setBlockState(crown.up(2), Blocks.OAK_LEAVES.getDefaultState());
    }

    public record BuildResult(BlockPos origin, BlockPos overlook, BlockPos shelter, BlockPos lookoutPerch) {}
}
