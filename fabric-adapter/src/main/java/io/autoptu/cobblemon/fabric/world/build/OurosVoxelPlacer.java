package io.autoptu.cobblemon.fabric.world.build;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;

/** Projects pure Ouros voxel geometry into a live server world. */
public final class OurosVoxelPlacer {
    private OurosVoxelPlacer() {}

    public static void place(
            ServerWorld world,
            BlockPos origin,
            Collection<OurosVoxelGeometry.Voxel> voxels,
            BlockState state
    ) {
        if (world == null || origin == null || voxels == null || state == null) {
            throw new IllegalArgumentException("world, origin, voxels and state are required");
        }
        for (OurosVoxelGeometry.Voxel voxel : voxels) {
            world.setBlockState(origin.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }
}
