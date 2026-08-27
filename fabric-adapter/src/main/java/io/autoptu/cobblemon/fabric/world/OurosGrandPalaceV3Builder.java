package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.litLantern;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.stair;

/**
 * Exterior-first reconstruction of the Ouros Grand Palace.
 *
 * V1/V2 started from a complete rectangular plate and envelope, then attempted to disguise that
 * mass with facade work. V3 reverses the dependency: a deliberately articulated footprint and the
 * exterior architectural composition are authored first. Circulation and the nineteen reference
 * rooms are then inserted into that architecture, followed by the dense reference-interior pass.
 *
 * The older OurosGrandPalace class remains only as the room-program coordinate authority while V3
 * is under exact browser review. No legacy rectangular foundation, outer envelope, global mansard
 * or prototype facade method is invoked here.
 */
public final class OurosGrandPalaceV3Builder {
    private static final int CLEAR_MIN_X = -54;
    private static final int CLEAR_MAX_X = 54;
    private static final int CLEAR_MIN_Y = 1;
    private static final int CLEAR_MAX_Y = 72;
    private static final int CLEAR_MIN_Z = -72;
    private static final int CLEAR_MAX_Z = 66;

    private OurosGrandPalaceV3Builder() {}

    public static OurosGrandPalace.BuildResult build(ServerWorld world, BlockPos origin) {
        clearWorkingVolume(world, origin);

        // Architecture first. Nothing below depends on a rectangular prototype shell.
        OurosGrandPalaceArticulatedFootprintPass.apply(world, origin);
        OurosGrandPalaceExteriorRebuildPass.apply(world, origin);
        OurosGrandPalaceExteriorArticulationV3Pass.apply(world, origin);
        OurosGrandPalaceExteriorCleanupPass.apply(world, origin);
        OurosGrandPalaceRoofSupportPass.apply(world, origin);
        OurosGrandPalaceCourDHonneurPass.apply(world, origin);

        // Interior program is inserted only after the exterior massing is established.
        buildGroundCirculation(world, origin);
        buildUpperCirculation(world, origin);
        buildGrandStaircases(world, origin);
        OurosGrandPalaceCeremonialRooms.buildAll(world, origin);
        OurosGrandPalaceSalonRooms.buildAll(world, origin);
        OurosGrandPalaceUpperRooms.buildAll(world, origin);
        OurosGrandPalaceStructuralCompletionPass.finishInterior(world, origin);

        return new OurosGrandPalace.BuildResult(origin, 109, 77, 139, 19);
    }

    private static void clearWorkingVolume(ServerWorld world, BlockPos o) {
        // Preserve terrain/foundation levels outside the authored footprint. The footprint pass owns
        // Y -3..0; this clear removes only previous architecture above grade across the review area.
        clear(world, o, CLEAR_MIN_X, CLEAR_MIN_Y, CLEAR_MIN_Z,
                CLEAR_MAX_X, CLEAR_MAX_Y, CLEAR_MAX_Z);
    }

    private static void buildGroundCirculation(ServerWorld world, BlockPos o) {
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState light = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState accent = Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();

        // Two internal longitudinal galleries. V3 deliberately has no rectangular perimeter gallery.
        for (int x = -16; x <= -12; x++) {
            for (int z = -53; z <= 53; z++) {
                world.setBlockState(o.add(x, 0, z), Math.floorMod(z, 6) == 0 ? accent : dark);
            }
        }
        for (int x = 12; x <= 16; x++) {
            for (int z = -53; z <= 53; z++) {
                world.setBlockState(o.add(x, 0, z), Math.floorMod(z, 6) == 0 ? accent : dark);
            }
        }

        // Three transverse bridges connect the pavilion rows while keeping the exterior notches open.
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            for (int x = -39; x <= 39; x++) {
                for (int z = band[0]; z <= band[1]; z++) {
                    world.setBlockState(o.add(x, 0, z), Math.floorMod(x, 8) == 0 ? accent : light);
                }
            }
        }

        fill(world, o, -2, 1, -56, 2, 1, 56, Blocks.RED_CARPET.getDefaultState());

        for (int z = -50; z <= 50; z += 7) {
            fill(world, o, -16, 12, z, -12, 12, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            fill(world, o, 12, 12, z, 16, 12, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            world.setBlockState(o.add(-14, 11, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-14, 10, z), litLantern(true));
            world.setBlockState(o.add(14, 11, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(14, 10, z), litLantern(true));
        }
    }

    private static void buildUpperCirculation(ServerWorld world, BlockPos o) {
        BlockState floor = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState rail = Blocks.BAMBOO_FENCE.getDefaultState();

        fill(world, o, -16, 15, -53, -12, 15, 53, floor);
        fill(world, o, 12, 15, -53, 16, 15, 53, floor);
        for (int z = -52; z <= 52; z++) {
            world.setBlockState(o.add(-12, 16, z), rail);
            world.setBlockState(o.add(12, 16, z), rail);
        }
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            fill(world, o, -39, 15, band[0], 39, 15, band[1], Blocks.BAMBOO_MOSAIC.getDefaultState());
        }
    }

    private static void buildGrandStaircases(ServerWorld world, BlockPos o) {
        BlockState stairBlock = Blocks.POLISHED_DIORITE_STAIRS.getDefaultState();
        BlockState landing = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState rail = Blocks.BAMBOO_FENCE.getDefaultState();

        for (int side : new int[]{-1, 1}) {
            int x0 = side < 0 ? -16 : 12;
            for (int step = 0; step < 14; step++) {
                int y = 1 + step;
                int z = -18 + step;
                for (int dx = 0; dx < 5; dx++) {
                    int x = x0 + dx;
                    world.setBlockState(o.add(x, y, z), stair(stairBlock, Direction.SOUTH));
                    if (dx == 0 || dx == 4) {
                        world.setBlockState(o.add(x, y + 1, z), rail);
                    }
                }
            }
            fill(world, o, x0, 15, -4, x0 + 4, 15, 2, landing);
        }
    }
}
