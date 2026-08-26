package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.world.build.OurosVoxelGeometry;
import io.autoptu.cobblemon.fabric.world.build.OurosVoxelPlacer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * First production consumer of the Ouros authored voxel toolkit.
 *
 * This pass targets the weakest visual reads in the current Gym blockout: the flat central tree,
 * the rectangular battle floor and the stepped conservatory roof. All changes remain world
 * presentation. No block shape or material creates PTU terrain effects.
 */
public final class MeridianCanopyGymAuthoredGeometryPass {
    private static final BlockState TRUNK = Blocks.DARK_OAK_WOOD.getDefaultState();
    private static final BlockState ROOT = Blocks.MANGROVE_ROOTS.getDefaultState();
    private static final BlockState LEAVES = Blocks.AZALEA_LEAVES.getDefaultState();
    private static final BlockState FLOWERS = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState();
    private static final BlockState ARENA_FIELD = Blocks.PACKED_MUD.getDefaultState();
    private static final BlockState ARENA_BORDER = Blocks.SMOOTH_SANDSTONE.getDefaultState();
    private static final BlockState ARENA_MARK = Blocks.MOSS_BLOCK.getDefaultState();
    private static final BlockState SEAT = Blocks.STONE_BRICK_SLAB.getDefaultState();
    private static final BlockState RAIL = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState RIB = Blocks.OXIDIZED_COPPER.getDefaultState();

    private MeridianCanopyGymAuthoredGeometryPass() {}

    public static void apply(ServerWorld world, BlockPos origin) {
        rebuildCentralSpecimenTree(world, origin);
        reshapeLeaderArena(world, origin);
        addConservatoryArchRibs(world, origin);
    }

    private static void rebuildCentralSpecimenTree(ServerWorld world, BlockPos o) {
        // Remove only the legacy flat foliage. Preserve glass, bridges, beams and every other block.
        for (int x = -8; x <= 8; x++) {
            for (int y = 9; y <= 18; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = o.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.AZALEA_LEAVES) || state.isOf(Blocks.FLOWERING_AZALEA_LEAVES)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        // A broad taper gives the specimen an old institutional-garden trunk rather than a pole.
        OurosVoxelPlacer.place(
                world,
                o.add(0, 1, 0),
                OurosVoxelGeometry.taperedColumn(2, 2, 1, 1, 11),
                TRUNK
        );

        // Buttress roots remain inside the planted island and make the trunk meet the ground.
        for (OurosVoxelGeometry.Voxel end : new OurosVoxelGeometry.Voxel[]{
                new OurosVoxelGeometry.Voxel(-4, 0, -2),
                new OurosVoxelGeometry.Voxel(4, 0, -1),
                new OurosVoxelGeometry.Voxel(-3, 0, 4),
                new OurosVoxelGeometry.Voxel(3, 0, 4)
        }) {
            placeReplaceable(
                    world,
                    o.add(0, 1, 0),
                    OurosVoxelGeometry.line3d(new OurosVoxelGeometry.Voxel(0, 1, 0), end),
                    ROOT,
                    false
            );
        }

        // Asymmetric crown masses create a silhouette that can be read from several atrium levels.
        int[][] crowns = {
                {-6, 13, -3, 4, 2, 3},
                {6, 14, -2, 4, 2, 3},
                {-4, 15, 5, 3, 2, 4},
                {5, 13, 5, 4, 2, 3},
                {0, 17, -4, 4, 2, 3},
                {0, 16, 3, 5, 2, 4}
        };
        for (int[] crown : crowns) {
            placeMixedFoliage(world, o.add(crown[0], crown[1], crown[2]), crown[3], crown[4], crown[5]);
        }

        // Branches are laid after foliage so their structure remains legible through the crown.
        BlockPos branchOrigin = o.add(0, 10, 0);
        for (OurosVoxelGeometry.Voxel end : new OurosVoxelGeometry.Voxel[]{
                new OurosVoxelGeometry.Voxel(-6, 3, -3),
                new OurosVoxelGeometry.Voxel(6, 4, -2),
                new OurosVoxelGeometry.Voxel(-4, 5, 5),
                new OurosVoxelGeometry.Voxel(5, 3, 5),
                new OurosVoxelGeometry.Voxel(0, 7, -4),
                new OurosVoxelGeometry.Voxel(0, 6, 3)
        }) {
            placeReplaceable(
                    world,
                    branchOrigin,
                    OurosVoxelGeometry.line3d(new OurosVoxelGeometry.Voxel(0, 0, 0), end),
                    TRUNK,
                    true
            );
        }
    }

    private static void reshapeLeaderArena(ServerWorld world, BlockPos o) {
        BlockPos center = o.add(0, 0, 22);

        // A clearly bounded oval battle floor replaces the nested rectangular carpet pattern.
        OurosVoxelPlacer.place(world, center, OurosVoxelGeometry.filledEllipse(16, 6, 0), ARENA_FIELD);
        OurosVoxelPlacer.place(world, center, OurosVoxelGeometry.ellipseRing(18, 8, 16, 6, 0), ARENA_BORDER);
        OurosVoxelPlacer.place(world, center, OurosVoxelGeometry.ellipseRing(4, 3, 3, 2, 0), ARENA_MARK);

        // A center axis helps the hall read immediately as a formal battle institution.
        for (int x = -11; x <= 11; x++) {
            world.setBlockState(center.add(x, 0, 0), ARENA_BORDER);
        }
        world.setBlockState(center, ARENA_MARK);

        // Curved side terraces leave the entry and leader ends clear while creating a spectator bowl.
        for (int tier = 0; tier < 3; tier++) {
            int outerX = 20 - tier;
            int outerZ = 8 - tier;
            int innerX = 18 - tier;
            int innerZ = 6 - tier;
            Set<OurosVoxelGeometry.Voxel> ring = OurosVoxelGeometry.ellipseRing(
                    outerX, outerZ, innerX, innerZ, 1 + tier
            );
            for (OurosVoxelGeometry.Voxel voxel : ring) {
                if (Math.abs(voxel.x()) >= 14) {
                    world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), SEAT);
                }
            }
        }

