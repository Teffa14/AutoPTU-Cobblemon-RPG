package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Authored room program for the courtyard-based Grand Palace V4.
 *
 * V4 deliberately does not run the legacy V1/V3 room pass. Every room is rebuilt at its physical
 * courtyard-plan coordinate, with layered wall architecture, circulation openings, furnishings and
 * a distinct focal composition. The repeated shell primitive is only the construction grammar; the
 * room identity comes from the authored composition below.
 */
final class OurosGrandPalaceV4Rooms {
    private static final BlockState WALL = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState PALE = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState TRIM = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState WOOD = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState WOOD_2 = Blocks.SPRUCE_PLANKS.getDefaultState();
    private static final BlockState FLOOR = Blocks.POLISHED_ANDESITE.getDefaultState();
    private static final BlockState FLOOR_2 = Blocks.CALCITE.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
    private static final BlockState METAL = Blocks.WAXED_CUT_COPPER.getDefaultState();

    private OurosGrandPalaceV4Rooms() {}

    static void buildAll(ServerWorld world, BlockPos o) {
        for (Room room : ceremonialRooms()) buildRoom(world, o, room);
        for (Room room : groundSideRooms()) buildRoom(world, o, room);
        for (Room room : upperSideRooms()) buildRoom(world, o, room);
        buildVerticalCirculation(world, o);
    }

    private static void buildRoom(ServerWorld world, BlockPos o, Room room) {
        BlockState accent = accentFor(room.name());
        roomShell(world, o, room, WALL, PALE, FLOOR, FLOOR_2, Blocks.SMOOTH_QUARTZ.getDefaultState(), TRIM);
        insetCeiling(world, o, room, PALE, Blocks.SMOOTH_QUARTZ.getDefaultState(), accent);
        layerInteriorWalls(world, o, room, accent);
        materialFrieze(world, o, room);
        buildCirculationAndWindows(world, o, room);
        buildBaselineFurniture(world, o, room, accent);
        buildTheme(world, o, room, accent);
    }

    /** A genuine second wall order: pilasters, dado and upper panels sit inside the structural shell. */
    private static void layerInteriorWalls(ServerWorld world, BlockPos o, Room r, BlockState accent) {
        int y1 = r.floorY() + 1;
        int y2 = r.ceilingY() - 1;
        for (int z = r.minZ() + 1; z <= r.maxZ() - 1; z++) {
            BlockState state = Math.floorMod(z - r.minZ(), 5) == 0 ? PALE : WALL;
            fill(world, o, r.minX() + 1, y1, z, r.minX() + 1, y2, z, state);
            fill(world, o, r.maxX() - 1, y1, z, r.maxX() - 1, y2, z, state);
        }
        for (int x = r.minX() + 1; x <= r.maxX() - 1; x++) {
            BlockState state = Math.floorMod(x - r.minX(), 5) == 0 ? PALE : WALL;
            fill(world, o, x, y1, r.minZ() + 1, x, y2, r.minZ() + 1, state);
            fill(world, o, x, y1, r.maxZ() - 1, x, y2, r.maxZ() - 1, state);
        }
        corniceRing(world, o, r, accent, r.floorY() + 5);
        corniceRing(world, o, r, TRIM, Math.max(r.floorY() + 7, r.ceilingY() - 4));
    }

    /** Ten-material collector frieze keeps every salon materially authored rather than palette-flat. */
    private static void materialFrieze(ServerWorld world, BlockPos o, Room r) {
        BlockState[] samples = {
                Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState(),
                Blocks.CUT_COPPER.getDefaultState(),
                Blocks.DARK_OAK_PLANKS.getDefaultState(),
                Blocks.BOOKSHELF.getDefaultState(),
                Blocks.RED_TERRACOTTA.getDefaultState(),
                Blocks.BLUE_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(),
                Blocks.AMETHYST_BLOCK.getDefaultState(),
                Blocks.SEA_LANTERN.getDefaultState(),
                Blocks.GILDED_BLACKSTONE.getDefaultState()
        };
        int z = r.minZ() + 2;
        int y = r.floorY() + 4;
        for (int i = 0; i < samples.length; i++) {
            int x = r.centerX() - 5 + i;
            world.setBlockState(o.add(x, y, z), samples[i]);
        }
    }

