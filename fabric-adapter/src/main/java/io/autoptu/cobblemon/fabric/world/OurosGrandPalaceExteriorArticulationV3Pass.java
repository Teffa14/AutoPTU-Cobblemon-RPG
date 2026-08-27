package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.stair;

/**
 * V3 silhouette pass.
 *
 * The palace needs to read as a collection of connected architectural bodies from plan view and
 * from eye level. This pass gives each side pavilion a different projection, opens the transverse
 * breaks as deep exterior notches, adds usable two-storey loggias, pushes the entrance portico into
 * the court and gives the rear elevation a garden gallery. The result cannot collapse back into a
 * single rectangular perimeter without deleting visible circulation and supported exterior space.
 */
final class OurosGrandPalaceExteriorArticulationV3Pass {
    private static final BlockState FOUNDATION = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState PLINTH = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState TIMBER = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState DARK = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_SLAB = Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState();
    private static final BlockState ROOF_STAIR = Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState();
    private static final BlockState ROOF_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();

    private OurosGrandPalaceExteriorArticulationV3Pass() {}

    static void apply(ServerWorld world, BlockPos o) {
        carveDeepTransverseNotches(world, o);

        // Four room rows receive intentionally different projection depths.
        buildSideLoggia(world, o, -1, -53, -31, 50);
        buildSideLoggia(world, o,  1, -53, -31, 50);
        buildSideLoggia(world, o, -1, -25,  -3, 46);
        buildSideLoggia(world, o,  1, -25,  -3, 46);
        buildSideLoggia(world, o, -1,   3,  25, 52);
        buildSideLoggia(world, o,  1,   3,  25, 52);
        buildSideLoggia(world, o, -1,  31,  53, 48);
        buildSideLoggia(world, o,  1,  31,  53, 48);

        buildCentralCourtPortico(world, o);
        buildRearGardenGallery(world, o);
        buildNotchPortals(world, o);
    }

