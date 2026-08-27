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

/** Independent mansard bodies for the courtyard-based V4 massing. */
final class OurosGrandPalaceV4RoofPass {
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState RIDGE = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState FRAME = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();

    private OurosGrandPalaceV4RoofPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        // Ground cells define the eight side-wing roof pavilions; their upper rooms share footprint.
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
        int layers = ceremonial ? 9 : 8;

        // A stepped perimeter produces a readable Minecraft mansard without a giant universal roof.
        for (int layer = 0; layer < layers; layer++) {
            int ax1 = x1 + layer;
            int ax2 = x2 - layer;
            int az1 = z1 + layer;
            int az2 = z2 - layer;
            if (ax1 > ax2 || az1 > az2) break;
            BlockState state = layer == 0 || layer == layers - 1 ? RIDGE : ROOF;
            fill(world, o, ax1, eaveY + layer, az1, ax2, eaveY + layer, az1, state);
            fill(world, o, ax1, eaveY + layer, az2, ax2, eaveY + layer, az2, state);
            fill(world, o, ax1, eaveY + layer, az1, ax1, eaveY + layer, az2, state);
            fill(world, o, ax2, eaveY + layer, az1, ax2, eaveY + layer, az2, state);
        }

        int topInset = layers;
        fill(world, o, x1 + topInset, eaveY + layers, z1 + topInset,
                x2 - topInset, eaveY + layers, z2 - topInset, COPPER);
        world.setBlockState(o.add(room.centerX(), eaveY + layers + 1, room.centerZ()),
                Blocks.LIGHTNING_ROD.getDefaultState());
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
        west.forEach((z, ignored) -> dormerX(world, o, -53, z, Direction.WEST));
        east.forEach((z, ignored) -> dormerX(world, o, 53, z, Direction.EAST));

        // Court-facing dormers make each pavilion visible from the garden, not only from outside.
        west.forEach((z, ignored) -> dormerX(world, o, -26, z, Direction.EAST));
        east.forEach((z, ignored) -> dormerX(world, o, 26, z, Direction.WEST));
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
        // Thin copper lines emphasize the four roof edges that define the two open courts.
        for (int x : new int[]{WEST_WING_MAX_X + 1, CENTRAL_MIN_X - 1, CENTRAL_MAX_X + 1, EAST_WING_MIN_X - 1}) {
            for (int z = -51; z <= 51; z++) {
                if (Math.floorMod(z + 53, 28) >= 23) continue;
                world.setBlockState(o.add(x, 30, z), Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState());
            }
        }
    }
}