        Set<OurosVoxelGeometry.Voxel> rail = OurosVoxelGeometry.ellipseRing(20, 8, 19, 7, 4);
        for (OurosVoxelGeometry.Voxel voxel : rail) {
            if (Math.abs(voxel.x()) >= 17) {
                BlockPos pos = center.add(voxel.x(), voxel.y(), voxel.z());
                if (world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, RAIL);
                }
            }
        }
    }

    private static void addConservatoryArchRibs(ServerWorld world, BlockPos o) {
        // Continuous parabolic ribs replace the impression of a roof assembled only from flat steps.
        for (int z : new int[]{-10, -5, 0, 5, 10}) {
            Set<OurosVoxelGeometry.Voxel> arch = OurosVoxelGeometry.parabolicArchX(15, 5, 1, z);
            placeReplaceable(world, o.add(0, 15, 0), arch, RIB, true);
        }
    }

    private static void placeMixedFoliage(
            ServerWorld world,
            BlockPos center,
            int radiusX,
            int radiusY,
            int radiusZ
    ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipsoid(radiusX, radiusY, radiusZ)) {
            BlockPos pos = center.add(voxel.x(), voxel.y(), voxel.z());
            BlockState existing = world.getBlockState(pos);
            if (!existing.isAir()
                    && !existing.isOf(Blocks.AZALEA_LEAVES)
                    && !existing.isOf(Blocks.FLOWERING_AZALEA_LEAVES)) {
                continue;
            }
            int pattern = Math.floorMod(voxel.x() * 31 + voxel.y() * 17 + voxel.z() * 13, 11);
            world.setBlockState(pos, pattern == 0 || pattern == 7 ? FLOWERS : LEAVES);
        }
    }

    private static void placeReplaceable(
            ServerWorld world,
            BlockPos origin,
            Set<OurosVoxelGeometry.Voxel> voxels,
            BlockState state,
            boolean allowLeaves
    ) {
        for (OurosVoxelGeometry.Voxel voxel : voxels) {
            BlockPos pos = origin.add(voxel.x(), voxel.y(), voxel.z());
            BlockState existing = world.getBlockState(pos);
            boolean replaceable = existing.isAir()
                    || existing.isOf(Blocks.DARK_OAK_LOG)
                    || existing.isOf(Blocks.DARK_OAK_WOOD)
                    || existing.isOf(Blocks.MANGROVE_ROOTS)
                    || (allowLeaves && (existing.isOf(Blocks.AZALEA_LEAVES)
                    || existing.isOf(Blocks.FLOWERING_AZALEA_LEAVES)));
            if (replaceable) {
                world.setBlockState(pos, state);
            }
        }
    }
}
