package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;

/**
 * Removes the prototype's rectangular foundation/plinth from the final world and replaces it with
 * the actual pavilion/galleries footprint.
 *
 * The first Palace implementation used a complete 87x115 underground plate and continuous lower
 * envelope. Even after facade decoration this made the building read as a single box. The authored
 * room grid already contains 5-block transverse gaps; this pass exposes those gaps to the exterior,
 * supports each pavilion independently, and keeps only the central/cross galleries that physically
 * connect them.
 *
 * This pass is intentionally destructive and runs after exterior massing. It only rewrites Y -3..3
 * inside the old prototype footprint, so room interiors and the rebuilt upper architecture remain
 * untouched.
 */
final class OurosGrandPalaceArticulatedFootprintPass {
    private static final BlockState FOUNDATION = Blocks.STONE.getDefaultState();
    private static final BlockState FOUNDATION_MID = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState FOUNDATION_TOP = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState PLINTH = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState WATER_TABLE = Blocks.POLISHED_DIORITE.getDefaultState();

    private static final List<OurosGrandPalaceBuildKit.Room> GROUND_ROOMS = List.of(
            OurosGrandPalace.CABINET,
            OurosGrandPalace.ANTECHAMBER,
            OurosGrandPalace.SALLA_TERRENA,
            OurosGrandPalace.BLOOMING_SALON,
            OurosGrandPalace.AUDIENCE_CHAMBER,
            OurosGrandPalace.HUNTING_SALON,
            OurosGrandPalace.LIBRARY,
            OurosGrandPalace.THEMIS_HALL,
            OurosGrandPalace.GEOGRAPHY_CABINET,
            OurosGrandPalace.PORCELAIN_HALL,
            OurosGrandPalace.MARBLE_SALON,
            OurosGrandPalace.GALLERY_OF_ART
    );

