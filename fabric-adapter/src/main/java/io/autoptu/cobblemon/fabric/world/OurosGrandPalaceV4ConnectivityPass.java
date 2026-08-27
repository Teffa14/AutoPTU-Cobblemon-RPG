package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/** Targeted structural joins for galleries, garden props and independent roof eaves. */
final class OurosGrandPalaceV4ConnectivityPass {
    private static final BlockState GALLERY = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState CORNICE = Blocks.POLISHED_DIORITE.getDefaultState();

    private OurosGrandPalaceV4ConnectivityPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        connectExteriorUpperGalleries(world, o);
        supportRearGardenPots(world, o);
        connectUpperStairwellDisplays(world, o);
        for (OurosGrandPalaceBuildKit.Room room : groundSideRooms()) supportRoofEave(world, o, room);
        for (OurosGrandPalaceBuildKit.Room room : ceremonialRooms()) supportRoofEave(world, o, room);
    }

    private static void connectExteriorUpperGalleries(ServerWorld world, BlockPos o) {
        // The decorative rail is at x=±54. Extend the y=15 gallery deck two blocks outward so each
        // fence post is physically connected to the upper-room floor instead of hanging in space.
        fill(world, o, -54, 15, -53, -50, 15, 53, GALLERY);
        fill(world, o, 50, 15, -53, 54, 15, 53, GALLERY);
    }

    private static void supportRearGardenPots(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-15, -5, 5, 15}) {
            // The terrace floor is y=0; the original pots begin at y=2.
            world.setBlockState(o.add(x, 1, 63), Blocks.POLISHED_ANDESITE.getDefaultState());
        }
    }

    private static void connectUpperStairwellDisplays(ServerWorld world, BlockPos o) {
        OurosGrandPalaceBuildKit.Room west = physical(OurosGrandPalace.RAILING_SALON);
        int railingZ = west.minZ() + 14;
        // The middle railing strip crosses the west stair void. Give its safe east end a real post
        // onto intact upper-floor structure instead of dropping a column through the stair flight.
        world.setBlockState(
                o.add(west.maxX() - 4, west.floorY() + 1, railingZ),
                Blocks.DARK_OAK_FENCE.getDefaultState()
        );

        OurosGrandPalaceBuildKit.Room east = physical(OurosGrandPalace.COAT_OF_ARMS_HALL);
        int displayX = east.centerX() + 6;
        int displayZ = east.centerZ();
        // The right heraldic pedestal sits above the east stair void. A short stone bearer reaches
        // the intact floor at x=displayX+3 while retaining the stair's full vertical clearance.
        fill(world, o,
                displayX, east.floorY() + 1, displayZ,
                displayX + 3, east.floorY() + 1, displayZ,
                CORNICE);
    }

    private static void supportRoofEave(ServerWorld world, BlockPos o, OurosGrandPalaceBuildKit.Room room) {
        int y = 29;
        int x1 = room.minX() - 2;
        int x2 = room.maxX() + 2;
        int z1 = room.minZ() - 2;
        int z2 = room.maxZ() + 2;

        // A two-block overhang connects the y=30 mansard perimeter to the room ceiling/wall order.
        // Adjacent pavilion cells remain separated by the authored six-block z gaps.
        fill(world, o, x1, y, z1, x2, y, room.minZ(), CORNICE);
        fill(world, o, x1, y, room.maxZ(), x2, y, z2, CORNICE);
        fill(world, o, x1, y, z1, room.minX(), y, z2, CORNICE);
        fill(world, o, room.maxX(), y, z1, x2, y, z2, CORNICE);
    }
}
