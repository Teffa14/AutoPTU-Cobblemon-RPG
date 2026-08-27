package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;

/** Double-height ceremonial rooms at the heart of the Grand Palace. */
final class OurosGrandPalaceCeremonialRooms {
    private OurosGrandPalaceCeremonialRooms() {}

    static void buildAll(ServerWorld world, BlockPos o) {
        buildAntechamber(world, o);
        buildAudienceChamber(world, o);
        buildThemisHall(world, o);
        buildMarbleSalon(world, o);
    }

    private static void buildAntechamber(ServerWorld world, BlockPos o) {
        Room r = ANTECHAMBER;
        BlockState red = Blocks.RED_TERRACOTTA.getDefaultState();
        BlockState darkRed = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();

        roomShell(world, o, r, red, darkRed, dark, Blocks.STRIPPED_DARK_OAK_WOOD.getDefaultState(),
                Blocks.DARK_PRISMARINE.getDefaultState(), gold);
        doorNorth(world, o, r, 7, 8);
        doorSouth(world, o, r, 7, 8);
        grandDoorFrameNorth(world, o, r, gold);
        insetCeiling(world, o, r, gold, Blocks.DARK_PRISMARINE.getDefaultState(), pale);

        // Triple-height wall articulation inspired by the reference's red ceremonial antechamber.
        for (int yBand : new int[]{4, 11, 18, 25}) {
            fill(world, o, r.minX() + 1, yBand, r.minZ() + 1, r.maxX() - 1, yBand, r.minZ() + 1, gold);
            fill(world, o, r.minX() + 1, yBand, r.maxZ() - 1, r.maxX() - 1, yBand, r.maxZ() - 1, gold);
        }
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            fill(world, o, r.minX() + 1, 1, z, r.minX() + 2, 27, z, darkRed);
            fill(world, o, r.maxX() - 2, 1, z, r.maxX() - 1, 27, z, darkRed);
            world.setBlockState(o.add(r.minX() + 3, 8, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 8, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            wallCandelabra(world, o, r.minX() + 3, 9, z, Direction.EAST, gold);
            wallCandelabra(world, o, r.maxX() - 3, 9, z, Direction.WEST, gold);
        }

        // Upper overlook openings connect the room to the palace gallery rather than enclosing a void.
        for (int z : new int[]{r.centerZ() - 6, r.centerZ(), r.centerZ() + 6}) {
            clear(world, o, r.minX(), 16, z - 1, r.minX(), 21, z + 1);
            clear(world, o, r.maxX(), 16, z - 1, r.maxX(), 21, z + 1);
            fill(world, o, r.minX() + 1, 16, z - 1, r.minX() + 1, 17, z + 1, Blocks.BAMBOO_FENCE.getDefaultState());
            fill(world, o, r.maxX() - 1, 16, z - 1, r.maxX() - 1, 17, z + 1, Blocks.BAMBOO_FENCE.getDefaultState());
        }

        // Processional carpet and low furniture keep the center legible.
        fill(world, o, -2, 1, r.minZ() + 1, 2, 1, r.maxZ() - 1, Blocks.RED_CARPET.getDefaultState());
        for (int z : new int[]{r.minZ() + 5, r.maxZ() - 5}) {
            bench(world, o, r.minX() + 4, z, r.minX() + 8, z, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            bench(world, o, r.maxX() - 8, z, r.maxX() - 4, z, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        wallPanelZ(world, o, -5, 5, r.maxZ() - 1, 7, 8, gold, Blocks.WHITE_TERRACOTTA.getDefaultState(), darkRed);
        wallPanelZ(world, o, 5, 5, r.maxZ() - 1, 7, 8, gold, Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), darkRed);
        wallPanelZ(world, o, 0, 15, r.maxZ() - 1, 9, 8, pale, Blocks.RED_TERRACOTTA.getDefaultState(), gold);

        chandelier(world, o, 0, r.ceilingY(), r.centerZ(), 8, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() - 7, 6, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() + 7, 6, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildAudienceChamber(ServerWorld world, BlockPos o) {
        Room r = AUDIENCE_CHAMBER;
        BlockState wall = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState red = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState gold = Blocks.WAXED_CUT_COPPER.getDefaultState();
        BlockState bamboo = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState glass = Blocks.WHITE_STAINED_GLASS.getDefaultState();

        roomShell(world, o, r, wall, red, Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.POLISHED_BLACKSTONE.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState(), bamboo);
        doorNorth(world, o, r, 7, 8);
        doorSouth(world, o, r, 7, 8);
        insetCeiling(world, o, r, bamboo, Blocks.DARK_OAK_PLANKS.getDefaultState(), gold);
        fill(world, o, -2, 1, r.minZ() + 1, 2, 1, r.maxZ() - 1, Blocks.RED_CARPET.getDefaultState());

        // Throne dais and canopy form the room's unmistakable destination.
        fill(world, o, -7, 1, r.maxZ() - 6, 7, 1, r.maxZ() - 2, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        fill(world, o, -5, 2, r.maxZ() - 5, 5, 2, r.maxZ() - 2, Blocks.RED_CARPET.getDefaultState());
        fill(world, o, -4, 3, r.maxZ() - 3, 4, 3, r.maxZ() - 2, Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState());
        fill(world, o, -5, 4, r.maxZ() - 1, 5, 17, r.maxZ() - 1, red);
        for (int x : new int[]{-5, 5}) {
            fill(world, o, x, 3, r.maxZ() - 2, x, 19, r.maxZ() - 2, bamboo);
        }
        fill(world, o, -6, 19, r.maxZ() - 3, 6, 20, r.maxZ() - 1, bamboo);
        steppedArchZ(world, o, 0, 20, r.maxZ() - 2, 6, 4, gold);

        chair(world, o, 0, 3, r.maxZ() - 4, Direction.NORTH,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        world.setBlockState(o.add(0, 4, r.maxZ() - 5), Blocks.RED_BANNER.getDefaultState());

        // Side audience benches, paired columns and glass lanterns.
        for (int z = r.minZ() + 4; z <= r.maxZ() - 8; z += 5) {
            bench(world, o, r.minX() + 3, z, r.minX() + 7, z, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            bench(world, o, r.maxX() - 7, z, r.maxX() - 3, z, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            fill(world, o, r.minX() + 2, 1, z + 2, r.minX() + 3, 25, z + 2, bamboo);
            fill(world, o, r.maxX() - 3, 1, z + 2, r.maxX() - 2, 25, z + 2, bamboo);
        }

        // Upper gallery apertures with real railings.
        for (int z : new int[]{r.centerZ() - 5, r.centerZ() + 5}) {
            clear(world, o, r.minX(), 16, z - 2, r.minX(), 22, z + 2);
            clear(world, o, r.maxX(), 16, z - 2, r.maxX(), 22, z + 2);
            fill(world, o, r.minX() + 1, 16, z - 2, r.minX() + 1, 17, z + 2, Blocks.BAMBOO_FENCE.getDefaultState());
            fill(world, o, r.maxX() - 1, 16, z - 2, r.maxX() - 1, 17, z + 2, Blocks.BAMBOO_FENCE.getDefaultState());
        }

        chandelier(world, o, 0, r.ceilingY(), r.centerZ() - 4, 9, bamboo, glass);
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() + 5, 9, bamboo, glass);
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 7}) {
            wallCandelabra(world, o, r.minX() + 2, 8, z, Direction.EAST, gold);
            wallCandelabra(world, o, r.maxX() - 2, 8, z, Direction.WEST, gold);
        }
    }

    private static void buildThemisHall(ServerWorld world, BlockPos o) {
        Room r = THEMIS_HALL;
        BlockState green = Blocks.GREEN_TERRACOTTA.getDefaultState();
        BlockState oxidized = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();

        roomShell(world, o, r, green, oxidized, Blocks.POLISHED_TUFF.getDefaultState(),
                Blocks.CUT_COPPER.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(), gold);
        doorNorth(world, o, r, 7, 8);
        doorSouth(world, o, r, 7, 8);
        glazedCeiling(world, o, r, gold, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(),
                Blocks.LIME_STAINED_GLASS.getDefaultState());
        fill(world, o, -2, 1, r.minZ() + 1, 2, 1, r.maxZ() - 1, Blocks.RED_CARPET.getDefaultState());

        // Dense green/gold articulation from the reference, organized into structural bays.
        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += 4) {
            fill(world, o, r.minX() + 1, 1, z, r.minX() + 2, 27, z, oxidized);
            fill(world, o, r.maxX() - 2, 1, z, r.maxX() - 1, 27, z, oxidized);
            world.setBlockState(o.add(r.minX() + 3, 6, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(r.maxX() - 3, 6, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
        }
        for (int y : new int[]{5, 12, 19, 26}) {
            fill(world, o, r.minX() + 1, y, r.minZ() + 1, r.minX() + 2, y, r.maxZ() - 1, gold);
            fill(world, o, r.maxX() - 2, y, r.minZ() + 1, r.maxX() - 1, y, r.maxZ() - 1, gold);
        }

        // Themis relief: abstract balance scales rather than a textual sign.
        int z = r.maxZ() - 1;
        fill(world, o, 0, 6, z - 1, 0, 19, z - 1, pale);
        fill(world, o, -6, 17, z - 1, 6, 18, z - 1, gold);
        for (int x : new int[]{-5, 5}) {
            fill(world, o, x, 13, z - 1, x, 16, z - 1, Blocks.CHAIN.getDefaultState());
            fill(world, o, x - 2, 12, z - 1, x + 2, 12, z - 1, Blocks.CUT_COPPER_SLAB.getDefaultState());
            fill(world, o, x - 1, 11, z - 1, x + 1, 11, z - 1, Blocks.WAXED_CUT_COPPER.getDefaultState());
        }
        steppedArchZ(world, o, 0, 20, z - 1, 8, 5, gold);

        // Tribunal tables and symmetrical seating.
        table(world, o, -7, r.maxZ() - 6, 7, r.maxZ() - 4, 3,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        for (int x = -6; x <= 6; x += 3) {
            chair(world, o, x, 2, r.maxZ() - 8, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }
        for (int zz = r.minZ() + 5; zz <= r.centerZ() + 1; zz += 5) {
            bench(world, o, r.minX() + 3, zz, r.minX() + 7, zz, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
            bench(world, o, r.maxX() - 7, zz, r.maxX() - 3, zz, 2,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.BAMBOO_FENCE.getDefaultState());
        }

        chandelier(world, o, 0, r.ceilingY(), r.centerZ() - 5, 8, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() + 5, 8, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void buildMarbleSalon(ServerWorld world, BlockPos o) {
        Room r = MARBLE_SALON;
        BlockState marble = Blocks.CALCITE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.getDefaultState();
        BlockState purple = Blocks.PURPLE_TERRACOTTA.getDefaultState();
        BlockState gold = Blocks.BAMBOO_MOSAIC.getDefaultState();

        roomShell(world, o, r, marble, diorite, Blocks.POLISHED_DIORITE.getDefaultState(),
                Blocks.POLISHED_BLACKSTONE.getDefaultState(), marble, gold);
        doorNorth(world, o, r, 7, 8);
        doorSouth(world, o, r, 7, 8);
        insetCeiling(world, o, r, amethyst, marble, purple);
        fill(world, o, -2, 1, r.minZ() + 1, 2, 1, r.maxZ() - 1, Blocks.RED_CARPET.getDefaultState());

        // Purple marble wall fields with carved pale frames.
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            wallPanelX(world, o, r.minX() + 1, 4, z, 5, 10, diorite, amethyst, gold);
            wallPanelX(world, o, r.maxX() - 1, 4, z, 5, 10, diorite, amethyst, gold);
        }
        for (int y : new int[]{8, 16, 24}) {
            corniceRing(world, o, r, y == 16 ? purple : gold, y);
        }

        // Formal salon groupings leave the central carpet open.
        for (int z : new int[]{r.minZ() + 6, r.centerZ(), r.maxZ() - 6}) {
            bench(world, o, r.minX() + 3, z, r.minX() + 7, z, 2,
                    Blocks.PALE_OAK_SLAB.getDefaultState(), Blocks.POLISHED_DIORITE_WALL.getDefaultState());
            bench(world, o, r.maxX() - 7, z, r.maxX() - 3, z, 2,
                    Blocks.PALE_OAK_SLAB.getDefaultState(), Blocks.POLISHED_DIORITE_WALL.getDefaultState());
            table(world, o, r.minX() + 5, z + 2, r.minX() + 6, z + 3, 2,
                    Blocks.POLISHED_DIORITE_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            table(world, o, r.maxX() - 6, z + 2, r.maxX() - 5, z + 3, 2,
                    Blocks.POLISHED_DIORITE_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        pottedPlant(world, o, r.minX() + 4, 2, r.minZ() + 4, Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        pottedPlant(world, o, r.maxX() - 4, 2, r.minZ() + 4, Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ(), 8, gold, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() - 7, 7, gold, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        chandelier(world, o, 0, r.ceilingY(), r.centerZ() + 7, 7, gold, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
    }
}