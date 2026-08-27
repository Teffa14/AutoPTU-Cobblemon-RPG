package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;

/** Ground-floor side salons, collections and working rooms. */
final class OurosGrandPalaceSalonRooms {
    private OurosGrandPalaceSalonRooms() {}

    static void buildAll(ServerWorld world, BlockPos o) {
        buildCabinet(world, o);
        buildSallaTerrena(world, o);
        buildBloomingSalon(world, o);
        buildHuntingSalon(world, o);
        buildLibrary(world, o);
        buildGeographyCabinet(world, o);
        buildPorcelainHall(world, o);
        buildGalleryOfArt(world, o);
    }

    private static void connectLeftRoom(ServerWorld world, BlockPos o, Room r) {
        doorEast(world, o, r, 5, 5);
        doorWest(world, o, r, 3, 4);
    }

    private static void connectRightRoom(ServerWorld world, BlockPos o, Room r) {
        doorWest(world, o, r, 5, 5);
        doorEast(world, o, r, 3, 4);
    }

    private static void buildCabinet(ServerWorld world, BlockPos o) {
        Room r = CABINET;
        BlockState wood = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState red = Blocks.RED_TERRACOTTA.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();

        roomShell(world, o, r, wood, red, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.STRIPPED_DARK_OAK_WOOD.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState(), trim);
        connectLeftRoom(world, o, r);
        insetCeiling(world, o, r, trim, Blocks.DARK_OAK_PLANKS.getDefaultState(), pale);
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        cabinetZ(world, o, r.minX() + 3, r.minX() + 9, 1, r.minZ() + 1, 8, wood, trim, true);
        cabinetZ(world, o, r.maxX() - 9, r.maxX() - 3, 1, r.minZ() + 1, 8, wood, trim, true);
        cabinetZ(world, o, r.minX() + 3, r.minX() + 9, 1, r.maxZ() - 1, 8, wood, trim, false);
        cabinetZ(world, o, r.maxX() - 9, r.maxX() - 3, 1, r.maxZ() - 1, 8, wood, trim, false);

        table(world, o, r.centerX() - 4, r.centerZ() - 2, r.centerX() + 4, r.centerZ() + 2, 3,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        for (int x : new int[]{r.centerX() - 3, r.centerX(), r.centerX() + 3}) {
            chair(world, o, x, 2, r.centerZ() - 4, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, x, 2, r.centerZ() + 4, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }
        world.setBlockState(o.add(r.centerX(), 4, r.centerZ()), Blocks.LECTERN.getDefaultState());
        world.setBlockState(o.add(r.centerX() - 3, 4, r.centerZ()), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        world.setBlockState(o.add(r.centerX() + 3, 4, r.centerZ()), Blocks.BREWING_STAND.getDefaultState());

        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            wallCandelabra(world, o, r.minX() + 1, 6, z, Direction.EAST, trim);
        }
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, trim, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildSallaTerrena(ServerWorld world, BlockPos o) {
        Room r = SALLA_TERRENA;
        BlockState white = Blocks.CALCITE.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState green = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, white, pale, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.SMOOTH_QUARTZ.getDefaultState(), white, trim);
        connectRightRoom(world, o, r);
        glazedCeiling(world, o, r, green, Blocks.WHITE_STAINED_GLASS.getDefaultState(),
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Pale arcade with plant-filled niches and a cool glazed roof.
        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += 5) {
            fill(world, o, r.minX() + 1, 1, z, r.minX() + 2, 11, z, pale);
            fill(world, o, r.maxX() - 2, 1, z, r.maxX() - 1, 11, z, pale);
            fill(world, o, r.minX() + 3, 4, z - 1, r.minX() + 3, 9, z + 1, Blocks.GLASS.getDefaultState());
            fill(world, o, r.maxX() - 3, 4, z - 1, r.maxX() - 3, 9, z + 1, Blocks.GLASS.getDefaultState());
        }

        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            pottedPlant(world, o, r.minX() + 5, 1, z, Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
            pottedPlant(world, o, r.maxX() - 5, 1, z, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
            bench(world, o, r.minX() + 7, z, r.minX() + 10, z, 2,
                    Blocks.BIRCH_SLAB.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState());
        }

        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ() - 5, 4, trim, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ() + 5, 4, trim, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildBloomingSalon(ServerWorld world, BlockPos o) {
        Room r = BLOOMING_SALON;
        BlockState blush = Blocks.PINK_TERRACOTTA.getDefaultState();
        BlockState teal = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
        BlockState terracotta = Blocks.TERRACOTTA.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, blush, terracotta, Blocks.POLISHED_GRANITE.getDefaultState(),
                Blocks.CUT_COPPER.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(), trim);
        connectLeftRoom(world, o, r);
        insetCeiling(world, o, r, teal, Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.CYAN_TERRACOTTA.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        for (int x : new int[]{r.minX() + 4, r.minX() + 9, r.maxX() - 9, r.maxX() - 4}) {
            fill(world, o, x, 1, r.minZ() + 2, x, 9, r.minZ() + 2, terracotta);
            world.setBlockState(o.add(x, 10, r.minZ() + 2), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            pottedPlant(world, o, x, 1, r.maxZ() - 4, Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        }
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 4) {
            fill(world, o, r.minX() + 2, 5, z, r.minX() + 2, 10, z, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            fill(world, o, r.maxX() - 2, 5, z, r.maxX() - 2, 10, z, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
        }

        for (int z : new int[]{r.centerZ() - 4, r.centerZ() + 4}) {
            bench(world, o, r.minX() + 5, z, r.minX() + 10, z, 2,
                    Blocks.CHERRY_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            table(world, o, r.maxX() - 9, z - 1, r.maxX() - 6, z + 1, 2,
                    Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }

        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, trim, Blocks.PINK_STAINED_GLASS.getDefaultState());
    }

    private static void buildHuntingSalon(ServerWorld world, BlockPos o) {
        Room r = HUNTING_SALON;
        BlockState brown = Blocks.BROWN_TERRACOTTA.getDefaultState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState green = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState trim = Blocks.STRIPPED_SPRUCE_WOOD.getDefaultState();

        roomShell(world, o, r, brown, dark, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.SPRUCE_PLANKS.getDefaultState(), dark, trim);
        connectRightRoom(world, o, r);
        insetCeiling(world, o, r, trim, Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.GREEN_TERRACOTTA.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Trophy wall abstractions use targets, timber frames and antler-like fence silhouettes.
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            wallPanelX(world, o, r.maxX() - 1, 4, z, 5, 6, trim, Blocks.TARGET.getDefaultState(), dark);
            world.setBlockState(o.add(r.maxX() - 2, 9, z - 1), Blocks.SPRUCE_FENCE.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 2, 9, z + 1), Blocks.SPRUCE_FENCE.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 10, z - 2), Blocks.SPRUCE_FENCE.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 10, z + 2), Blocks.SPRUCE_FENCE.getDefaultState());
        }

        fill(world, o, r.minX() + 2, 1, r.minZ() + 2, r.minX() + 7, 3, r.minZ() + 5, Blocks.MOSS_BLOCK.getDefaultState());
        world.setBlockState(o.add(r.minX() + 4, 4, r.minZ() + 4), Blocks.OAK_LEAVES.getDefaultState());
        table(world, o, r.centerX() - 4, r.centerZ() - 2, r.centerX() + 4, r.centerZ() + 2, 3,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        for (int x : new int[]{r.centerX() - 3, r.centerX() + 3}) {
            chair(world, o, x, 2, r.centerZ() - 4, Direction.SOUTH,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
            chair(world, o, x, 2, r.centerZ() + 4, Direction.NORTH,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        }
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, trim, Blocks.ORANGE_STAINED_GLASS.getDefaultState());
    }

    private static void buildLibrary(ServerWorld world, BlockPos o) {
        Room r = LIBRARY;
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState trim = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();

        roomShell(world, o, r, dark, trim, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.POLISHED_DIORITE.getDefaultState(), dark, gold);
        connectLeftRoom(world, o, r);
        glazedCeiling(world, o, r, trim, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Floor-to-ceiling book bays with pilasters, ladders and reading desks.
        for (int z = r.minZ() + 2; z <= r.maxZ() - 4; z += 5) {
            fill(world, o, r.minX() + 1, 1, z, r.minX() + 1, 11, z + 3, Blocks.BOOKSHELF.getDefaultState());
            fill(world, o, r.maxX() - 1, 1, z, r.maxX() - 1, 11, z + 3, Blocks.BOOKSHELF.getDefaultState());
            fill(world, o, r.minX() + 2, 1, z - 1, r.minX() + 2, 12, z - 1, gold);
            fill(world, o, r.maxX() - 2, 1, z - 1, r.maxX() - 2, 12, z - 1, gold);
            world.setBlockState(o.add(r.minX() + 2, 4, z + 1), Blocks.LADDER.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 2, 4, z + 1), Blocks.LADDER.getDefaultState());
        }

        table(world, o, r.centerX() - 5, r.centerZ() - 2, r.centerX() + 5, r.centerZ() + 2, 3,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        for (int x = r.centerX() - 4; x <= r.centerX() + 4; x += 4) {
            world.setBlockState(o.add(x, 4, r.centerZ()), Blocks.LECTERN.getDefaultState());
            chair(world, o, x, 2, r.centerZ() - 4, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            chair(world, o, x, 2, r.centerZ() + 4, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }
        pottedPlant(world, o, r.minX() + 4, 1, r.minZ() + 4, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 4, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildGeographyCabinet(ServerWorld world, BlockPos o) {
        Room r = GEOGRAPHY_CABINET;
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState teal = Blocks.WARPED_PLANKS.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, dark, teal, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.POLISHED_ANDESITE.getDefaultState(), dark, trim);
        connectRightRoom(world, o, r);
        insetCeiling(world, o, r, trim, Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.CYAN_TERRACOTTA.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        // Block mosaics stand in for map cabinets and geographic wall plates.
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            wallPanelX(world, o, r.maxX() - 1, 4, z, 5, 6, trim,
                    Math.floorMod(z, 12) == 0 ? Blocks.BLUE_TERRACOTTA.getDefaultState() : Blocks.GREEN_TERRACOTTA.getDefaultState(),
                    Blocks.CUT_COPPER.getDefaultState());
        }
        cabinetX(world, o, r.minX() + 1, 1, r.minZ() + 3, r.maxZ() - 3, 8, dark, trim, false);
        globe(world, o, r.centerX() - 5, 2, r.centerZ());
        globe(world, o, r.centerX() + 5, 2, r.centerZ() + 5);

        table(world, o, r.centerX() - 3, r.centerZ() - 3, r.centerX() + 3, r.centerZ() + 1, 3,
                Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        world.setBlockState(o.add(r.centerX(), 4, r.centerZ() - 1), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 5, trim, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildPorcelainHall(ServerWorld world, BlockPos o) {
        Room r = PORCELAIN_HALL;
        BlockState white = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState cyan = Blocks.CYAN_TERRACOTTA.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState trim = Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();

        roomShell(world, o, r, white, pale, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.WHITE_TERRACOTTA.getDefaultState(), white, trim);
        connectLeftRoom(world, o, r);
        glazedCeiling(world, o, r, trim, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.WHITE_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += 4) {
            fill(world, o, r.minX() + 1, 2, z, r.minX() + 2, 10, z, cyan);
            fill(world, o, r.maxX() - 2, 2, z, r.maxX() - 1, 10, z, cyan);
            world.setBlockState(o.add(r.minX() + 3, 3, z), Blocks.DECORATED_POT.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 3, z), Blocks.DECORATED_POT.getDefaultState());
            world.setBlockState(o.add(r.minX() + 3, 7, z), Blocks.FLOWER_POT.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 7, z), Blocks.FLOWER_POT.getDefaultState());
        }
        fill(world, o, r.minX() + 3, 2, r.minZ() + 2, r.maxX() - 3, 2, r.minZ() + 3, Blocks.QUARTZ_SLAB.getDefaultState());
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
            world.setBlockState(o.add(x, 3, r.minZ() + 3), Blocks.DECORATED_POT.getDefaultState());
        }
        bench(world, o, r.centerX() - 4, r.maxZ() - 5, r.centerX() + 4, r.maxZ() - 5, 2,
                Blocks.BIRCH_SLAB.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 4, Blocks.BAMBOO_MOSAIC.getDefaultState(), Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildGalleryOfArt(ServerWorld world, BlockPos o) {
        Room r = GALLERY_OF_ART;
        BlockState dark = Blocks.DEEPSLATE_BRICKS.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, dark, pale, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.POLISHED_GRANITE.getDefaultState(), Blocks.GLASS.getDefaultState(), trim);
        connectRightRoom(world, o, r);
        glazedCeiling(world, o, r, Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState(),
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        fill(world, o, r.minX() + 1, 1, r.centerZ() - 1, r.maxX() - 1, 1, r.centerZ() + 1, Blocks.RED_CARPET.getDefaultState());

        int variant = 0;
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 7) {
            wallPanelX(world, o, r.minX() + 1, 3, z, 6, 8, pale,
                    variant % 2 == 0 ? Blocks.WHITE_TERRACOTTA.getDefaultState() : Blocks.PINK_TERRACOTTA.getDefaultState(), trim);
            wallPanelX(world, o, r.maxX() - 1, 3, z, 6, 8, pale,
                    variant % 2 == 0 ? Blocks.BLUE_TERRACOTTA.getDefaultState() : Blocks.GREEN_TERRACOTTA.getDefaultState(), trim);
            variant++;
        }
        for (int x : new int[]{r.centerX() - 5, r.centerX() + 5}) {
            fill(world, o, x - 1, 1, r.centerZ() - 1, x + 1, 3, r.centerZ() + 1, Blocks.QUARTZ_BLOCK.getDefaultState());
            world.setBlockState(o.add(x, 4, r.centerZ()), Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
            world.setBlockState(o.add(x, 5, r.centerZ()), Blocks.QUARTZ_PILLAR.getDefaultState());
        }
        for (int z : new int[]{r.minZ() + 5, r.maxZ() - 5}) {
            bench(world, o, r.centerX() - 4, z, r.centerX() + 4, z, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(), 4, trim, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }
}