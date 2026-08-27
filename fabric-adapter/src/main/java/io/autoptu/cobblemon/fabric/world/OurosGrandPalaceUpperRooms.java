package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;

/** Seven second-floor rooms completing the nineteen-space palace program. */
final class OurosGrandPalaceUpperRooms {
    private OurosGrandPalaceUpperRooms() {}

    static void buildAll(ServerWorld world, BlockPos o) {
        buildRailingSalon(world, o);
        buildCoatOfArmsHall(world, o);
        buildAccountingOffice(world, o);
        buildMusicChamber(world, o);
        buildGlobeBookCabinet(world, o);
        buildBlueSalon(world, o);
        buildBanquetHall(world, o);
    }

    private static void connectLeftUpper(ServerWorld world, BlockPos o, Room r) {
        doorEast(world, o, r, 5, 5);
    }

    private static void connectRightUpper(ServerWorld world, BlockPos o, Room r) {
        doorWest(world, o, r, 5, 5);
    }

    private static void buildRailingSalon(ServerWorld world, BlockPos o) {
        Room r = RAILING_SALON;
        BlockState cream = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState red = Blocks.RED_TERRACOTTA.getDefaultState();

        roomShell(world, o, r, cream, red, dark, Blocks.POLISHED_GRANITE.getDefaultState(), cream, gold);
        connectLeftUpper(world, o, r);
        insetCeiling(world, o, r, gold, Blocks.DARK_PRISMARINE.getDefaultState(), cream);
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Dense but ordered furniture study from the rail/table/chair reference.
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            table(world, o, r.minX() + 5, z - 2, r.minX() + 10, z + 2, 18,
                    Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            table(world, o, r.maxX() - 10, z - 2, r.maxX() - 5, z + 2, 18,
                    Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, r.minX() + 7, 17, z - 4, Direction.SOUTH,
                    Blocks.BAMBOO_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, r.minX() + 7, 17, z + 4, Direction.NORTH,
                    Blocks.BAMBOO_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, r.maxX() - 7, 17, z - 4, Direction.SOUTH,
                    Blocks.BAMBOO_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, r.maxX() - 7, 17, z + 4, Direction.NORTH,
                    Blocks.BAMBOO_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }

        // A real balustrade gallery creates the reference's repeated railing language.
        fill(world, o, r.minX() + 3, 17, r.minZ() + 3, r.maxX() - 3, 17, r.minZ() + 3, Blocks.BAMBOO_FENCE.getDefaultState());
        fill(world, o, r.minX() + 3, 17, r.maxZ() - 3, r.maxX() - 3, 17, r.maxZ() - 3, Blocks.BAMBOO_FENCE.getDefaultState());
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x += 3) {
            world.setBlockState(o.add(x, 18, r.minZ() + 3), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(x, 18, r.maxZ() - 3), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
        }

        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildCoatOfArmsHall(ServerWorld world, BlockPos o) {
        Room r = COAT_OF_ARMS_HALL;
        BlockState stone = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState dark = Blocks.DEEPSLATE_BRICKS.getDefaultState();
        BlockState gold = Blocks.WAXED_CUT_COPPER.getDefaultState();
        BlockState red = Blocks.RED_TERRACOTTA.getDefaultState();

        roomShell(world, o, r, stone, dark, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.POLISHED_ANDESITE.getDefaultState(), stone, gold);
        connectRightUpper(world, o, r);
        insetCeiling(world, o, r, gold, Blocks.POLISHED_DIORITE.getDefaultState(), dark);
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        coatOfArms(world, o, r.centerX(), 18, r.maxZ() - 1);
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            fill(world, o, r.minX() + 2, 16, z, r.minX() + 3, 26, z, gold);
            fill(world, o, r.maxX() - 3, 16, z, r.maxX() - 2, 26, z, gold);
            wallCandelabra(world, o, r.minX() + 4, 21, z, Direction.EAST, gold);
            wallCandelabra(world, o, r.maxX() - 4, 21, z, Direction.WEST, gold);
        }

        bench(world, o, r.centerX() - 5, r.minZ() + 5, r.centerX() + 5, r.minZ() + 5, 17,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildAccountingOffice(ServerWorld world, BlockPos o) {
        Room r = ACCOUNTING_OFFICE;
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState red = Blocks.RED_TERRACOTTA.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState teal = Blocks.WARPED_PLANKS.getDefaultState();

        roomShell(world, o, r, dark, teal, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.BAMBOO_MOSAIC.getDefaultState(), dark, gold);
        connectLeftUpper(world, o, r);
        insetCeiling(world, o, r, gold, Blocks.DARK_OAK_PLANKS.getDefaultState(), red);
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            table(world, o, r.minX() + 4, z - 2, r.minX() + 10, z + 1, 18,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            chair(world, o, r.minX() + 7, 17, z + 3, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            world.setBlockState(o.add(r.minX() + 5, 19, z), Blocks.LECTERN.getDefaultState());
            world.setBlockState(o.add(r.minX() + 8, 19, z), Blocks.CRAFTER.getDefaultState());
        }
        cabinetX(world, o, r.maxX() - 1, 16, r.minZ() + 3, r.maxZ() - 3, 10, dark, gold, true);
        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += 4) {
            world.setBlockState(o.add(r.maxX() - 2, 18, z), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 2, 22, z), Blocks.CHISELED_BOOKSHELF.getDefaultState());
        }
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildMusicChamber(ServerWorld world, BlockPos o) {
        Room r = MUSIC_CHAMBER;
        BlockState red = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState cream = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, red, dark, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.STRIPPED_DARK_OAK_WOOD.getDefaultState(), cream, gold);
        connectRightUpper(world, o, r);
        insetCeiling(world, o, r, gold, cream, Blocks.ORANGE_TERRACOTTA.getDefaultState());
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Harpsichord sits on a shallow performance platform with audience seating opposite.
        fill(world, o, r.minX() + 3, 16, r.minZ() + 3, r.maxX() - 3, 17, r.minZ() + 9, Blocks.DARK_OAK_PLANKS.getDefaultState());
        harpsichord(world, o, r.centerX(), 19, r.minZ() + 7, Direction.SOUTH);
        for (int z : new int[]{r.centerZ() + 2, r.maxZ() - 5}) {
            for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
                chair(world, o, x, 17, z, Direction.NORTH,
                        Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            }
        }

        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += 5) {
            wallPanelX(world, o, r.maxX() - 1, 19, z, 4, 6, gold, cream, dark);
            wallPanelX(world, o, r.minX() + 1, 19, z, 4, 6, gold, Blocks.BROWN_TERRACOTTA.getDefaultState(), dark);
        }
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildGlobeBookCabinet(ServerWorld world, BlockPos o) {
        Room r = GLOBE_BOOK_CABINET;
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();

        roomShell(world, o, r, dark, Blocks.OXIDIZED_CUT_COPPER.getDefaultState(),
                Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState(), dark, gold);
        connectLeftUpper(world, o, r);
        glazedCeiling(world, o, r, Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState(),
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        cabinetX(world, o, r.minX() + 1, 16, r.minZ() + 2, r.maxZ() - 2, 10, dark, gold, true);
        cabinetZ(world, o, r.minX() + 3, r.maxX() - 3, 16, r.maxZ() - 1, 10, dark, gold, true);
        globe(world, o, r.centerX() + 4, 17, r.centerZ());
        table(world, o, r.centerX() - 6, r.centerZ() - 2, r.centerX(), r.centerZ() + 2, 18,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        world.setBlockState(o.add(r.centerX() - 3, 19, r.centerZ()), Blocks.LECTERN.getDefaultState());
        chair(world, o, r.centerX() - 3, 17, r.centerZ() + 4, Direction.NORTH,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ() - 5, 4, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ() + 5, 4, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildBlueSalon(ServerWorld world, BlockPos o) {
        Room r = BLUE_SALON;
        BlockState blue = Blocks.BLUE_TERRACOTTA.getDefaultState();
        BlockState cyan = Blocks.CYAN_TERRACOTTA.getDefaultState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, blue, cyan, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.POLISHED_DIORITE.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(), gold);
        connectRightUpper(world, o, r);
        insetCeiling(world, o, r, gold, Blocks.DARK_PRISMARINE.getDefaultState(), Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState());
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 1, r.maxX() - 1, 16, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            wallPanelX(world, o, r.minX() + 1, 19, z, 5, 7, gold, cyan, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.getDefaultState());
            wallPanelX(world, o, r.maxX() - 1, 19, z, 5, 7, gold, blue, Blocks.CYAN_GLAZED_TERRACOTTA.getDefaultState());
        }
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            bench(world, o, r.minX() + 4, z, r.minX() + 9, z, 17,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            bench(world, o, r.maxX() - 9, z, r.maxX() - 4, z, 17,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        pottedPlant(world, o, r.minX() + 4, 17, r.minZ() + 4, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        pottedPlant(world, o, r.maxX() - 4, 17, r.minZ() + 4, Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, gold, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
    }

    private static void buildBanquetHall(ServerWorld world, BlockPos o) {
        Room r = BANQUET_HALL;
        BlockState cream = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState copper = Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();

        roomShell(world, o, r, cream, pale, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(), gold);
        connectLeftUpper(world, o, r);
        glazedCeiling(world, o, r, copper, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 16, r.centerZ() - 2, r.maxX() - 1, 16, r.centerZ() + 2, Blocks.RED_CARPET.getDefaultState());

        // Two long feast tables flank the ceremonial runner.
        for (int x : new int[]{r.minX() + 7, r.maxX() - 7}) {
            table(world, o, x - 2, r.minZ() + 4, x + 2, r.maxZ() - 4, 18,
                    Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += 4) {
                chair(world, o, x - 4, 17, z, Direction.EAST,
                        Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
                chair(world, o, x + 4, 17, z, Direction.WEST,
                        Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
                world.setBlockState(o.add(x, 19, z), Math.floorMod(z, 8) == 0
                        ? Blocks.CAKE.getDefaultState()
                        : litCandle(Blocks.WHITE_CANDLE.getDefaultState()));
            }
        }

        // Floral table centerpieces and service sideboards.
        for (int z = r.minZ() + 7; z <= r.maxZ() - 7; z += 8) {
            world.setBlockState(o.add(r.minX() + 7, 19, z), Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 7, 19, z), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        }
        cabinetZ(world, o, r.minX() + 3, r.minX() + 9, 16, r.minZ() + 1, 7,
                Blocks.DARK_OAK_PLANKS.getDefaultState(), gold, false);
        cabinetZ(world, o, r.maxX() - 9, r.maxX() - 3, 16, r.minZ() + 1, 7,
                Blocks.DARK_OAK_PLANKS.getDefaultState(), gold, false);

        for (int z : new int[]{r.minZ() + 6, r.centerZ(), r.maxZ() - 6}) {
            chandelier(world, o, r.centerX(), r.ceilingY(), z, 4, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        }
    }
}