    private OurosGrandPalaceArticulatedFootprintPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        erasePrototypePlateAndLowEnvelope(world, o);
        supportGroundRoomPavilions(world, o);
        supportLongitudinalGalleries(world, o);
        supportTransverseGalleries(world, o);
        supportEntranceAndRearPavilions(world, o);
        supportCornerTowers(world, o);
        supportSideFacadeBays(world, o);
        rebuildVisibleWaterTable(world, o);
    }

    private static void erasePrototypePlateAndLowEnvelope(ServerWorld world, BlockPos o) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = OurosGrandPalace.MIN_X; x <= OurosGrandPalace.MAX_X; x++) {
            for (int z = OurosGrandPalace.MIN_Z; z <= OurosGrandPalace.MAX_Z; z++) {
                if (belongsToFinalFootprint(x, z)) continue;
                for (int y = -3; y <= 3; y++) {
                    world.setBlockState(o.add(x, y, z), air);
                }
            }
        }
    }

    private static boolean belongsToFinalFootprint(int x, int z) {
        // Twelve independent ground-floor room masses.
        for (OurosGrandPalaceBuildKit.Room room : GROUND_ROOMS) {
            if (inside(room.minX() - 1, room.maxX() + 1, room.minZ() - 1, room.maxZ() + 1, x, z)) {
                return true;
            }
        }

        // Longitudinal galleries beside the ceremonial spine. They are deliberately narrow so
        // the transverse gaps remain visible as deep notches from both long elevations.
        if (inside(-16, -12, -55, 55, x, z) || inside(12, 16, -55, 55, x, z)) return true;

        // Cross galleries stop at the room fronts instead of reaching the old rectangular perimeter.
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            if (inside(-39, 39, band[0], band[1], x, z)) return true;
        }

        // Rebuilt front corps and flanking pavilions.
        if (inside(-20, 20, -60, -52, x, z)) return true;
        if (inside(-43, -18, -58, -51, x, z) || inside(18, 43, -58, -51, x, z)) return true;

        // Rear state pavilion.
        if (inside(-27, 27, 52, 59, x, z)) return true;

        // Four tower bases.
        for (int tx : new int[]{-38, 38}) {
            for (int tz : new int[]{-52, 52}) {
                if (inside(tx - 7, tx + 7, tz - 7, tz + 7, x, z)) return true;
            }
        }

        // Side facade bays are isolated strips, preserving exterior voids at each transverse break.
        for (int side : new int[]{-1, 1}) {
            int x1 = side < 0 ? -44 : 40;
            int x2 = side < 0 ? -40 : 44;
            for (int centerZ : new int[]{-50, -30, -10, 10, 30, 50}) {
                if (inside(x1, x2, centerZ - 9, centerZ + 9, x, z)) return true;
            }
        }
        return false;
    }

    private static boolean inside(int minX, int maxX, int minZ, int maxZ, int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private static void supportGroundRoomPavilions(ServerWorld world, BlockPos o) {
        for (OurosGrandPalaceBuildKit.Room room : GROUND_ROOMS) {
            foundationRect(world, o, room.minX() - 1, room.minZ() - 1,
                    room.maxX() + 1, room.maxZ() + 1, false);
        }
    }

    private static void supportLongitudinalGalleries(ServerWorld world, BlockPos o) {
        foundationRect(world, o, -16, -55, -12, 55, true);
        foundationRect(world, o, 12, -55, 16, 55, true);
    }

    private static void supportTransverseGalleries(ServerWorld world, BlockPos o) {
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            foundationRect(world, o, -39, band[0], 39, band[1], true);
        }
    }

    private static void supportEntranceAndRearPavilions(ServerWorld world, BlockPos o) {
        foundationRect(world, o, -20, -60, 20, -52, true);
        foundationRect(world, o, -43, -58, -18, -51, true);
        foundationRect(world, o, 18, -58, 43, -51, true);
        foundationRect(world, o, -27, 52, 27, 59, true);
    }

    private static void supportCornerTowers(ServerWorld world, BlockPos o) {
        for (int tx : new int[]{-38, 38}) {
            for (int tz : new int[]{-52, 52}) {
                foundationRect(world, o, tx - 7, tz - 7, tx + 7, tz + 7, true);
            }
        }
    }

    private static void supportSideFacadeBays(ServerWorld world, BlockPos o) {
        for (int side : new int[]{-1, 1}) {
            int x1 = side < 0 ? -44 : 40;
            int x2 = side < 0 ? -40 : 44;
            for (int centerZ : new int[]{-50, -30, -10, 10, 30, 50}) {
                foundationRect(world, o, x1, centerZ - 9, x2, centerZ + 9, true);
            }
        }
    }

    private static void foundationRect(ServerWorld world, BlockPos o,
                                       int minX, int minZ, int maxX, int maxZ,
                                       boolean visiblePlinth) {
        fill(world, o, minX, -3, minZ, maxX, -3, maxZ, FOUNDATION);
        fill(world, o, minX, -2, minZ, maxX, -2, maxZ, FOUNDATION_MID);
        fill(world, o, minX, -1, minZ, maxX, -1, maxZ, FOUNDATION_TOP);
        if (visiblePlinth) {
            // Only the boundary receives heavy masonry. Interior Y=0 is left to authored floors.
            fill(world, o, minX, 0, minZ, maxX, 0, minZ, PLINTH);
            fill(world, o, minX, 0, maxZ, maxX, 0, maxZ, PLINTH);
            fill(world, o, minX, 0, minZ + 1, minX, 0, maxZ - 1, PLINTH);
            fill(world, o, maxX, 0, minZ + 1, maxX, 0, maxZ - 1, PLINTH);
        }
    }

    private static void rebuildVisibleWaterTable(ServerWorld world, BlockPos o) {
        // Short courses on the outward pavilion faces emphasize the stepped plan in low-angle views.
        for (OurosGrandPalaceBuildKit.Room room : GROUND_ROOMS) {
            int outwardX = room.centerX() < 0 ? room.minX() - 1 : room.centerX() > 0 ? room.maxX() + 1 : 0;
            if (outwardX != 0) {
                fill(world, o, outwardX, 1, room.minZ() + 2, outwardX, 1, room.maxZ() - 2, WATER_TABLE);
            }
        }
    }
}