    private static void carveDeepTransverseNotches(ServerWorld world, BlockPos o) {
        // The old side elevation crossed these three circulation bands with an almost continuous
        // facade. V3 leaves the bridges inside the palace but opens the outer eleven blocks to sky.
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            clear(world, o, -54, 2, band[0], -40, 36, band[1]);
            clear(world, o, 40, 2, band[0], 54, 36, band[1]);
        }
    }

    private static void buildSideLoggia(ServerWorld world, BlockPos o,
                                        int side, int z1, int z2, int projection) {
        int wallX = side < 0 ? -43 : 43;
        int outerX = side < 0 ? -projection : projection;
        int minX = Math.min(wallX, outerX);
        int maxX = Math.max(wallX, outerX);

        // Grounded terrace. The open loggia is usable exterior circulation rather than empty enclosed
        // volume, and its changing depth is legible in the exact top view.
        fill(world, o, minX, -3, z1, maxX, -3, z2, FOUNDATION);
        fill(world, o, minX, -2, z1, maxX, -1, z2, PLINTH);
        fill(world, o, minX, 0, z1, maxX, 1, z2, Blocks.POLISHED_ANDESITE.getDefaultState());

        // Outer colonnade and strong return piers tie both storeys into the terrace.
        for (int z = z1 + 2; z <= z2 - 2; z += 5) {
            column(world, o, outerX, 1, z, 29);
        }
        for (int x = minX; x <= maxX; x += 4) {
            column(world, o, x, 1, z1, 29);
            column(world, o, x, 1, z2, 29);
        }

        // Two exterior galleries. Open rails and timber ceilings give the facade human scale.
        fill(world, o, minX, 15, z1 + 1, maxX, 15, z2 - 1, DARK);
        int railX = side < 0 ? minX : maxX;
        for (int z = z1 + 1; z <= z2 - 1; z += 2) {
            fill(world, o, railX, 16, z, railX, 17, z, Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        fill(world, o, railX, 18, z1 + 1, railX, 18, z2 - 1, Blocks.DARK_OAK_FENCE.getDefaultState());

        // Repeated beams terminate on actual columns, never in mid-air.
        for (int z = z1 + 2; z <= z2 - 2; z += 5) {
            fill(world, o, minX, 13, z, maxX, 13, z, TIMBER);
            fill(world, o, minX, 28, z, maxX, 28, z, TIMBER);
        }

        buildLoggiaCanopy(world, o, minX, maxX, z1, z2, side);

        // Short water-table courses accent the different projection depths.
        fill(world, o, railX, 3, z1 + 2, railX, 4, z2 - 2, ASHLAR);
    }

    private static void column(ServerWorld world, BlockPos o, int x, int y, int z, int topY) {
        fill(world, o, x - 1, y, z - 1, x + 1, y + 1, z + 1, PLINTH);
        fill(world, o, x, y + 2, z, x, topY - 2, z, ASHLAR);
        fill(world, o, x - 1, topY - 1, z - 1, x + 1, topY, z + 1, COPPER);
    }

    private static void buildLoggiaCanopy(ServerWorld world, BlockPos o,
                                          int minX, int maxX, int z1, int z2, int side) {
        // Supported shallow roof with a raised longitudinal ridge. The changing X span between rows
        // creates a stepped roof silhouette instead of one continuous slab.
        fill(world, o, minX, 29, z1, maxX, 29, z2, Blocks.DEEPSLATE_TILES.getDefaultState());
        fill(world, o, minX + 1, 30, z1 + 1, maxX - 1, 30, z2 - 1, ROOF_SLAB);

        int outerX = side < 0 ? minX : maxX;
        Direction inward = side < 0 ? Direction.EAST : Direction.WEST;
        for (int z = z1; z <= z2; z++) {
            world.setBlockState(o.add(outerX, 30, z), stair(ROOF_STAIR, inward));
        }
        int ridgeX = side < 0 ? Math.min(maxX - 1, minX + 2) : Math.max(minX + 1, maxX - 2);
        fill(world, o, ridgeX, 31, z1 + 2, ridgeX, 31, z2 - 2, COPPER_SLAB);
    }

    private static void buildCentralCourtPortico(ServerWorld world, BlockPos o) {
        // Deep entrance projection: the player passes through a freestanding order before reaching
        // the facade. It occupies the previously empty middle of the cour d'honneur.
        fill(world, o, -14, -3, -64, 14, -3, -55, FOUNDATION);
        fill(world, o, -14, -2, -64, 14, 0, -55, PLINTH);
        fill(world, o, -12, 1, -63, 12, 1, -56, Blocks.POLISHED_DIORITE.getDefaultState());

        for (int x : new int[]{-10, -5, 5, 10}) {
            column(world, o, x, 1, -61, 20);
        }
        for (int x : new int[]{-12, 12}) {
            fill(world, o, x - 1, 1, -63, x + 1, 20, -56, WALL);
            fill(world, o, x - 2, 18, -64, x + 2, 20, -55, COPPER);
        }

        // Entablature and stepped pediment.
        fill(world, o, -13, 20, -63, 13, 22, -56, ASHLAR);
        fill(world, o, -14, 22, -64, 14, 23, -55, COPPER);
        for (int layer = 0; layer < 6; layer++) {
            int half = 12 - layer * 2;
            if (half < 2) break;
            fill(world, o, -half, 24 + layer, -62, half, 24 + layer, -57, WALL);
        }
        fill(world, o, -2, 26, -56, 2, 29, -56, Blocks.COPPER_GRATE.getDefaultState());

        // Broad, shallow arrival steps into the portico.
        for (int step = 0; step < 4; step++) {
            fill(world, o, -15 + step, step, -66 + step,
                    15 - step, step, -64 + step, Blocks.POLISHED_DIORITE.getDefaultState());
        }
    }

    private static void buildRearGardenGallery(ServerWorld world, BlockPos o) {
        // A lower rear gallery breaks the symmetry and gives the state rooms a real destination.
        fill(world, o, -24, -3, 55, 24, -3, 64, FOUNDATION);
        fill(world, o, -24, -2, 55, 24, 0, 64, PLINTH);
        fill(world, o, -23, 1, 56, 23, 1, 63, Blocks.POLISHED_ANDESITE.getDefaultState());

        for (int x = -21; x <= 21; x += 6) {
            column(world, o, x, 1, 63, 16);
        }
        for (int x = -21; x <= 21; x += 6) {
            fill(world, o, x, 15, 56, x, 15, 63, TIMBER);
        }
        fill(world, o, -23, 16, 56, 23, 16, 63, Blocks.DARK_OAK_SLAB.getDefaultState());

        // Central glazed garden lantern supported by the gallery roof.
        fill(world, o, -8, 17, 58, 8, 17, 63, COPPER);
        for (int x : new int[]{-8, 8}) {
            fill(world, o, x, 18, 58, x, 24, 63, ASHLAR);
            fill(world, o, x, 19, 59, x, 23, 62, Blocks.GLASS_PANE.getDefaultState());
        }
        for (int z : new int[]{58, 63}) {
            fill(world, o, -8, 18, z, 8, 24, z, ASHLAR);
            fill(world, o, -6, 19, z, 6, 23, z, Blocks.GLASS_PANE.getDefaultState());
        }
        fill(world, o, -9, 25, 57, 9, 25, 64, Blocks.DEEPSLATE_TILES.getDefaultState());
        fill(world, o, -7, 26, 59, 7, 26, 62, COPPER_SLAB);
    }

    private static void buildNotchPortals(ServerWorld world, BlockPos o) {
        // The three side cuts expose the transverse bridges. Each gets a framed portal at the room
        // edge so the void reads as intentional courtyard architecture rather than missing chunks.
        for (int z : new int[]{-28, 0, 28}) {
            framedSidePortal(world, o, -39, z, Direction.WEST);
            framedSidePortal(world, o, 39, z, Direction.EAST);
        }
    }

    private static void framedSidePortal(ServerWorld world, BlockPos o, int x, int z, Direction outward) {
        int dx = outward == Direction.WEST ? -1 : 1;
        fill(world, o, x, 1, z - 3, x + dx, 12, z - 2, ASHLAR);
        fill(world, o, x, 1, z + 2, x + dx, 12, z + 3, ASHLAR);
        fill(world, o, x, 11, z - 3, x + dx, 13, z + 3, COPPER);
        fill(world, o, x, 13, z - 1, x + dx, 15, z + 1, WALL);
    }
}
