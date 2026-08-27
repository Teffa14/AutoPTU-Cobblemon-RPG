package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;

/**
 * Reference-driven interior composition pass for the Grand Palace.
 *
 * The original room builders proved routing and palette separation but left too much undecorated
 * volume. This pass treats each supplied reference room as a complete composition: wall architecture,
 * ceiling architecture, lighting, furniture, focal objects and small-scale ornament all receive an
 * authored layer. It intentionally uses ordinary vanilla BlockState geometry so the live-server
 * manifest remains the authority.
 */
final class OurosGrandPalaceReferenceInteriorPass {
    private static final BlockState GOLD = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState GOLD_SLAB = Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState();
    private static final BlockState GOLD_TRAP = Blocks.BAMBOO_TRAPDOOR.getDefaultState();
    private static final BlockState DARK = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState DARK_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState DARK_FENCE = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState PALE = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState WHITE = Blocks.CALCITE.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState OXIDIZED = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();

    private OurosGrandPalaceReferenceInteriorPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        antechamber(world, o);
        audienceChamber(world, o);
        themisHall(world, o);
        railingSalon(world, o);
        cabinet(world, o);
        sallaTerrena(world, o);
        coatOfArmsHall(world, o);
        bloomingSalon(world, o);
        huntingSalon(world, o);
        library(world, o);
        globeBookCabinet(world, o);
        geographyCabinet(world, o);
        porcelainHall(world, o);
        marbleSalon(world, o);
        galleryOfArt(world, o);
        accountingOffice(world, o);
        musicChamber(world, o);
        blueSalon(world, o);
        banquetHall(world, o);
    }

    private static void antechamber(ServerWorld world, BlockPos o) {
        Room r = ANTECHAMBER;
        architecturalFrame(world, o, r, Blocks.RED_NETHER_BRICKS.getDefaultState(), GOLD, PALE, 5);
        ceilingCoffers(world, o, r, Blocks.DARK_PRISMARINE.getDefaultState(), GOLD, Blocks.RED_TERRACOTTA.getDefaultState(), 5);
        processionalCarpet(world, o, r, 5, Blocks.RED_CARPET.getDefaultState());

        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            consoleX(world, o, r.minX() + 3, r.floorY() + 2, z, Direction.EAST, DARK, GOLD);
            consoleX(world, o, r.maxX() - 3, r.floorY() + 2, z, Direction.WEST, DARK, GOLD);
            wallCandelabra(world, o, r.minX() + 2, r.floorY() + 8, z, Direction.EAST, GOLD);
            wallCandelabra(world, o, r.maxX() - 2, r.floorY() + 8, z, Direction.WEST, GOLD);
        }
        for (int z : new int[]{r.centerZ() - 7, r.centerZ(), r.centerZ() + 7}) {
            chandelier(world, o, r.centerX(), r.ceilingY() - 1, z, 7, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        }
        doubleBench(world, o, r.centerX(), r.floorY() + 2, r.centerZ() - 10, Direction.SOUTH,
                Blocks.RED_TERRACOTTA.getDefaultState());
        doubleBench(world, o, r.centerX(), r.floorY() + 2, r.centerZ() + 10, Direction.NORTH,
                Blocks.RED_TERRACOTTA.getDefaultState());
        framedReliefZ(world, o, r.centerX(), r.floorY() + 11, r.maxZ() - 1, 11, 9,
                GOLD, Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.RED_GLAZED_TERRACOTTA.getDefaultState());
    }

    private static void audienceChamber(ServerWorld world, BlockPos o) {
        Room r = AUDIENCE_CHAMBER;
        architecturalFrame(world, o, r, Blocks.RED_NETHER_BRICKS.getDefaultState(), GOLD, Blocks.POLISHED_BLACKSTONE.getDefaultState(), 5);
        ceilingCoffers(world, o, r, DARK, GOLD, Blocks.RED_TERRACOTTA.getDefaultState(), 5);
        processionalCarpet(world, o, r, 5, Blocks.RED_CARPET.getDefaultState());

        int z = r.maxZ() - 2;
        fill(world, o, r.centerX() - 7, r.floorY() + 1, z - 3,
                r.centerX() + 7, r.floorY() + 2, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
        fill(world, o, r.centerX() - 5, r.floorY() + 3, z - 1,
                r.centerX() + 5, r.floorY() + 15, z, Blocks.RED_TERRACOTTA.getDefaultState());
        for (int x : new int[]{r.centerX() - 7, r.centerX() + 7}) {
            ornateColumn(world, o, x, r.floorY() + 2, z - 1, 17, PALE, GOLD);
        }
        fill(world, o, r.centerX() - 8, r.floorY() + 17, z - 2,
                r.centerX() + 8, r.floorY() + 19, z, GOLD);
        steppedArchZ(world, o, r.centerX(), r.floorY() + 20, z - 1, 8, 5, COPPER);
        chair(world, o, r.centerX(), r.floorY() + 3, z - 4, Direction.NORTH,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
        world.setBlockState(o.add(r.centerX(), r.floorY() + 5, z - 5), Blocks.RED_BANNER.getDefaultState());

        for (int zz = r.minZ() + 5; zz <= r.centerZ() + 3; zz += 5) {
            doubleBench(world, o, r.minX() + 7, r.floorY() + 2, zz, Direction.EAST,
                    Blocks.RED_TERRACOTTA.getDefaultState());
            doubleBench(world, o, r.maxX() - 7, r.floorY() + 2, zz, Direction.WEST,
                    Blocks.RED_TERRACOTTA.getDefaultState());
        }
        for (int zz : new int[]{r.centerZ() - 6, r.centerZ() + 3}) {
            chandelier(world, o, r.centerX(), r.ceilingY() - 1, zz, 8, GOLD,
                    Blocks.WHITE_STAINED_GLASS.getDefaultState());
        }
    }

    private static void themisHall(ServerWorld world, BlockPos o) {
        Room r = THEMIS_HALL;
        architecturalFrame(world, o, r, Blocks.GREEN_TERRACOTTA.getDefaultState(), GOLD, OXIDIZED, 4);
        glazedCofferGrid(world, o, r, OXIDIZED, GOLD,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 5, Blocks.RED_CARPET.getDefaultState());

        for (int z = r.minZ() + 5; z <= r.centerZ() + 3; z += 4) {
            for (int x : new int[]{r.minX() + 5, r.minX() + 9, r.maxX() - 9, r.maxX() - 5}) {
                chair(world, o, x, r.floorY() + 2, z, Direction.SOUTH,
                        Blocks.BAMBOO_STAIRS.getDefaultState(), GOLD);
            }
        }
        table(world, o, r.centerX() - 7, r.maxZ() - 7, r.centerX() + 7, r.maxZ() - 4,
                r.floorY() + 3, DARK_SLAB, DARK_FENCE);
        for (int x = r.centerX() - 6; x <= r.centerX() + 6; x += 3) {
            chair(world, o, x, r.floorY() + 2, r.maxZ() - 9, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
        }
        framedReliefZ(world, o, r.centerX(), r.floorY() + 11, r.maxZ() - 1, 13, 9,
                GOLD, PALE, Blocks.GREEN_GLAZED_TERRACOTTA.getDefaultState());
        for (int z : new int[]{r.centerZ() - 6, r.centerZ() + 6}) {
            chandelier(world, o, r.centerX(), r.ceilingY() - 1, z, 8, GOLD,
                    Blocks.WHITE_STAINED_GLASS.getDefaultState());
        }
    }

    private static void railingSalon(ServerWorld world, BlockPos o) {
        Room r = RAILING_SALON;
        architecturalFrame(world, o, r, Blocks.SMOOTH_SANDSTONE.getDefaultState(), GOLD,
                Blocks.RED_TERRACOTTA.getDefaultState(), 4);
        ceilingCoffers(world, o, r, Blocks.DARK_PRISMARINE.getDefaultState(), GOLD,
                Blocks.SMOOTH_SANDSTONE.getDefaultState(), 4);
        perimeterBalustrade(world, o, r, GOLD, r.floorY() + 2);

        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            table(world, o, r.minX() + 5, z - 2, r.minX() + 10, z + 2, r.floorY() + 3,
                    GOLD_SLAB, GOLD);
            table(world, o, r.maxX() - 10, z - 2, r.maxX() - 5, z + 2, r.floorY() + 3,
                    GOLD_SLAB, GOLD);
            for (int x : new int[]{r.minX() + 6, r.minX() + 9, r.maxX() - 9, r.maxX() - 6}) {
                chair(world, o, x, r.floorY() + 2, z - 4, Direction.SOUTH,
                        Blocks.BAMBOO_STAIRS.getDefaultState(), GOLD);
                chair(world, o, x, r.floorY() + 2, z + 4, Direction.NORTH,
                        Blocks.BAMBOO_STAIRS.getDefaultState(), GOLD);
            }
        }
        chandelierRow(world, o, r, 5, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void cabinet(ServerWorld world, BlockPos o) {
        Room r = CABINET;
        architecturalFrame(world, o, r, Blocks.DARK_OAK_PLANKS.getDefaultState(), GOLD,
                Blocks.RED_TERRACOTTA.getDefaultState(), 4);
        ceilingCoffers(world, o, r, DARK, GOLD, Blocks.RED_NETHER_BRICKS.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        cabinetZ(world, o, r.minX() + 2, r.maxX() - 2, r.floorY() + 1, r.minZ() + 1, 10, DARK, GOLD, true);
        cabinetZ(world, o, r.minX() + 2, r.maxX() - 2, r.floorY() + 1, r.maxZ() - 1, 10, DARK, GOLD, true);
        cabinetX(world, o, r.minX() + 1, r.floorY() + 1, r.minZ() + 3, r.maxZ() - 3, 10, DARK, GOLD, true);
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 5) {
            world.setBlockState(o.add(x, r.floorY() + 4, r.minZ() + 2), Blocks.LECTERN.getDefaultState());
        }
        table(world, o, r.centerX() - 5, r.centerZ() - 3, r.centerX() + 5, r.centerZ() + 3,
                r.floorY() + 3, DARK_SLAB, DARK_FENCE);
        for (int x = r.centerX() - 4; x <= r.centerX() + 4; x += 4) {
            chair(world, o, x, r.floorY() + 2, r.centerZ() - 5, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
            chair(world, o, x, r.floorY() + 2, r.centerZ() + 5, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
        }
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, GOLD,
                Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void sallaTerrena(ServerWorld world, BlockPos o) {
        Room r = SALLA_TERRENA;
        architecturalFrame(world, o, r, WHITE, OXIDIZED, PALE, 4);
        glazedCofferGrid(world, o, r, OXIDIZED, GOLD,
                Blocks.WHITE_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            indoorTree(world, o, r.minX() + 5, r.floorY() + 1, z, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            indoorTree(world, o, r.maxX() - 5, r.floorY() + 1, z, Blocks.AZALEA_LEAVES.getDefaultState());
            ornamentalUrn(world, o, r.minX() + 9, r.floorY() + 1, z, Blocks.PINK_TULIP.getDefaultState());
            ornamentalUrn(world, o, r.maxX() - 9, r.floorY() + 1, z, Blocks.WHITE_TULIP.getDefaultState());
        }
        for (int z : new int[]{r.centerZ() - 5, r.centerZ() + 5}) {
            doubleBench(world, o, r.centerX(), r.floorY() + 2, z, Direction.NORTH,
                    Blocks.SMOOTH_SANDSTONE.getDefaultState());
        }
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void coatOfArmsHall(ServerWorld world, BlockPos o) {
        Room r = COAT_OF_ARMS_HALL;
        architecturalFrame(world, o, r, PALE, COPPER, Blocks.DEEPSLATE_BRICKS.getDefaultState(), 4);
        ceilingCoffers(world, o, r, PALE, COPPER, Blocks.POLISHED_DEEPSLATE.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        coatOfArms(world, o, r.centerX(), r.floorY() + 4, r.maxZ() - 1);
        framedReliefZ(world, o, r.centerX() - 7, r.floorY() + 5, r.maxZ() - 1, 5, 6,
                COPPER, PALE, Blocks.RED_GLAZED_TERRACOTTA.getDefaultState());
        framedReliefZ(world, o, r.centerX() + 7, r.floorY() + 5, r.maxZ() - 1, 5, 6,
                COPPER, PALE, Blocks.RED_GLAZED_TERRACOTTA.getDefaultState());
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            ornateColumn(world, o, r.minX() + 3, r.floorY() + 1, z, 10, PALE, COPPER);
            ornateColumn(world, o, r.maxX() - 3, r.floorY() + 1, z, 10, PALE, COPPER);
        }
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, COPPER,
                Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void bloomingSalon(ServerWorld world, BlockPos o) {
        Room r = BLOOMING_SALON;
        architecturalFrame(world, o, r, Blocks.PINK_TERRACOTTA.getDefaultState(), GOLD, OXIDIZED, 4);
        ceilingCoffers(world, o, r, Blocks.WHITE_TERRACOTTA.getDefaultState(), OXIDIZED,
                Blocks.CYAN_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            indoorTree(world, o, r.minX() + 5, r.floorY() + 1, z, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            indoorTree(world, o, r.maxX() - 5, r.floorY() + 1, z, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            ornamentalUrn(world, o, r.minX() + 9, r.floorY() + 1, z, Blocks.PINK_TULIP.getDefaultState());
            ornamentalUrn(world, o, r.maxX() - 9, r.floorY() + 1, z, Blocks.OXEYE_DAISY.getDefaultState());
        }
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x += 4) {
            world.setBlockState(o.add(x, r.ceilingY() - 2, r.minZ() + 2), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            world.setBlockState(o.add(x, r.ceilingY() - 2, r.maxZ() - 2), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
        }
        doubleBench(world, o, r.centerX(), r.floorY() + 2, r.centerZ() - 4, Direction.SOUTH,
                Blocks.CHERRY_PLANKS.getDefaultState());
        doubleBench(world, o, r.centerX(), r.floorY() + 2, r.centerZ() + 4, Direction.NORTH,
                Blocks.CHERRY_PLANKS.getDefaultState());
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, GOLD,
                Blocks.PINK_STAINED_GLASS.getDefaultState());
    }

    private static void huntingSalon(ServerWorld world, BlockPos o) {
        Room r = HUNTING_SALON;
        architecturalFrame(world, o, r, Blocks.BROWN_TERRACOTTA.getDefaultState(),
                Blocks.STRIPPED_SPRUCE_WOOD.getDefaultState(), DARK, 4);
        ceilingCoffers(world, o, r, DARK, Blocks.STRIPPED_SPRUCE_WOOD.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            trophyMountX(world, o, r.maxX() - 2, r.floorY() + 6, z, Direction.WEST);
            trophyMountX(world, o, r.minX() + 2, r.floorY() + 6, z, Direction.EAST);
        }
        fireplaceZ(world, o, r.centerX(), r.floorY() + 1, r.maxZ() - 1,
                Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Blocks.MAGMA_BLOCK.getDefaultState());
        table(world, o, r.centerX() - 4, r.centerZ() - 2, r.centerX() + 4, r.centerZ() + 2,
                r.floorY() + 3, DARK_SLAB, Blocks.SPRUCE_FENCE.getDefaultState());
        for (int x : new int[]{r.centerX() - 4, r.centerX(), r.centerX() + 4}) {
            chair(world, o, x, r.floorY() + 2, r.centerZ() - 5, Direction.SOUTH,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
            chair(world, o, x, r.floorY() + 2, r.centerZ() + 5, Direction.NORTH,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        }
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5,
                Blocks.STRIPPED_SPRUCE_WOOD.getDefaultState(), Blocks.ORANGE_STAINED_GLASS.getDefaultState());
    }

    private static void library(ServerWorld world, BlockPos o) {
        Room r = LIBRARY;
        architecturalFrame(world, o, r, DARK, GOLD, OXIDIZED, 4);
        glazedCofferGrid(world, o, r, OXIDIZED, GOLD,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 2; z <= r.maxZ() - 5; z += 4) {
            bookBayX(world, o, r.minX() + 1, r.floorY() + 1, z, Direction.EAST, 10);
            bookBayX(world, o, r.maxX() - 1, r.floorY() + 1, z, Direction.WEST, 10);
        }
        fill(world, o, r.minX() + 3, r.floorY() + 7, r.minZ() + 2,
                r.maxX() - 3, r.floorY() + 7, r.minZ() + 4, DARK_SLAB);
        fill(world, o, r.minX() + 3, r.floorY() + 8, r.minZ() + 2,
                r.maxX() - 3, r.floorY() + 8, r.minZ() + 2, GOLD);
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
            world.setBlockState(o.add(x, r.floorY() + 8, r.minZ() + 3), Blocks.BAMBOO_FENCE.getDefaultState());
        }
        table(world, o, r.centerX() - 6, r.centerZ() - 3, r.centerX() + 6, r.centerZ() + 3,
                r.floorY() + 3, DARK_SLAB, DARK_FENCE);
        for (int x = r.centerX() - 5; x <= r.centerX() + 5; x += 5) {
            chair(world, o, x, r.floorY() + 2, r.centerZ() - 5, Direction.SOUTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
            chair(world, o, x, r.floorY() + 2, r.centerZ() + 5, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
        }
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void globeBookCabinet(ServerWorld world, BlockPos o) {
        Room r = GLOBE_BOOK_CABINET;
        architecturalFrame(world, o, r, DARK, GOLD, OXIDIZED, 4);
        glazedCofferGrid(world, o, r, OXIDIZED, GOLD,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        cabinetX(world, o, r.minX() + 1, r.floorY() + 1, r.minZ() + 2, r.maxZ() - 2, 10, DARK, GOLD, true);
        cabinetX(world, o, r.maxX() - 1, r.floorY() + 1, r.minZ() + 2, r.maxZ() - 2, 10, DARK, GOLD, true);
        cabinetZ(world, o, r.minX() + 2, r.maxX() - 2, r.floorY() + 1, r.maxZ() - 1, 10, DARK, GOLD, true);
        globe(world, o, r.centerX() + 4, r.floorY() + 2, r.centerZ());
        table(world, o, r.centerX() - 7, r.centerZ() - 3, r.centerX(), r.centerZ() + 3,
                r.floorY() + 3, DARK_SLAB, DARK_FENCE);
        world.setBlockState(o.add(r.centerX() - 4, r.floorY() + 4, r.centerZ()), Blocks.LECTERN.getDefaultState());
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void geographyCabinet(ServerWorld world, BlockPos o) {
        Room r = GEOGRAPHY_CABINET;
        architecturalFrame(world, o, r, Blocks.DARK_OAK_PLANKS.getDefaultState(), GOLD,
                Blocks.CYAN_TERRACOTTA.getDefaultState(), 4);
        ceilingCoffers(world, o, r, DARK, GOLD, Blocks.BLUE_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        mapWallZ(world, o, r.centerX(), r.floorY() + 4, r.maxZ() - 1, 15, 8);
        globe(world, o, r.centerX() + 5, r.floorY() + 2, r.centerZ() - 2);
        globe(world, o, r.centerX() - 5, r.floorY() + 2, r.centerZ() + 3);
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            table(world, o, r.minX() + 4, z - 2, r.minX() + 10, z + 2, r.floorY() + 3,
                    DARK_SLAB, DARK_FENCE);
            world.setBlockState(o.add(r.minX() + 7, r.floorY() + 4, z), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        }
        cabinetX(world, o, r.maxX() - 1, r.floorY() + 1, r.minZ() + 3, r.maxZ() - 3, 10, DARK, GOLD, true);
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, GOLD,
                Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void porcelainHall(ServerWorld world, BlockPos o) {
        Room r = PORCELAIN_HALL;
        architecturalFrame(world, o, r, Blocks.WHITE_TERRACOTTA.getDefaultState(), GOLD,
                Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), 4);
        glazedCofferGrid(world, o, r, PALE, GOLD,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.WHITE_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            porcelainVitrineX(world, o, r.minX() + 2, r.floorY() + 1, z, Direction.EAST);
            porcelainVitrineX(world, o, r.maxX() - 2, r.floorY() + 1, z, Direction.WEST);
        }
        for (int x : new int[]{r.centerX() - 5, r.centerX() + 5}) {
            displayPedestal(world, o, x, r.floorY() + 1, r.centerZ(), Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState());
        }
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void marbleSalon(ServerWorld world, BlockPos o) {
        Room r = MARBLE_SALON;
        architecturalFrame(world, o, r, Blocks.CALCITE.getDefaultState(), GOLD,
                Blocks.POLISHED_DIORITE.getDefaultState(), 5);
        ceilingCoffers(world, o, r, Blocks.CALCITE.getDefaultState(), Blocks.AMETHYST_BLOCK.getDefaultState(),
                Blocks.PURPLE_TERRACOTTA.getDefaultState(), 5);
        processionalCarpet(world, o, r, 5, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += 6) {
            ornateColumn(world, o, r.minX() + 3, r.floorY() + 1, z, 17, PALE, GOLD);
            ornateColumn(world, o, r.maxX() - 3, r.floorY() + 1, z, 17, PALE, GOLD);
        }
        for (int z : new int[]{r.centerZ() - 7, r.centerZ() + 7}) {
            statue(world, o, r.minX() + 7, r.floorY() + 1, z);
            statue(world, o, r.maxX() - 7, r.floorY() + 1, z);
        }
        for (int z : new int[]{r.centerZ() - 6, r.centerZ() + 6}) {
            chandelier(world, o, r.centerX(), r.ceilingY() - 1, z, 8, GOLD,
                    Blocks.WHITE_STAINED_GLASS.getDefaultState());
        }
    }

    private static void galleryOfArt(ServerWorld world, BlockPos o) {
        Room r = GALLERY_OF_ART;
        architecturalFrame(world, o, r, Blocks.WHITE_TERRACOTTA.getDefaultState(), GOLD,
                Blocks.POLISHED_DIORITE.getDefaultState(), 4);
        glazedCofferGrid(world, o, r, PALE, GOLD,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        int variant = 0;
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
            artPanelX(world, o, r.minX() + 1, r.floorY() + 4, z, variant++);
            artPanelX(world, o, r.maxX() - 1, r.floorY() + 4, z, variant++);
        }
        for (int z : new int[]{r.minZ() + 6, r.centerZ(), r.maxZ() - 6}) {
            statue(world, o, r.centerX() - 4, r.floorY() + 1, z);
            statue(world, o, r.centerX() + 4, r.floorY() + 1, z);
        }
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void accountingOffice(ServerWorld world, BlockPos o) {
        Room r = ACCOUNTING_OFFICE;
        architecturalFrame(world, o, r, DARK, GOLD, Blocks.WARPED_PLANKS.getDefaultState(), 4);
        ceilingCoffers(world, o, r, DARK, GOLD, Blocks.RED_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            officeDesk(world, o, r.minX() + 5, r.floorY() + 2, z, Direction.EAST);
            officeDesk(world, o, r.maxX() - 5, r.floorY() + 2, z, Direction.WEST);
        }
        cabinetZ(world, o, r.minX() + 2, r.maxX() - 2, r.floorY() + 1, r.maxZ() - 1, 10, DARK, GOLD, true);
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
            world.setBlockState(o.add(x, r.floorY() + 2, r.maxZ() - 3), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(x, r.floorY() + 6, r.maxZ() - 2), Blocks.CHISELED_BOOKSHELF.getDefaultState());
        }
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, GOLD,
                Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void musicChamber(ServerWorld world, BlockPos o) {
        Room r = MUSIC_CHAMBER;
        architecturalFrame(world, o, r, Blocks.RED_NETHER_BRICKS.getDefaultState(), GOLD,
                Blocks.SMOOTH_SANDSTONE.getDefaultState(), 4);
        ceilingCoffers(world, o, r, Blocks.SMOOTH_SANDSTONE.getDefaultState(), GOLD,
                Blocks.ORANGE_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        fill(world, o, r.minX() + 3, r.floorY() + 1, r.minZ() + 3,
                r.maxX() - 3, r.floorY() + 2, r.minZ() + 10, DARK);
        harpsichord(world, o, r.centerX(), r.floorY() + 4, r.minZ() + 7, Direction.SOUTH);
        for (int z : new int[]{r.centerZ() + 2, r.maxZ() - 5}) {
            for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
                chair(world, o, x, r.floorY() + 2, z, Direction.NORTH,
                        Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
            }
        }
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            framedReliefX(world, o, r.minX() + 1, r.floorY() + 4, z, 5, 7,
                    GOLD, Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.BROWN_GLAZED_TERRACOTTA.getDefaultState());
            framedReliefX(world, o, r.maxX() - 1, r.floorY() + 4, z, 5, 7,
                    GOLD, Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.ORANGE_GLAZED_TERRACOTTA.getDefaultState());
        }
        chandelierRow(world, o, r, 6, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void blueSalon(ServerWorld world, BlockPos o) {
        Room r = BLUE_SALON;
        architecturalFrame(world, o, r, Blocks.BLUE_TERRACOTTA.getDefaultState(), GOLD,
                Blocks.CYAN_TERRACOTTA.getDefaultState(), 4);
        ceilingCoffers(world, o, r, Blocks.DARK_PRISMARINE.getDefaultState(), GOLD,
                Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), 4);
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            framedReliefX(world, o, r.minX() + 1, r.floorY() + 4, z, 5, 7,
                    GOLD, Blocks.CYAN_TERRACOTTA.getDefaultState(), Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.getDefaultState());
            framedReliefX(world, o, r.maxX() - 1, r.floorY() + 4, z, 5, 7,
                    GOLD, Blocks.BLUE_TERRACOTTA.getDefaultState(), Blocks.CYAN_GLAZED_TERRACOTTA.getDefaultState());
        }
        for (int z : new int[]{r.minZ() + 5, r.centerZ(), r.maxZ() - 5}) {
            doubleBench(world, o, r.minX() + 7, r.floorY() + 2, z, Direction.EAST,
                    Blocks.BLUE_TERRACOTTA.getDefaultState());
            doubleBench(world, o, r.maxX() - 7, r.floorY() + 2, z, Direction.WEST,
                    Blocks.CYAN_TERRACOTTA.getDefaultState());
            lowSalonTable(world, o, r.centerX(), r.floorY() + 2, z);
        }
        chandelier(world, o, r.centerX(), r.ceilingY() - 1, r.centerZ(), 5, GOLD,
                Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void banquetHall(ServerWorld world, BlockPos o) {
        Room r = BANQUET_HALL;
        architecturalFrame(world, o, r, Blocks.SMOOTH_SANDSTONE.getDefaultState(), GOLD,
                Blocks.POLISHED_GRANITE.getDefaultState(), 4);
        glazedCofferGrid(world, o, r, GOLD, PALE,
                Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        processionalCarpet(world, o, r, 3, Blocks.RED_CARPET.getDefaultState());
        int z1 = r.minZ() + 4;
        int z2 = r.maxZ() - 4;
        fill(world, o, r.centerX() - 5, r.floorY() + 3, z1,
                r.centerX() + 5, r.floorY() + 3, z2, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        for (int z = z1; z <= z2; z += 3) {
            for (int x : new int[]{r.centerX() - 6, r.centerX() + 6}) {
                chair(world, o, x, r.floorY() + 2, z,
                        x < r.centerX() ? Direction.WEST : Direction.EAST,
                        Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
            }
            banquetPlaceSetting(world, o, r.centerX() - 3, r.floorY() + 4, z);
            banquetPlaceSetting(world, o, r.centerX() + 3, r.floorY() + 4, z);
        }
        sideboardZ(world, o, r.minX() + 3, r.floorY() + 2, r.centerZ(), Direction.EAST);
        sideboardZ(world, o, r.maxX() - 3, r.floorY() + 2, r.centerZ(), Direction.WEST);
        chandelierRow(world, o, r, 5, GOLD, Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void architecturalFrame(ServerWorld world, BlockPos o, Room r,
                                           BlockState wallAccent, BlockState trim, BlockState pilaster,
                                           int spacing) {
        int y0 = r.floorY() + 1;
        int top = r.ceilingY() - 1;
        int lowBand = Math.min(top - 2, y0 + 3);
        int highBand = Math.max(lowBand + 2, top - 3);
        for (int y : new int[]{lowBand, highBand, top}) {
            fill(world, o, r.minX() + 1, y, r.minZ() + 1, r.maxX() - 1, y, r.minZ() + 1, trim);
            fill(world, o, r.minX() + 1, y, r.maxZ() - 1, r.maxX() - 1, y, r.maxZ() - 1, trim);
            fill(world, o, r.minX() + 1, y, r.minZ() + 1, r.minX() + 1, y, r.maxZ() - 1, trim);
            fill(world, o, r.maxX() - 1, y, r.minZ() + 1, r.maxX() - 1, y, r.maxZ() - 1, trim);
        }
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x += spacing) {
            ornateColumn(world, o, x, y0, r.minZ() + 2, Math.max(5, top - y0 - 1), pilaster, trim);
            ornateColumn(world, o, x, y0, r.maxZ() - 2, Math.max(5, top - y0 - 1), pilaster, trim);
            if (x + 2 <= r.maxX() - 2) {
                world.setBlockState(o.add(x + 1, lowBand + 1, r.minZ() + 2), wallAccent);
                world.setBlockState(o.add(x + 1, lowBand + 1, r.maxZ() - 2), wallAccent);
            }
        }
        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += spacing) {
            ornateColumn(world, o, r.minX() + 2, y0, z, Math.max(5, top - y0 - 1), pilaster, trim);
            ornateColumn(world, o, r.maxX() - 2, y0, z, Math.max(5, top - y0 - 1), pilaster, trim);
        }
    }

    private static void ornateColumn(ServerWorld world, BlockPos o, int x, int y, int z, int height,
                                     BlockState shaft, BlockState accent) {
        fill(world, o, x, y, z, x, y + height, z, shaft);
        fill(world, o, x - 1, y, z - 1, x + 1, y, z + 1, accent);
        fill(world, o, x - 1, y + 1, z - 1, x + 1, y + 1, z + 1, shaft);
        fill(world, o, x - 1, y + height - 1, z - 1, x + 1, y + height - 1, z + 1, shaft);
        fill(world, o, x - 1, y + height, z - 1, x + 1, y + height, z + 1, accent);
    }

    private static void ceilingCoffers(ServerWorld world, BlockPos o, Room r,
                                       BlockState field, BlockState beam, BlockState boss, int spacing) {
        int y = r.ceilingY() - 1;
        fill(world, o, r.minX() + 2, y, r.minZ() + 2, r.maxX() - 2, y, r.maxZ() - 2, field);
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x += spacing) {
            fill(world, o, x, y - 1, r.minZ() + 2, x, y, r.maxZ() - 2, beam);
        }
        for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += spacing) {
            fill(world, o, r.minX() + 2, y - 1, z, r.maxX() - 2, y, z, beam);
        }
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x += spacing) {
            for (int z = r.minZ() + 3; z <= r.maxZ() - 3; z += spacing) {
                world.setBlockState(o.add(x, y - 2, z), boss);
            }
        }
    }

    private static void glazedCofferGrid(ServerWorld world, BlockPos o, Room r,
                                         BlockState frame, BlockState accent, BlockState glassA, BlockState glassB) {
        int y = r.ceilingY() - 1;
        for (int x = r.minX() + 2; x <= r.maxX() - 2; x++) {
            for (int z = r.minZ() + 2; z <= r.maxZ() - 2; z++) {
                boolean beam = Math.floorMod(x - r.minX(), 4) == 0 || Math.floorMod(z - r.minZ(), 4) == 0;
                BlockState state = beam ? frame : (Math.floorMod(x * 7 + z * 11, 9) == 0 ? glassB : glassA);
                world.setBlockState(o.add(x, y, z), state);
            }
        }
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 8) {
            for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 8) {
                world.setBlockState(o.add(x, y - 1, z), accent);
            }
        }
    }

    private static void processionalCarpet(ServerWorld world, BlockPos o, Room r, int width, BlockState carpet) {
        int half = width / 2;
        fill(world, o, r.centerX() - half, r.floorY() + 1, r.minZ() + 2,
                r.centerX() + half, r.floorY() + 1, r.maxZ() - 2, carpet);
    }

    private static void chandelierRow(ServerWorld world, BlockPos o, Room r, int spacing,
                                      BlockState metal, BlockState glass) {
        for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += spacing) {
            chandelier(world, o, r.centerX(), r.ceilingY() - 1, z,
                    Math.max(4, Math.min(7, (r.ceilingY() - r.floorY()) / 3)), metal, glass);
        }
    }

    private static void consoleX(ServerWorld world, BlockPos o, int x, int y, int z, Direction facing,
                                 BlockState wood, BlockState trim) {
        int dx = facing.getOffsetX();
        fill(world, o, x, y, z - 2, x, y, z + 2, wood);
        world.setBlockState(o.add(x - dx, y - 1, z - 2), trim);
        world.setBlockState(o.add(x - dx, y - 1, z + 2), trim);
        world.setBlockState(o.add(x - dx, y + 1, z), Blocks.DECORATED_POT.getDefaultState());
    }

    private static void doubleBench(ServerWorld world, BlockPos o, int centerX, int y, int z,
                                    Direction facing, BlockState upholstery) {
        Direction.Axis axis = facing.getAxis();
        if (axis == Direction.Axis.Z) {
            fill(world, o, centerX - 3, y, z, centerX + 3, y, z, DARK_SLAB);
            fill(world, o, centerX - 3, y + 1, z - facing.getOffsetZ(), centerX + 3, y + 1, z - facing.getOffsetZ(), upholstery);
            world.setBlockState(o.add(centerX - 3, y - 1, z), DARK_FENCE);
            world.setBlockState(o.add(centerX + 3, y - 1, z), DARK_FENCE);
        } else {
            fill(world, o, centerX, y, z - 3, centerX, y, z + 3, DARK_SLAB);
            fill(world, o, centerX - facing.getOffsetX(), y + 1, z - 3, centerX - facing.getOffsetX(), y + 1, z + 3, upholstery);
            world.setBlockState(o.add(centerX, y - 1, z - 3), DARK_FENCE);
            world.setBlockState(o.add(centerX, y - 1, z + 3), DARK_FENCE);
        }
    }

    private static void framedReliefZ(ServerWorld world, BlockPos o, int centerX, int y, int z,
                                      int width, int height, BlockState frame, BlockState field, BlockState motif) {
        int half = width / 2;
        fill(world, o, centerX - half, y, z, centerX + half, y + height, z, frame);
        fill(world, o, centerX - half + 1, y + 1, z - 1, centerX + half - 1, y + height - 1, z - 1, field);
        for (int dx = -half + 2; dx <= half - 2; dx += 2) {
            world.setBlockState(o.add(centerX + dx, y + height / 2, z - 2), motif);
        }
        world.setBlockState(o.add(centerX, y + height - 2, z - 2), motif);
    }

    private static void framedReliefX(ServerWorld world, BlockPos o, int x, int y, int centerZ,
                                      int width, int height, BlockState frame, BlockState field, BlockState motif) {
        int half = width / 2;
        fill(world, o, x, y, centerZ - half, x, y + height, centerZ + half, frame);
        fill(world, o, x + (x < 0 ? 1 : -1), y + 1, centerZ - half + 1,
                x + (x < 0 ? 1 : -1), y + height - 1, centerZ + half - 1, field);
        int ix = x + (x < 0 ? 2 : -2);
        for (int dz = -half + 2; dz <= half - 2; dz += 2) {
            world.setBlockState(o.add(ix, y + height / 2, centerZ + dz), motif);
        }
    }

    private static void perimeterBalustrade(ServerWorld world, BlockPos o, Room r, BlockState rail, int y) {
        for (int x = r.minX() + 3; x <= r.maxX() - 3; x++) {
            if (x % 2 == 0) {
                world.setBlockState(o.add(x, y, r.minZ() + 3), rail);
                world.setBlockState(o.add(x, y, r.maxZ() - 3), rail);
            }
        }
        fill(world, o, r.minX() + 3, y + 1, r.minZ() + 3, r.maxX() - 3, y + 1, r.minZ() + 3, GOLD_TRAP);
        fill(world, o, r.minX() + 3, y + 1, r.maxZ() - 3, r.maxX() - 3, y + 1, r.maxZ() - 3, GOLD_TRAP);
    }

    private static void indoorTree(ServerWorld world, BlockPos o, int x, int y, int z, BlockState leaves) {
        fill(world, o, x, y, z, x, y + 4, z, Blocks.STRIPPED_CHERRY_LOG.getDefaultState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                world.setBlockState(o.add(x + dx, y + 5, z + dz), leaves);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    world.setBlockState(o.add(x + dx, y + 6, z + dz), leaves);
                }
            }
        }
        fill(world, o, x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, Blocks.POLISHED_GRANITE.getDefaultState());
    }

    private static void ornamentalUrn(ServerWorld world, BlockPos o, int x, int y, int z, BlockState flower) {
        world.setBlockState(o.add(x, y, z), Blocks.POLISHED_GRANITE.getDefaultState());
        world.setBlockState(o.add(x, y + 1, z), Blocks.DECORATED_POT.getDefaultState());
        world.setBlockState(o.add(x, y + 2, z), flower);
    }

    private static void trophyMountX(ServerWorld world, BlockPos o, int x, int y, int z, Direction inward) {
        int dx = inward.getOffsetX();
        world.setBlockState(o.add(x, y, z), Blocks.TARGET.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 1, z), Blocks.SPRUCE_FENCE.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 2, z - 1), Blocks.SPRUCE_FENCE.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 2, z + 1), Blocks.SPRUCE_FENCE.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 3, z - 2), Blocks.SPRUCE_FENCE.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 3, z + 2), Blocks.SPRUCE_FENCE.getDefaultState());
        fill(world, o, x, y - 2, z - 2, x, y - 1, z + 2, Blocks.DARK_OAK_PLANKS.getDefaultState());
    }

    private static void fireplaceZ(ServerWorld world, BlockPos o, int centerX, int y, int z,
                                   BlockState surround, BlockState hearth) {
        fill(world, o, centerX - 4, y, z - 1, centerX + 4, y + 7, z, surround);
        clear(world, o, centerX - 2, y + 1, z - 2, centerX + 2, y + 4, z - 1);
        fill(world, o, centerX - 2, y, z - 2, centerX + 2, y, z - 1, hearth);
        fill(world, o, centerX - 5, y + 7, z - 1, centerX + 5, y + 8, z, Blocks.STRIPPED_DARK_OAK_WOOD.getDefaultState());
        for (int x = centerX - 2; x <= centerX + 2; x += 2) {
            world.setBlockState(o.add(x, y + 2, z - 2), litLantern(false));
        }
    }

    private static void bookBayX(ServerWorld world, BlockPos o, int x, int y, int z, Direction inward, int height) {
        int dx = inward.getOffsetX();
        fill(world, o, x, y, z, x, y + height, z + 3, DARK);
        fill(world, o, x + dx, y + 1, z, x + dx, y + height - 1, z + 3, Blocks.BOOKSHELF.getDefaultState());
        for (int yy = y + 2; yy < y + height; yy += 3) {
            fill(world, o, x + dx * 2, yy, z, x + dx * 2, yy, z + 3, GOLD_SLAB);
        }
        world.setBlockState(o.add(x + dx * 2, y + 3, z + 1), Blocks.LADDER.getDefaultState());
        world.setBlockState(o.add(x + dx * 2, y + 4, z + 1), Blocks.LADDER.getDefaultState());
    }

    private static void mapWallZ(ServerWorld world, BlockPos o, int centerX, int y, int z, int width, int height) {
        int half = width / 2;
        fill(world, o, centerX - half - 1, y - 1, z, centerX + half + 1, y + height + 1, z, GOLD);
        BlockState[] colors = {
                Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.BLUE_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(), Blocks.LIME_TERRACOTTA.getDefaultState(),
                Blocks.SANDSTONE.getDefaultState(), Blocks.BROWN_TERRACOTTA.getDefaultState()
        };
        for (int x = centerX - half; x <= centerX + half; x++) {
            for (int yy = y; yy <= y + height; yy++) {
                int index = Math.floorMod(x * 17 + yy * 11 + (x * yy), colors.length);
                world.setBlockState(o.add(x, yy, z - 1), colors[index]);
            }
        }
    }

    private static void porcelainVitrineX(ServerWorld world, BlockPos o, int x, int y, int z, Direction inward) {
        int dx = inward.getOffsetX();
        fill(world, o, x, y, z - 2, x, y + 8, z + 2, PALE);
        fill(world, o, x + dx, y + 1, z - 1, x + dx, y + 7, z + 1, Blocks.GLASS.getDefaultState());
        for (int yy = y + 2; yy <= y + 6; yy += 2) {
            fill(world, o, x + dx * 2, yy, z - 1, x + dx * 2, yy, z + 1, GOLD_SLAB);
            world.setBlockState(o.add(x + dx * 2, yy + 1, z),
                    yy % 4 == 0 ? Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState() : Blocks.DECORATED_POT.getDefaultState());
        }
    }

    private static void displayPedestal(ServerWorld world, BlockPos o, int x, int y, int z, BlockState display) {
        fill(world, o, x - 1, y, z - 1, x + 1, y, z + 1, PALE);
        world.setBlockState(o.add(x, y + 1, z), Blocks.DIORITE_WALL.getDefaultState());
        world.setBlockState(o.add(x, y + 2, z), display);
        world.setBlockState(o.add(x, y + 3, z), Blocks.WHITE_STAINED_GLASS.getDefaultState());
    }

    private static void statue(ServerWorld world, BlockPos o, int x, int y, int z) {
        fill(world, o, x - 1, y, z - 1, x + 1, y, z + 1, Blocks.POLISHED_DIORITE.getDefaultState());
        fill(world, o, x, y + 1, z, x, y + 4, z, Blocks.QUARTZ_PILLAR.getDefaultState());
        world.setBlockState(o.add(x, y + 5, z), Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        world.setBlockState(o.add(x - 1, y + 3, z), Blocks.QUARTZ_STAIRS.getDefaultState());
        world.setBlockState(o.add(x + 1, y + 3, z), Blocks.QUARTZ_STAIRS.getDefaultState());
    }

    private static void artPanelX(ServerWorld world, BlockPos o, int x, int y, int centerZ, int variant) {
        int ix = x < 0 ? x + 1 : x - 1;
        BlockState[] colors = {
                Blocks.RED_TERRACOTTA.getDefaultState(), Blocks.BLUE_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(), Blocks.PURPLE_TERRACOTTA.getDefaultState(),
                Blocks.ORANGE_TERRACOTTA.getDefaultState(), Blocks.CYAN_TERRACOTTA.getDefaultState()
        };
        fill(world, o, x, y, centerZ - 3, x, y + 6, centerZ + 3, DARK);
        for (int yy = y + 1; yy <= y + 5; yy++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                int index = Math.floorMod(variant * 7 + yy * 3 + z * 5, colors.length);
                world.setBlockState(o.add(ix, yy, z), colors[index]);
            }
        }
    }

    private static void officeDesk(ServerWorld world, BlockPos o, int x, int y, int z, Direction facing) {
        int dx = facing.getOffsetX();
        int dz = facing.getOffsetZ();
        fill(world, o, x - 2, y, z - 2, x + 2, y, z + 2, DARK_SLAB);
        world.setBlockState(o.add(x - 2, y - 1, z - 2), DARK_FENCE);
        world.setBlockState(o.add(x + 2, y - 1, z + 2), DARK_FENCE);
        world.setBlockState(o.add(x, y + 1, z), Blocks.LECTERN.getDefaultState());
        world.setBlockState(o.add(x + dx, y + 1, z + dz), Blocks.CRAFTER.getDefaultState());
        chair(world, o, x - dx * 3, y - 1, z - dz * 3, facing,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), GOLD);
    }

    private static void lowSalonTable(ServerWorld world, BlockPos o, int x, int y, int z) {
        fill(world, o, x - 2, y, z - 2, x + 2, y, z + 2, Blocks.DARK_OAK_SLAB.getDefaultState());
        world.setBlockState(o.add(x, y - 1, z), DARK_FENCE);
        world.setBlockState(o.add(x - 1, y + 1, z), Blocks.POTTED_BLUE_ORCHID.getDefaultState());
        world.setBlockState(o.add(x + 1, y + 1, z), litCandle(Blocks.CANDLE.getDefaultState()));
    }

    private static void sideboardZ(ServerWorld world, BlockPos o, int x, int y, int z, Direction inward) {
        int dx = inward.getOffsetX();
        fill(world, o, x, y, z - 4, x, y + 3, z + 4, DARK);
        for (int zz = z - 3; zz <= z + 3; zz += 2) {
            world.setBlockState(o.add(x + dx, y + 1, zz), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(x + dx, y + 4, zz), litCandle(Blocks.CANDLE.getDefaultState()));
        }
        fill(world, o, x + dx, y + 3, z - 4, x + dx, y + 3, z + 4, GOLD_SLAB);
    }

    private static void banquetPlaceSetting(ServerWorld world, BlockPos o, int x, int y, int z) {
        world.setBlockState(o.add(x, y, z), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState());
        world.setBlockState(o.add(x - 1, y, z), litCandle(Blocks.WHITE_CANDLE.getDefaultState()));
        world.setBlockState(o.add(x + 1, y, z), Blocks.FLOWER_POT.getDefaultState());
    }
}