    private static void buildCirculationAndWindows(ServerWorld world, BlockPos o, Room r) {
        int doorHeight = r.ceilingY() - r.floorY() > 20 ? 8 : 6;
        if (isCentralPhysical(r)) {
            clearPortalWest(world, o, r, 5, doorHeight);
            clearPortalEast(world, o, r, 5, doorHeight);
            clearPortalNorth(world, o, r, 5, doorHeight);
            clearPortalSouth(world, o, r, 5, doorHeight);
            addWindowOnX(world, o, r, r.minX(), r.minX() + 1, r.centerZ() - 7);
            addWindowOnX(world, o, r, r.maxX(), r.maxX() - 1, r.centerZ() + 7);
            return;
        }

        boolean westWing = r.centerX() < 0;
        if (westWing) {
            clearPortalEast(world, o, r, 5, doorHeight);
            addWindowOnX(world, o, r, r.minX(), r.minX() + 1, r.centerZ() - 6);
            addWindowOnX(world, o, r, r.minX(), r.minX() + 1, r.centerZ() + 6);
        } else {
            clearPortalWest(world, o, r, 5, doorHeight);
            addWindowOnX(world, o, r, r.maxX(), r.maxX() - 1, r.centerZ() - 6);
            addWindowOnX(world, o, r, r.maxX(), r.maxX() - 1, r.centerZ() + 6);
        }

        // Neighboring pavilions connect only through the authored bridge/corridor bands.
        if (r.minZ() > -53) clearPortalNorth(world, o, r, 3, 5);
        if (r.maxZ() < 53) clearPortalSouth(world, o, r, 3, 5);
    }

    private static boolean isCentralPhysical(Room r) {
        return r.minX() == CENTRAL_MIN_X && r.maxX() == CENTRAL_MAX_X;
    }

    private static void addWindowOnX(ServerWorld world, BlockPos o, Room r, int wallX, int innerX, int centerZ) {
        int height = r.ceilingY() - r.floorY() > 20 ? 10 : 7;
        clear(world, o, Math.min(wallX, innerX), r.floorY() + 2, centerZ - 3,
                Math.max(wallX, innerX), r.floorY() + height + 2, centerZ + 3);
        tallWindowX(world, o, wallX, centerZ, r.floorY(), height, METAL, GLASS);
    }

    private static void clearPortalWest(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = r.centerZ();
        clear(world, o, r.minX(), r.floorY() + 1, c - width / 2,
                r.minX() + 1, r.floorY() + height, c + width / 2);
    }

    private static void clearPortalEast(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = r.centerZ();
        clear(world, o, r.maxX() - 1, r.floorY() + 1, c - width / 2,
                r.maxX(), r.floorY() + height, c + width / 2);
    }

    private static void clearPortalNorth(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = r.centerX();
        clear(world, o, c - width / 2, r.floorY() + 1, r.minZ(),
                c + width / 2, r.floorY() + height, r.minZ() + 1);
    }

    private static void clearPortalSouth(ServerWorld world, BlockPos o, Room r, int width, int height) {
        int c = r.centerX();
        clear(world, o, c - width / 2, r.floorY() + 1, r.maxZ() - 1,
                c + width / 2, r.floorY() + height, r.maxZ());
    }

