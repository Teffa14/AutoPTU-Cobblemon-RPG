package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.Room;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Removes the cloned table/chair pair that the first V4 room proof placed in every authored room.
 *
 * The baseline was useful while proving density, but browser review makes the repetition obvious.
 * This pass only removes blocks at the exact baseline coordinates and only when the live state is one
 * of the original baseline furniture materials. Room-specific furniture, shelves, exhibits, carpets,
 * heraldry and later authored layouts are therefore left intact.
 */
final class OurosGrandPalaceV4GenericFurnitureCleanupPass {
    private OurosGrandPalaceV4GenericFurnitureCleanupPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (Room room : allRooms()) removeBaselineFurniture(world, o, room);
    }

    private static List<Room> allRooms() {
        List<Room> rooms = new ArrayList<>(19);
        rooms.addAll(ceremonialRooms());
        rooms.addAll(groundSideRooms());
        rooms.addAll(upperSideRooms());
        return rooms;
    }

    private static void removeBaselineFurniture(ServerWorld world, BlockPos o, Room room) {
        int cx = room.centerX();
        int cz = room.centerZ();
        int floor = room.floorY();

        removeBaselineTable(world, o, cx, cz - 8, cz - 6, floor);
        removeBaselineTable(world, o, cx, cz + 6, cz + 8, floor);

        for (int dx : new int[]{-3, 3}) {
            for (int z : new int[]{cz - 7, cz + 7}) {
                removeIfBaseline(world, o.add(cx + dx, floor + 2, z));
                int backX = dx < 0 ? cx - 4 : cx + 4;
                removeIfBaseline(world, o.add(backX, floor + 3, z));
            }
        }
    }

    private static void removeBaselineTable(ServerWorld world, BlockPos o,
                                            int cx, int z1, int z2, int floor) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = z1; z <= z2; z++) {
                removeIfBaseline(world, o.add(x, floor + 2, z));
            }
        }
        for (int x : new int[]{cx - 2, cx + 2}) {
            for (int z : new int[]{z1, z2}) {
                removeIfBaseline(world, o.add(x, floor + 1, z));
            }
        }
    }

    private static void removeIfBaseline(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.DARK_OAK_PLANKS)
                || state.isOf(Blocks.SPRUCE_PLANKS)
                || state.isOf(Blocks.DARK_OAK_FENCE)
                || state.isOf(Blocks.SPRUCE_FENCE)
                || state.isOf(Blocks.DARK_OAK_STAIRS)
                || state.isOf(Blocks.SPRUCE_STAIRS)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }
}
