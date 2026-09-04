package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Lower, hierarchy-driven Palace V4 roofscape.
 *
 * The original V4 proof used tall stepped pyramids on every pavilion. It guaranteed independent
 * roofs, but from the review camera the roof mass overwhelmed the facades and every room competed
 * for skyline priority. This pass keeps the independent-roof contract while using a true mansard
 * proportion: a short steep lower slope, a shallow upper roof and only two central cupolas.
 */
final class OurosGrandPalaceV4AuthoredRoofPass {
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState ROOF_SLAB = Blocks.POLISHED_DEEPSLATE_SLAB.getDefaultState();
    private static final BlockState RIDGE = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_SLAB = Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
    private static final BlockState FRAME = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();

    private OurosGrandPalaceV4AuthoredRoofPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (Room room : groundSideRooms()) buildSideMansard(world, o, room);
        for (Room room : ceremonialRooms()) buildCentralMansard(world, o, room);

        buildCupola(world, o, AUDIENCE_CHAMBER, 35);
        buildCupola(world, o, THEMIS_HALL, 36);
        buildSelectiveDormers(world, o);
    }

    private static void buildSideMansard(ServerWorld world, BlockPos o, Room room) {
        int layers = Math.abs(room.centerZ()) > 30 ? 4 : 3;
        buildLowMansard(world, o, room, layers, true);
    }

    private static void buildCentralMansard(ServerWorld world, BlockPos o, Room room) {
        int layers = Math.abs(room.centerZ()) > 30 ? 3 : 4;
        buildLowMansard(world, o, room, layers, false);
    }

    private static void buildLowMansard(ServerWorld world, BlockPos o, Room room, int layers, boolean sideWing) {
        int x1 = room.minX() - 2;
        int x2 = room.maxX() + 2;
        int z1 = room.minZ() - 2;
        int z2 = room.maxZ() + 2;
        int eaveY = 30;

        // A short, steep lower slope. One ring per level is enough because the y=29 eave support
        // already joins the room shell to this roof perimeter.
        for (int layer = 0; layer < layers; layer++) {
            int ax1 = x1 + layer;
            int ax2 = x2 - layer;
            int az1 = z1 + layer;
            int az2 = z2 - layer;
            BlockState state = layer == 0 ? RIDGE : ROOF;
            ring(world, o, ax1, ax2, az1, az2, eaveY + layer, state);
        }

        // Mansards should not continue into mountains. A shallow slab field closes the upper roof,
        // then one narrow directional ridge gives each pavilion a readable long axis.
        int capX1 = x1 + layers;
        int capX2 = x2 - layers;
        int capZ1 = z1 + layers;
        int capZ2 = z2 - layers;
        int capY = eaveY + layers;
        if (capX1 <= capX2 && capZ1 <= capZ2) {
            fill(world, o, capX1, capY, capZ1, capX2, capY, capZ2, ROOF_SLAB);
            int ridgeX = room.centerX();
            int rz1 = capZ1 + 2;
            int rz2 = capZ2 - 2;
            if (rz1 <= rz2) {
                fill(world, o, ridgeX, capY + 1, rz1, ridgeX, capY + 1, rz2,
                        sideWing ? COPPER_SLAB : RIDGE);
            }
        }
    }

    private static void ring(ServerWorld world, BlockPos o,
                             int x1, int x2, int z1, int z2, int y, BlockState state) {
        fill(world, o, x1, y, z1, x2, y, z1, state);
        fill(world, o, x1, y, z2, x2, y, z2, state);
        fill(world, o, x1, y, z1, x1, y, z2, state);
        fill(world, o, x2, y, z1, x2, y, z2, state);
    }

    /** Small glazed cupolas mark the central audience sequence; no other pavilion gets a tower box. */
    private static void buildCupola(ServerWorld world, BlockPos o, Room sourceRoom, int baseY) {
        Room room = physical(sourceRoom);
        int cx = room.centerX();
        int cz = room.centerZ();

        fill(world, o, cx - 4, baseY, cz - 4, cx + 4, baseY, cz + 4, RIDGE);
        for (int y = baseY + 1; y <= baseY + 4; y++) {
            for (int x : new int[]{cx - 4, cx + 4}) {
                fill(world, o, x, y, cz - 3, x, y, cz + 3, GLASS);
            }
            for (int z : new int[]{cz - 4, cz + 4}) {
                fill(world, o, cx - 3, y, z, cx + 3, y, z, GLASS);
            }
        }
        for (int x : new int[]{cx - 4, cx, cx + 4}) {
            fill(world, o, x, baseY + 1, cz - 4, x, baseY + 4, cz + 4, FRAME);
        }

        fill(world, o, cx - 5, baseY + 5, cz - 5, cx + 5, baseY + 5, cz + 5, COPPER_SLAB);
        fill(world, o, cx - 3, baseY + 6, cz - 3, cx + 3, baseY + 6, cz + 3, COPPER_SLAB);
        fill(world, o, cx - 1, baseY + 7, cz - 1, cx + 1, baseY + 7, cz + 1, COPPER);
        world.setBlockState(o.add(cx, baseY + 8, cz), Blocks.LIGHTNING_ROD.getDefaultState());
    }

    /** One small outward dormer on the two inner wing rows is enough to break the long roof surface. */
    private static void buildSelectiveDormers(ServerWorld world, BlockPos o) {
        for (Room room : groundSideRooms()) {
            if (Math.abs(room.centerZ()) >= 30) continue;
            boolean west = room.centerX() < 0;
            int x = west ? room.minX() - 1 : room.maxX() + 1;
            int z = room.centerZ();
            int y = 32;

            fill(world, o, x, y, z - 1, x, y + 2, z + 1, RIDGE);
            int outerX = west ? x - 1 : x + 1;
            fill(world, o, outerX, y + 1, z, outerX, y + 2, z, GLASS);
            fill(world, o, x - 1, y + 3, z - 2, x + 1, y + 3, z + 2, COPPER_SLAB);
        }
    }
}
