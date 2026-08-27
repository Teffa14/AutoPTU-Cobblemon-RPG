package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/** Independent, face-connected mansard bodies for the courtyard-based V4 massing. */
final class OurosGrandPalaceV4RoofPass {
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState RIDGE = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState FRAME = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();

    private OurosGrandPalaceV4RoofPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (Room room : groundSideRooms()) mansard(world, o, room, false);
        for (Room room : ceremonialRooms()) mansard(world, o, room, true);
        buildCentralLantern(world, o, ceremonialRooms().get(1), 42);
        buildCentralLantern(world, o, ceremonialRooms().get(2), 46);
        buildDormers(world, o);
        buildCourtyardGutters(world, o);
    }

    private static void mansard(ServerWorld world, BlockPos o, Room room, boolean ceremonial) {
        int eaveY = 30;
        int x1 = room.minX() - 2;
        int x2 = room.maxX() + 2;
        int z1 = room.minZ() - 2;
        int z2 = room.maxZ() + 2;
        int layers = roofLayers(room, ceremonial);

        // Two-block-thick stepped rings overlap vertically with the next inset ring. The profile is
        // intentionally allowed to vary by pavilion row so the skyline does not read as cloned boxes.
        for (int layer = 0; layer < layers; layer++) {
            int ax1 = x1 + layer;
            int ax2 = x2 - layer;
            int az1 = z1 + layer;
            int az2 = z2 - layer;
            if (ax1 > ax2 || az1 > az2) break;
            BlockState state = layer == 0 ? RIDGE : ROOF;
            thickRing(world, o, ax1, ax2, az1, az2, eaveY + layer, state);
        }

        // The previous V4 ended every intermediate mansard in one broad, flat copper rectangle.
        // Continue the roof upward through several smaller rings so each pavilion resolves to a real
        // crown/ridge rather than a square tray visible from the browser's elevated camera.
        int capX1 = x1 + layers;
        int capX2 = x2 - layers;
        int capZ1 = z1 + layers;
        int capZ2 = z2 - layers;
        int capY = eaveY + layers;
        int extra = ceremonial ? 2 : 3;
        int built = 0;
        for (int i = 0; i < extra; i++) {
            int ax1 = capX1 + i;
            int ax2 = capX2 - i;
            int az1 = capZ1 + i;
            int az2 = capZ2 - i;
            if (ax1 > ax2 || az1 > az2) break;
            BlockState state = i == extra - 1 ? COPPER : ROOF;
            singleRing(world, o, ax1, ax2, az1, az2, capY + i, state);
            built = i + 1;
        }

        int crownY = capY + built;
        int crownX1 = capX1 + built;
        int crownX2 = capX2 - built;
        int crownZ1 = capZ1 + built;
        int crownZ2 = capZ2 - built;
        if (crownX1 <= crownX2 && crownZ1 <= crownZ2) {
            // A narrow cross ridge is visibly directional and avoids another flat square cap.
            fill(world, o, crownX1, crownY, room.centerZ(), crownX2, crownY, room.centerZ(), COPPER);
            fill(world, o, room.centerX(), crownY, crownZ1, room.centerX(), crownY, crownZ2, COPPER);
        } else {
            world.setBlockState(o.add(room.centerX(), crownY, room.centerZ()), COPPER);
        }
        world.setBlockState(o.add(room.centerX(), crownY + 1, room.centerZ()),
                Blocks.LIGHTNING_ROD.getDefaultState());
    }

    private static void thickRing(ServerWorld world, BlockPos o,
                                  int x1, int x2, int z1, int z2, int y, BlockState state) {
        fill(world, o, x1, y, z1, x2, y, Math.min(z1 + 1, z2), state);
        fill(world, o, x1, y, Math.max(z2 - 1, z1), x2, y, z2, state);
        fill(world, o, x1, y, z1, Math.min(x1 + 1, x2), y, z2, state);
        fill(world, o, Math.max(x2 - 1, x1), y, z1, x2, y, z2, state);
    }

    private static void singleRing(ServerWorld world, BlockPos o,
                                   int x1, int x2, int z1, int z2, int y, BlockState state) {
        fill(world, o, x1, y, z1, x2, y, z1, state);
        fill(world, o, x1, y, z2, x2, y, z2, state);
        fill(world, o, x1, y, z1, x1, y, z2, state);
        fill(world, o, x2, y, z1, x2, y, z2, state);
    }

    private static int roofLayers(Room room, boolean ceremonial) {
        int z = room.centerZ();
        if (ceremonial) {
            // State rooms step through three heights: the central pair carry lanterns, while the
            // terminal halls sit lower beneath the corner-tower skyline.
            if (Math.abs(z) > 30) return 8;
            return z < 0 ? 9 : 10;
        }

        // Four side-pavilion rows deliberately use four distinct profiles. Their terminal pavilions
        // remain tallest; the two inner rows alternate lower/higher to remove cloned roof rhythm.
        if (z <= -30) return 10;
        if (z < 0) return 7;
        if (z < 30) return 9;
        return 11;
    }

    private static void buildCentralLantern(ServerWorld world, BlockPos o, Room room, int topY) {
        int x = room.centerX();
        int z = room.centerZ();
        int baseY = 40;
        fill(world, o, x - 5, baseY, z - 5, x + 5, baseY, z + 5, RIDGE);
        for (int y = baseY + 1; y <= topY - 2; y++) {
            for (int xx : new int[]{x - 5, x + 5}) {
                fill(world, o, xx, y, z - 4, xx, y, z + 4, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
            }
            for (int zz : new int[]{z - 5, z + 5}) {
                fill(world, o, x - 4, y, zz, x + 4, y, zz, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
            }
        }
        for (int xx : new int[]{x - 5, x, x + 5})
            fill(world, o, xx, baseY + 1, z - 5, xx, topY - 2, z + 5, log(FRAME, Direction.Axis.Y));
        fill(world, o, x - 6, topY - 1, z - 6, x + 6, topY - 1, z + 6, COPPER);
        fill(world, o, x - 4, topY, z - 4, x + 4, topY, z + 4, COPPER);
        world.setBlockState(o.add(x, topY + 1, z), Blocks.LIGHTNING_ROD.getDefaultState());
    }

    private static void buildDormers(ServerWorld world, BlockPos o) {
        Map<Integer, Integer> west = pavilionCenters(groundSideRooms(), true);
        Map<Integer, Integer> east = pavilionCenters(groundSideRooms(), false);

        // Every pavilion gets one outward dormer. Court-facing dormers are reserved for the two
        // inner rows, leaving terminal roofs cleaner and giving the eight roofs distinct elevations.
        west.forEach((z, ignored) -> dormerX(world, o, -49, z, Direction.WEST));
        east.forEach((z, ignored) -> dormerX(world, o, 49, z, Direction.EAST));
        west.forEach((z, ignored) -> {
            if (Math.abs(z) < 30) dormerX(world, o, -29, z, Direction.EAST);
        });
        east.forEach((z, ignored) -> {
            if (Math.abs(z) < 30) dormerX(world, o, 29, z, Direction.WEST);
        });
    }

    private static Map<Integer, Integer> pavilionCenters(Iterable<Room> rooms, boolean west) {
        Map<Integer, Integer> centers = new LinkedHashMap<>();
        for (Room room : rooms) {
            if ((room.centerX() < 0) == west) centers.put(room.centerZ(), room.centerX());
        }
        return centers;
    }

    private static void dormerX(ServerWorld world, BlockPos o, int x, int z, Direction facing) {
        int y = 33;
        fill(world, o, x, y, z - 2, x, y + 4, z + 2, RIDGE);
        fill(world, o, x + facing.getOffsetX(), y + 1, z - 1,
                x + facing.getOffsetX(), y + 3, z + 1, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
        fill(world, o, x - 1, y + 5, z - 3, x + 1, y + 5, z + 3, COPPER);
    }

    private static void buildCourtyardGutters(ServerWorld world, BlockPos o) {
        for (int x : new int[]{WEST_WING_MAX_X + 1, CENTRAL_MIN_X - 1, CENTRAL_MAX_X + 1, EAST_WING_MIN_X - 1}) {
            for (int z = -51; z <= 51; z++) {
                if (Math.floorMod(z + 53, 28) >= 23) continue;
                world.setBlockState(o.add(x, 30, z), Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
            }
        }
    }
}
