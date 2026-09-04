package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.physical;

/**
 * Browser-review-driven compositions for rooms that still read as generic after baseline cleanup.
 *
 * Each intervention preserves the room shell and circulation portals. The goal is not to add density
 * for its own sake, but to give the room one legible use, axis and focal hierarchy.
 */
final class OurosGrandPalaceV4RoomCompositionPass {
    private static final BlockState DARK_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState DARK_FENCE = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState SPRUCE_SLAB = Blocks.SPRUCE_SLAB.getDefaultState();
    private static final BlockState QUARTZ = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
    private static final BlockState STONE = Blocks.POLISHED_DEEPSLATE.getDefaultState();

    private OurosGrandPalaceV4RoomCompositionPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        refineHuntingSalon(world, o, physical(HUNTING_SALON));
        refineCoatOfArmsHall(world, o, physical(COAT_OF_ARMS_HALL));
        refineGalleryOfArt(world, o, physical(GALLERY_OF_ART));
        refineBanquetHall(world, o, physical(BANQUET_HALL));
    }

    /** Replace three broad log slabs with narrow mounted trophies and side seating. */
    private static void refineHuntingSalon(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        int trophyZ = room.minZ() + 3;

        for (int x : new int[]{room.centerX() - 6, room.centerX(), room.centerX() + 6}) {
            clear(world, o, x - 1, floor + 2, trophyZ, x + 1, floor + 5, trophyZ);
            fill(world, o, x - 1, floor + 1, trophyZ, x + 1, floor + 1, trophyZ, STONE);
            world.setBlockState(o.add(x, floor + 2, trophyZ), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState());
            world.setBlockState(o.add(x, floor + 3, trophyZ), QUARTZ);
            world.setBlockState(o.add(x, floor + 4, trophyZ), Blocks.SKELETON_SKULL.getDefaultState());
        }

        int seatZ = room.maxZ() - 5;
        bench(world, o, room.minX() + 4, seatZ, room.centerX() - 3, seatZ,
                floor + 2, DARK_SLAB, DARK_FENCE);
        bench(world, o, room.centerX() + 3, seatZ, room.maxX() - 4, seatZ,
                floor + 2, DARK_SLAB, DARK_FENCE);
    }

    /** A heraldic processional axis and four standards turn the upper hall into a state display. */
    private static void refineCoatOfArmsHall(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        fill(world, o, room.centerX() - 1, floor + 1, room.minZ() + 3,
                room.centerX() + 1, floor + 1, room.maxZ() - 3, Blocks.RED_CARPET.getDefaultState());

        int[] xs = {room.centerX() - 6, room.centerX() + 6};
        int[] zs = {room.centerZ() - 5, room.centerZ() + 5};
        for (int x : xs) {
            for (int z : zs) {
                world.setBlockState(o.add(x, floor + 1, z), QUARTZ);
                BlockState banner = (x < room.centerX()) == (z < room.centerZ())
                        ? Blocks.RED_BANNER.getDefaultState()
                        : Blocks.WHITE_BANNER.getDefaultState();
                world.setBlockState(o.add(x, floor + 2, z), banner);
            }
        }

        bench(world, o, room.minX() + 4, room.centerZ() - 4,
                room.minX() + 4, room.centerZ() + 4, floor + 2, DARK_SLAB, DARK_FENCE);
        bench(world, o, room.maxX() - 4, room.centerZ() - 4,
                room.maxX() - 4, room.centerZ() + 4, floor + 2, DARK_SLAB, DARK_FENCE);
    }

    /** Keep the central sculpture but remove cloned flanking plinths and add gallery seating. */
    private static void refineGalleryOfArt(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        int z = room.centerZ();
        for (int x : new int[]{room.centerX() - 6, room.centerX() + 6}) {
            clear(world, o, x, floor + 1, z, x, floor + 4, z);
        }

        bench(world, o, room.minX() + 4, room.centerZ() - 5,
                room.minX() + 4, room.centerZ() + 5, floor + 2, SPRUCE_SLAB, DARK_FENCE);
        bench(world, o, room.maxX() - 4, room.centerZ() - 5,
                room.maxX() - 4, room.centerZ() + 5, floor + 2, SPRUCE_SLAB, DARK_FENCE);

        // Formal stone frame around the surviving central sculpture without blocking its sightline.
        for (int dx : new int[]{-2, 2}) {
            world.setBlockState(o.add(room.centerX() + dx, floor + 1, z - 2), STONE);
            world.setBlockState(o.add(room.centerX() + dx, floor + 1, z + 2), STONE);
        }
    }

    /** Replace cafeteria rows with one long state table, paired chairs and a clear ceremonial axis. */
    private static void refineBanquetHall(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        int cx = room.centerX();
        int z1 = room.minZ() + 4;
        int z2 = room.maxZ() - 4;

        clear(world, o, room.minX() + 3, floor + 1, room.minZ() + 3,
                room.maxX() - 3, floor + 4, room.maxZ() - 3);

        fill(world, o, cx - 1, floor + 1, room.minZ() + 3,
                cx + 1, floor + 1, room.maxZ() - 3, Blocks.RED_CARPET.getDefaultState());
        table(world, o, cx - 2, z1, cx + 2, z2, floor + 2, DARK_SLAB, DARK_FENCE);

        for (int z = z1 + 1; z <= z2 - 1; z += 3) {
            chair(world, o, cx - 4, floor + 2, z, Direction.EAST,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), DARK_FENCE);
            chair(world, o, cx + 4, floor + 2, z, Direction.WEST,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), DARK_FENCE);
        }
        chair(world, o, cx, floor + 2, z1 - 2, Direction.SOUTH,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), DARK_FENCE);
        chair(world, o, cx, floor + 2, z2 + 2, Direction.NORTH,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), DARK_FENCE);

        for (int z = z1 + 2; z <= z2 - 2; z += 4) {
            world.setBlockState(o.add(cx, floor + 3, z), litCandle(Blocks.WHITE_CANDLE.getDefaultState()));
        }
        world.setBlockState(o.add(cx, floor + 3, room.centerZ()), Blocks.CAKE.getDefaultState());
    }
}