    private static void buildBaselineFurniture(ServerWorld world, BlockPos o, Room r, BlockState accent) {
        int y = r.floorY() + 2;
        table(world, o, r.centerX() - 2, r.centerZ() - 8, r.centerX() + 2, r.centerZ() - 6,
                y, WOOD, Blocks.DARK_OAK_FENCE.getDefaultState());
        table(world, o, r.centerX() - 2, r.centerZ() + 6, r.centerX() + 2, r.centerZ() + 8,
                y, WOOD_2, Blocks.SPRUCE_FENCE.getDefaultState());
        for (int dx : new int[]{-3, 3}) {
            chair(world, o, r.centerX() + dx, y, r.centerZ() - 7, dx < 0 ? Direction.EAST : Direction.WEST,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            chair(world, o, r.centerX() + dx, y, r.centerZ() + 7, dx < 0 ? Direction.EAST : Direction.WEST,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        }
        fill(world, o, r.centerX() - 4, r.floorY() + 1, r.centerZ() - 1,
                r.centerX() + 4, r.floorY() + 1, r.centerZ() + 1, accent);
        chandelier(world, o, r.centerX(), r.ceilingY(), r.centerZ(),
                r.ceilingY() - r.floorY() > 20 ? 8 : 5, METAL, Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildTheme(ServerWorld world, BlockPos o, Room r, BlockState accent) {
        String name = r.name();
        if (name.equals(ANTECHAMBER.name())) buildAntechamber(world, o, r);
        else if (name.equals(AUDIENCE_CHAMBER.name())) buildAudience(world, o, r);
        else if (name.equals(THEMIS_HALL.name())) buildThemis(world, o, r);
        else if (name.equals(MARBLE_SALON.name())) buildMarble(world, o, r);
        else if (name.equals(CABINET.name())) buildCabinet(world, o, r);
        else if (name.equals(SALLA_TERRENA.name())) buildSallaTerrena(world, o, r);
        else if (name.equals(BLOOMING_SALON.name())) buildBlooming(world, o, r);
        else if (name.equals(HUNTING_SALON.name())) buildHunting(world, o, r);
        else if (name.equals(LIBRARY.name())) buildLibrary(world, o, r);
        else if (name.equals(GEOGRAPHY_CABINET.name())) buildGeography(world, o, r);
        else if (name.equals(PORCELAIN_HALL.name())) buildPorcelain(world, o, r);
        else if (name.equals(GALLERY_OF_ART.name())) buildGallery(world, o, r);
        else if (name.equals(RAILING_SALON.name())) buildRailingSalon(world, o, r);
        else if (name.equals(COAT_OF_ARMS_HALL.name())) buildCoatOfArms(world, o, r);
        else if (name.equals(ACCOUNTING_OFFICE.name())) buildAccounting(world, o, r);
        else if (name.equals(MUSIC_CHAMBER.name())) buildMusic(world, o, r);
        else if (name.equals(GLOBE_BOOK_CABINET.name())) buildGlobeBook(world, o, r);
        else if (name.equals(BLUE_SALON.name())) buildBlueSalon(world, o, r);
        else if (name.equals(BANQUET_HALL.name())) buildBanquet(world, o, r);
        else buildDisplayNiches(world, o, r, accent);
    }

    private static void buildAntechamber(ServerWorld world, BlockPos o, Room r) {
        fill(world, o, r.centerX() - 2, 1, r.minZ() + 3, r.centerX() + 2, 1, r.maxZ() - 3,
                Blocks.RED_CARPET.getDefaultState());
        bench(world, o, r.minX() + 4, r.centerZ() - 5, r.minX() + 4, r.centerZ() + 5, 2,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        bench(world, o, r.maxX() - 4, r.centerZ() - 5, r.maxX() - 4, r.centerZ() + 5, 2,
                Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        for (int z : new int[]{r.centerZ() - 7, r.centerZ() + 7})
            chandelier(world, o, r.centerX(), r.ceilingY(), z, 9, METAL, Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildAudience(ServerWorld world, BlockPos o, Room r) {
        fill(world, o, r.centerX() - 6, 1, r.maxZ() - 8, r.centerX() + 6, 2, r.maxZ() - 4,
                Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        fill(world, o, r.centerX() - 4, 3, r.maxZ() - 7, r.centerX() + 4, 3, r.maxZ() - 5,
                Blocks.RED_WOOL.getDefaultState());
        chair(world, o, r.centerX(), 4, r.maxZ() - 6, Direction.NORTH,
                Blocks.QUARTZ_STAIRS.getDefaultState(), Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        for (int x = r.centerX() - 7; x <= r.centerX() + 7; x += 7) {
            fill(world, o, x, 1, r.centerZ() - 2, x, 10, r.centerZ() - 2, PALE);
            world.setBlockState(o.add(x, 11, r.centerZ() - 2), METAL);
        }
    }

    private static void buildThemis(ServerWorld world, BlockPos o, Room r) {
        fill(world, o, r.centerX() - 5, 1, r.centerZ() - 5, r.centerX() + 5, 1, r.centerZ() + 5,
                Blocks.SMOOTH_QUARTZ.getDefaultState());
        fill(world, o, r.centerX() - 2, 2, r.centerZ() - 2, r.centerX() + 2, 4, r.centerZ() + 2,
                Blocks.QUARTZ_BRICKS.getDefaultState());
        fill(world, o, r.centerX(), 5, r.centerZ(), r.centerX(), 12, r.centerZ(),
                Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        fill(world, o, r.centerX() - 2, 9, r.centerZ(), r.centerX() + 2, 9, r.centerZ(), METAL);
        world.setBlockState(o.add(r.centerX() - 2, 8, r.centerZ()), Blocks.CHAIN.getDefaultState());
        world.setBlockState(o.add(r.centerX() + 2, 8, r.centerZ()), Blocks.CHAIN.getDefaultState());
        world.setBlockState(o.add(r.centerX(), 13, r.centerZ()), Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildMarble(ServerWorld world, BlockPos o, Room r) {
        for (int x : new int[]{r.minX() + 5, r.maxX() - 5}) {
            for (int z : new int[]{r.minZ() + 5, r.maxZ() - 5}) {
                fill(world, o, x, 1, z, x, 15, z, Blocks.QUARTZ_PILLAR.getDefaultState());
                fill(world, o, x - 1, 1, z - 1, x + 1, 1, z + 1, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
            }
        }
        fill(world, o, r.centerX() - 6, 1, r.centerZ() - 6, r.centerX() + 6, 1, r.centerZ() + 6,
                Blocks.WHITE_CARPET.getDefaultState());
        pottedPlant(world, o, r.centerX() - 7, 2, r.centerZ(), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        pottedPlant(world, o, r.centerX() + 7, 2, r.centerZ(), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
    }

    private static void buildCabinet(ServerWorld world, BlockPos o, Room r) {
        cabinetZ(world, o, r.minX() + 4, r.centerX() - 2, 2, r.maxZ() - 3, 6, WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), true);
        cabinetZ(world, o, r.centerX() + 2, r.maxX() - 4, 2, r.maxZ() - 3, 6, WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), false);
        for (int x = r.centerX() - 6; x <= r.centerX() + 6; x += 4)
            pottedPlant(world, o, x, 2, r.minZ() + 5, Blocks.POTTED_FERN.getDefaultState());
    }

    private static void buildSallaTerrena(ServerWorld world, BlockPos o, Room r) {
        fill(world, o, r.centerX() - 5, 1, r.centerZ() - 5, r.centerX() + 5, 1, r.centerZ() + 5,
                Blocks.MOSS_BLOCK.getDefaultState());
        for (int x : new int[]{r.centerX() - 5, r.centerX(), r.centerX() + 5}) {
            pottedPlant(world, o, x, 2, r.centerZ(), Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        }
        fill(world, o, r.centerX() - 2, 2, r.centerZ() - 2, r.centerX() + 2, 2, r.centerZ() + 2,
                Blocks.WATER.getDefaultState());
        world.setBlockState(o.add(r.centerX(), 3, r.centerZ()), Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildBlooming(ServerWorld world, BlockPos o, Room r) {
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 4) {
            fill(world, o, x, 2, r.minZ() + 4, x, 7, r.minZ() + 4, Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(o.add(x, 8, r.minZ() + 4), Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            pottedPlant(world, o, x, 2, r.maxZ() - 5, Blocks.POTTED_PINK_TULIP.getDefaultState());
        }
        fill(world, o, r.minX() + 4, 7, r.minZ() + 4, r.maxX() - 4, 7, r.minZ() + 4,
                Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
    }

    private static void buildHunting(ServerWorld world, BlockPos o, Room r) {
        coatOfArms(world, o, r.centerX(), 3, r.maxZ() - 1);
        for (int x : new int[]{r.centerX() - 6, r.centerX(), r.centerX() + 6}) {
            fill(world, o, x - 1, 2, r.minZ() + 3, x + 1, 4, r.minZ() + 3,
                    Blocks.STRIPPED_SPRUCE_LOG.getDefaultState());
            world.setBlockState(o.add(x, 5, r.minZ() + 3), Blocks.SKELETON_SKULL.getDefaultState());
        }
        fill(world, o, r.centerX() - 4, 1, r.centerZ() - 4, r.centerX() + 4, 1, r.centerZ() + 4,
                Blocks.BROWN_CARPET.getDefaultState());
    }

    private static void buildLibrary(ServerWorld world, BlockPos o, Room r) {
        cabinetZ(world, o, r.minX() + 3, r.maxX() - 3, 2, r.minZ() + 2, 8, WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), true);
        cabinetZ(world, o, r.minX() + 3, r.maxX() - 3, 2, r.maxZ() - 2, 8, WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), true);
        for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += 5) {
            table(world, o, r.centerX() - 4, z - 1, r.centerX() + 4, z + 1, 2,
                    Blocks.SPRUCE_SLAB.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        }
    }

    private static void buildGeography(ServerWorld world, BlockPos o, Room r) {
        globe(world, o, r.centerX(), 2, r.centerZ());
        for (int x : new int[]{r.centerX() - 6, r.centerX() + 6}) {
            world.setBlockState(o.add(x, 2, r.centerZ() - 5), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
            world.setBlockState(o.add(x, 2, r.centerZ() + 5), Blocks.LECTERN.getDefaultState());
        }
        wallPanelZ(world, o, r.centerX(), 3, r.maxZ() - 1, 11, 7, METAL,
                Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.GREEN_TERRACOTTA.getDefaultState());
    }

    private static void buildPorcelain(ServerWorld world, BlockPos o, Room r) {
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 4) {
            for (int x : new int[]{r.minX() + 4, r.maxX() - 4}) {
                world.setBlockState(o.add(x, 2, z), Blocks.QUARTZ_BLOCK.getDefaultState());
                world.setBlockState(o.add(x, 3, z), Math.floorMod(z, 8) == 0
                        ? Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState()
                        : Blocks.WHITE_GLAZED_TERRACOTTA.getDefaultState());
                world.setBlockState(o.add(x, 4, z), Blocks.GLASS.getDefaultState());
            }
        }
    }

    private static void buildGallery(ServerWorld world, BlockPos o, Room r) {
        for (int x = r.minX() + 5, variant = 0; x <= r.maxX() - 5; x += 7, variant++) {
            artPanelZ(world, o, x, 3, r.minZ() + 2, variant);
            artPanelZ(world, o, x, 3, r.maxZ() - 1, variant + 3);
        }
        for (int x = r.minX() + 5; x <= r.maxX() - 5; x += 6) {
            fill(world, o, x, 1, r.centerZ(), x, 3, r.centerZ(), Blocks.CHISELED_STONE_BRICKS.getDefaultState());
            world.setBlockState(o.add(x, 4, r.centerZ()), Blocks.AMETHYST_BLOCK.getDefaultState());
        }
    }

    private static void buildRailingSalon(ServerWorld world, BlockPos o, Room r) {
        int y = r.floorY() + 2;
        for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 5) {
            fill(world, o, r.minX() + 4, y, z, r.maxX() - 4, y, z, Blocks.DARK_OAK_SLAB.getDefaultState());
            for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 3)
                world.setBlockState(o.add(x, y + 1, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        }
    }

    private static void buildCoatOfArms(ServerWorld world, BlockPos o, Room r) {
        coatOfArms(world, o, r.centerX(), r.floorY() + 3, r.maxZ() - 1);
        coatOfArms(world, o, r.centerX(), r.floorY() + 3, r.minZ() + 2);
        for (int x : new int[]{r.centerX() - 6, r.centerX() + 6}) {
            world.setBlockState(o.add(x, r.floorY() + 2, r.centerZ()), Blocks.GOLD_BLOCK.getDefaultState());
            world.setBlockState(o.add(x, r.floorY() + 3, r.centerZ()), Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        }
    }

    private static void buildAccounting(ServerWorld world, BlockPos o, Room r) {
        int y = r.floorY() + 2;
        for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += 5) {
            table(world, o, r.centerX() - 6, z - 1, r.centerX() - 1, z + 1, y,
                    WOOD, Blocks.DARK_OAK_FENCE.getDefaultState());
            table(world, o, r.centerX() + 1, z - 1, r.centerX() + 6, z + 1, y,
                    WOOD_2, Blocks.SPRUCE_FENCE.getDefaultState());
            world.setBlockState(o.add(r.centerX() - 4, y + 1, z), Blocks.LECTERN.getDefaultState());
            world.setBlockState(o.add(r.centerX() + 4, y + 1, z), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        }
    }

    private static void buildMusic(ServerWorld world, BlockPos o, Room r) {
        harpsichord(world, o, r.centerX(), r.floorY() + 2, r.centerZ(), Direction.NORTH);
        for (int x : new int[]{r.centerX() - 7, r.centerX() + 7}) {
            for (int z : new int[]{r.centerZ() - 6, r.centerZ() + 6}) {
                world.setBlockState(o.add(x, r.floorY() + 2, z), Blocks.NOTE_BLOCK.getDefaultState());
                world.setBlockState(o.add(x, r.floorY() + 3, z), Blocks.LECTERN.getDefaultState());
            }
        }
    }

    private static void buildGlobeBook(ServerWorld world, BlockPos o, Room r) {
        globe(world, o, r.centerX(), r.floorY() + 2, r.centerZ());
        cabinetZ(world, o, r.minX() + 3, r.maxX() - 3, r.floorY() + 2, r.maxZ() - 2, 7,
                WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), true);
        cabinetX(world, o, r.minX() + 2, r.floorY() + 2, r.minZ() + 4, r.maxZ() - 4, 7,
                WOOD, Blocks.DARK_OAK_SLAB.getDefaultState(), true);
    }

    private static void buildBlueSalon(ServerWorld world, BlockPos o, Room r) {
        fill(world, o, r.centerX() - 7, r.floorY() + 1, r.centerZ() - 5,
                r.centerX() + 7, r.floorY() + 1, r.centerZ() + 5, Blocks.BLUE_CARPET.getDefaultState());
        bench(world, o, r.centerX() - 6, r.centerZ() - 5, r.centerX() + 6, r.centerZ() - 5,
                r.floorY() + 2, Blocks.BLUE_WOOL.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        bench(world, o, r.centerX() - 6, r.centerZ() + 5, r.centerX() + 6, r.centerZ() + 5,
                r.floorY() + 2, Blocks.LIGHT_BLUE_WOOL.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        for (int x : new int[]{r.centerX() - 7, r.centerX() + 7})
            pottedPlant(world, o, x, r.floorY() + 2, r.centerZ(), Blocks.POTTED_BLUE_ORCHID.getDefaultState());
    }

    private static void buildBanquet(ServerWorld world, BlockPos o, Room r) {
        int y = r.floorY() + 2;
        for (int z = r.minZ() + 5; z <= r.maxZ() - 5; z += 5) {
            table(world, o, r.minX() + 4, z - 1, r.maxX() - 4, z + 1, y,
                    Blocks.DARK_OAK_SLAB.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            for (int x = r.minX() + 5; x <= r.maxX() - 5; x += 4) {
                world.setBlockState(o.add(x, y + 1, z), Math.floorMod(x + z, 8) == 0
                        ? Blocks.CAKE.getDefaultState()
                        : litCandle(Blocks.WHITE_CANDLE.getDefaultState()));
            }
        }
    }

    private static void buildDisplayNiches(ServerWorld world, BlockPos o, Room r, BlockState accent) {
        for (int x = r.minX() + 5; x <= r.maxX() - 5; x += 5) {
            fill(world, o, x, r.floorY() + 2, r.maxZ() - 3, x, r.floorY() + 5, r.maxZ() - 3, PALE);
            world.setBlockState(o.add(x, r.floorY() + 6, r.maxZ() - 3), accent);
        }
    }

    /** Two monumental stair flights make both upper wings traversable without filling the courts. */
    private static void buildVerticalCirculation(ServerWorld world, BlockPos o) {
        buildWingStair(world, o, physical(CABINET), true);
        buildWingStair(world, o, physical(SALLA_TERRENA), false);
    }

    private static void buildWingStair(ServerWorld world, BlockPos o, Room lower, boolean west) {
        int x = west ? lower.minX() + 5 : lower.maxX() - 5;
        int z0 = lower.minZ() + 4;
        clear(world, o, x - 2, lower.floorY() + 1, z0 - 1, x + 2, 16, z0 + 16);
        for (int step = 0; step < 14; step++) {
            int z = z0 + step;
            int y = lower.floorY() + 1 + step;
            world.setBlockState(o.add(x, y, z), stair(Blocks.DARK_OAK_STAIRS.getDefaultState(), Direction.SOUTH));
            world.setBlockState(o.add(x - 1, y, z), Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(o.add(x + 1, y, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        clear(world, o, x - 1, 15, z0 + 12, x + 1, 16, z0 + 16);
        fill(world, o, x - 2, 15, z0 + 14, x + 2, 15, z0 + 17, Blocks.DARK_OAK_PLANKS.getDefaultState());
    }

    private static BlockState accentFor(String name) {
        int bucket = Math.floorMod(name.hashCode(), 8);
        return switch (bucket) {
            case 0 -> Blocks.WAXED_CUT_COPPER.getDefaultState();
            case 1 -> Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState();
            case 2 -> Blocks.GREEN_GLAZED_TERRACOTTA.getDefaultState();
            case 3 -> Blocks.RED_GLAZED_TERRACOTTA.getDefaultState();
            case 4 -> Blocks.PURPLE_GLAZED_TERRACOTTA.getDefaultState();
            case 5 -> Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
            case 6 -> Blocks.AMETHYST_BLOCK.getDefaultState();
            default -> Blocks.GOLD_BLOCK.getDefaultState();
        };
    }
}