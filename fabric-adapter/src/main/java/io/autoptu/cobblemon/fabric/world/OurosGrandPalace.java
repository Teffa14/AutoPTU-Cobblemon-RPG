package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;

/**
 * Monumental authored palace used to prove Ouros' post-prototype build doctrine.
 *
 * The palace is one coherent two-storey institution containing nineteen distinct authored spaces.
 * The supplied reference set informs composition, room hierarchy, ornament density and palette
 * without being copied block-for-block. All geometry is real Minecraft BlockState placement.
 */
public final class OurosGrandPalace {
    public static final int MIN_X = -43;
    public static final int MAX_X = 43;
    public static final int MIN_Y = -4;
    public static final int MAX_Y = 46;
    public static final int MIN_Z = -57;
    public static final int MAX_Z = 57;
    public static final int WIDTH = MAX_X - MIN_X + 1;
    public static final int DEPTH = MAX_Z - MIN_Z + 1;
    public static final int HEIGHT = MAX_Y - MIN_Y + 1;

    // Ground-floor room cells. Center-column rooms are double-height ceremonial volumes.
    static final Room CABINET = new Room("Cabinet", -39, -53, -17, -31, 0, 14);
    static final Room ANTECHAMBER = new Room("Antechamber", -11, -53, 11, -31, 0, 29);
    static final Room SALLA_TERRENA = new Room("Salla Terrena", 17, -53, 39, -31, 0, 14);

    static final Room BLOOMING_SALON = new Room("Blooming Salon", -39, -25, -17, -3, 0, 14);
    static final Room AUDIENCE_CHAMBER = new Room("Audience Chamber", -11, -25, 11, -3, 0, 29);
    static final Room HUNTING_SALON = new Room("Hunting Salon", 17, -25, 39, -3, 0, 14);

    static final Room LIBRARY = new Room("Library", -39, 3, -17, 25, 0, 14);
    static final Room THEMIS_HALL = new Room("Themis Hall", -11, 3, 11, 25, 0, 29);
    static final Room GEOGRAPHY_CABINET = new Room("Geography Cabinet", 17, 3, 39, 25, 0, 14);

    static final Room PORCELAIN_HALL = new Room("Porcelain Hall", -39, 31, -17, 53, 0, 14);
    static final Room MARBLE_SALON = new Room("Marble Salon", -11, 31, 11, 53, 0, 29);
    static final Room GALLERY_OF_ART = new Room("Gallery of Art", 17, 31, 39, 53, 0, 14);

    // Upper-floor rooms occupy side wings; ceremonial center rooms remain double height.
    static final Room RAILING_SALON = new Room("Railing, Tables and Chairs Salon", -39, -53, -17, -31, 15, 29);
    static final Room COAT_OF_ARMS_HALL = new Room("Coat of Arms Relief Hall", 17, -53, 39, -31, 15, 29);
    static final Room ACCOUNTING_OFFICE = new Room("Accounting Office", -39, -25, -17, -3, 15, 29);
    static final Room MUSIC_CHAMBER = new Room("Music Chamber", 17, -25, 39, -3, 15, 29);
    static final Room GLOBE_BOOK_CABINET = new Room("Book Cabinet and Globe Room", -39, 3, -17, 25, 15, 29);
    static final Room BLUE_SALON = new Room("Blue Salon", 17, 3, 39, 25, 15, 29);
    static final Room BANQUET_HALL = new Room("Banquet Hall", -39, 31, -17, 53, 15, 29);

    private OurosGrandPalace() {}

    public static BuildResult build(ServerWorld world, BlockPos origin) {
        clearAndLayFoundation(world, origin);
        buildOuterEnvelope(world, origin);
        buildGroundCirculation(world, origin);
        buildUpperCirculation(world, origin);
        buildGrandStaircases(world, origin);
        OurosGrandPalaceCeremonialRooms.buildAll(world, origin);
        OurosGrandPalaceSalonRooms.buildAll(world, origin);
        OurosGrandPalaceUpperRooms.buildAll(world, origin);
        buildExteriorFacade(world, origin);
        buildMansardAndSkylight(world, origin);
        buildRoofLanterns(world, origin);
        return new BuildResult(origin, WIDTH, HEIGHT, DEPTH, 19);
    }

    private static void clearAndLayFoundation(ServerWorld world, BlockPos o) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                for (int y = 1; y <= MAX_Y; y++) {
                    world.setBlockState(o.add(x, y, z), air);
                }
                world.setBlockState(o.add(x, 0, z),
                        Math.floorMod(x * 13 + z * 17, 11) == 0
                                ? Blocks.POLISHED_ANDESITE.getDefaultState()
                                : Blocks.POLISHED_TUFF.getDefaultState());
                world.setBlockState(o.add(x, -1, z), Blocks.TUFF_BRICKS.getDefaultState());
                world.setBlockState(o.add(x, -2, z), Blocks.STONE_BRICKS.getDefaultState());
                world.setBlockState(o.add(x, -3, z), Blocks.STONE.getDefaultState());
            }
        }
    }

    private static void buildOuterEnvelope(ServerWorld world, BlockPos o) {
        BlockState base = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState wall = Blocks.TUFF_BRICKS.getDefaultState();
        BlockState pale = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState trim = Blocks.BAMBOO_MOSAIC.getDefaultState();

        // Three-block-thick exterior masonry gives the palace real bulk.
        fill(world, o, MIN_X, 1, MIN_Z, MIN_X + 2, 30, MAX_Z, wall);
        fill(world, o, MAX_X - 2, 1, MIN_Z, MAX_X, 30, MAX_Z, wall);
        fill(world, o, MIN_X + 3, 1, MIN_Z, MAX_X - 3, 30, MIN_Z + 2, wall);
        fill(world, o, MIN_X + 3, 1, MAX_Z - 2, MAX_X - 3, 30, MAX_Z, wall);

        // Deep base and upper entablature.
        fill(world, o, MIN_X, 1, MIN_Z, MAX_X, 3, MIN_Z + 2, base);
        fill(world, o, MIN_X, 1, MAX_Z - 2, MAX_X, 3, MAX_Z, base);
        fill(world, o, MIN_X, 1, MIN_Z, MIN_X + 2, 3, MAX_Z, base);
        fill(world, o, MAX_X - 2, 1, MIN_Z, MAX_X, 3, MAX_Z, base);

        for (int y : new int[]{13, 14, 29, 30}) {
            fill(world, o, MIN_X, y, MIN_Z, MIN_X + 3, y, MAX_Z, y == 14 || y == 30 ? trim : pale);
            fill(world, o, MAX_X - 3, y, MIN_Z, MAX_X, y, MAX_Z, y == 14 || y == 30 ? trim : pale);
            fill(world, o, MIN_X + 3, y, MIN_Z, MAX_X - 3, y, MIN_Z + 3, y == 14 || y == 30 ? trim : pale);
            fill(world, o, MIN_X + 3, y, MAX_Z - 3, MAX_X - 3, y, MAX_Z, y == 14 || y == 30 ? trim : pale);
        }

        // Outer pilasters continue through both storeys.
        for (int z = -50; z <= 50; z += 10) {
            fill(world, o, MIN_X + 2, 1, z - 1, MIN_X + 4, 30, z + 1, pale);
            fill(world, o, MAX_X - 4, 1, z - 1, MAX_X - 2, 30, z + 1, pale);
            fill(world, o, MIN_X + 1, 4, z, MIN_X + 4, 4, z, trim);
            fill(world, o, MAX_X - 4, 4, z, MAX_X - 1, 4, z, trim);
        }
        for (int x = -36; x <= 36; x += 12) {
            fill(world, o, x - 1, 1, MIN_Z + 2, x + 1, 30, MIN_Z + 4, pale);
            fill(world, o, x - 1, 1, MAX_Z - 4, x + 1, 30, MAX_Z - 2, pale);
        }

        // Monumental front and rear portals.
        clear(world, o, -5, 1, MIN_Z, 5, 10, MIN_Z + 3);
        clear(world, o, -4, 1, MAX_Z - 3, 4, 8, MAX_Z);
        fill(world, o, -7, 1, MIN_Z + 2, -6, 12, MIN_Z + 3, pale);
        fill(world, o, 6, 1, MIN_Z + 2, 7, 12, MIN_Z + 3, pale);
        fill(world, o, -7, 11, MIN_Z + 2, 7, 13, MIN_Z + 3, trim);
    }

    private static void buildGroundCirculation(ServerWorld world, BlockPos o) {
        BlockState dark = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState light = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState accent = Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();

        // Axial and transverse five-block galleries.
        for (int x = -16; x <= -12; x++) {
            for (int z = -53; z <= 53; z++) {
                world.setBlockState(o.add(x, 0, z), Math.floorMod(z, 6) == 0 ? accent : dark);
            }
        }
        for (int x = 12; x <= 16; x++) {
            for (int z = -53; z <= 53; z++) {
                world.setBlockState(o.add(x, 0, z), Math.floorMod(z, 6) == 0 ? accent : dark);
            }
        }
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            for (int x = -39; x <= 39; x++) {
                for (int z = band[0]; z <= band[1]; z++) {
                    world.setBlockState(o.add(x, 0, z), Math.floorMod(x, 8) == 0 ? accent : light);
                }
            }
        }

        // Perimeter galleries make every side room accessible from more than one direction.
        fill(world, o, -42, 0, -54, -40, 0, 54, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(world, o, 40, 0, -54, 42, 0, 54, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(world, o, -39, 0, -56, 39, 0, -54, Blocks.POLISHED_ANDESITE.getDefaultState());
        fill(world, o, -39, 0, 54, 39, 0, 56, Blocks.POLISHED_ANDESITE.getDefaultState());

        // Long red processional carpet linking all four double-height halls.
        fill(world, o, -2, 1, -56, 2, 1, 56, Blocks.RED_CARPET.getDefaultState());

        // Corridor ceiling ribs and supported hanging lights.
        for (int z = -50; z <= 50; z += 7) {
            fill(world, o, -16, 12, z, -12, 12, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            fill(world, o, 12, 12, z, 16, 12, z, Blocks.BAMBOO_MOSAIC.getDefaultState());
            world.setBlockState(o.add(-14, 11, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(-14, 10, z), litLantern(true));
            world.setBlockState(o.add(14, 11, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(14, 10, z), litLantern(true));
        }
    }

    private static void buildUpperCirculation(ServerWorld world, BlockPos o) {
        BlockState floor = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState rail = Blocks.BAMBOO_FENCE.getDefaultState();
        // Two long upper galleries serve all seven second-floor rooms and overlook the ceremonial halls.
        fill(world, o, -16, 15, -53, -12, 15, 53, floor);
        fill(world, o, 12, 15, -53, 16, 15, 53, floor);
        for (int z = -52; z <= 52; z++) {
            world.setBlockState(o.add(-12, 16, z), rail);
            world.setBlockState(o.add(12, 16, z), rail);
        }
        for (int[] band : new int[][]{{-30, -26}, {-2, 2}, {26, 30}}) {
            fill(world, o, -39, 15, band[0], 39, 15, band[1], Blocks.BAMBOO_MOSAIC.getDefaultState());
        }
    }

    private static void buildGrandStaircases(ServerWorld world, BlockPos o) {
        BlockState stair = Blocks.POLISHED_DIORITE_STAIRS.getDefaultState();
        BlockState landing = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState rail = Blocks.BAMBOO_FENCE.getDefaultState();

        // Mirrored processional stairs rise inside the longitudinal gallery bands.
        for (int side : new int[]{-1, 1}) {
            int x0 = side < 0 ? -16 : 12;
            for (int step = 0; step < 14; step++) {
                int y = 1 + step;
                int z = -18 + step;
                for (int dx = 0; dx < 5; dx++) {
                    int x = x0 + dx;
                    world.setBlockState(o.add(x, y, z), stair(stair, Direction.SOUTH));
                    if (dx == 0 || dx == 4) {
                        world.setBlockState(o.add(x, y + 1, z), rail);
                    }
                }
            }
            fill(world, o, x0, 15, -4, x0 + 4, 15, 2, landing);
        }
    }

    private static void buildExteriorFacade(ServerWorld world, BlockPos o) {
        BlockState frame = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState glass = Blocks.GLASS.getDefaultState();
        BlockState balcony = Blocks.BAMBOO_MOSAIC_SLAB.getDefaultState();

        for (int z : new int[]{-42, -14, 14, 42}) {
            tallWindowX(world, o, MIN_X + 1, z, 0, 9, frame, glass);
            tallWindowX(world, o, MAX_X - 1, z, 0, 9, frame, glass);
            tallWindowX(world, o, MIN_X + 1, z, 15, 9, frame, glass);
            tallWindowX(world, o, MAX_X - 1, z, 15, 9, frame, glass);
        }
        for (int x : new int[]{-28, 0, 28}) {
            tallWindowZ(world, o, x, MIN_Z + 1, 0, 9, frame, glass);
            tallWindowZ(world, o, x, MAX_Z - 1, 0, 9, frame, glass);
            if (x != 0) {
                tallWindowZ(world, o, x, MIN_Z + 1, 15, 9, frame, glass);
                tallWindowZ(world, o, x, MAX_Z - 1, 15, 9, frame, glass);
            }
        }

        // Balconies, brackets and facade shadow lines.
        for (int z = -48; z <= 48; z += 16) {
            fill(world, o, MIN_X - 0, 14, z - 3, MIN_X + 3, 14, z + 3, balcony);
            fill(world, o, MAX_X - 3, 14, z - 3, MAX_X, 14, z + 3, balcony);
            for (int dz : new int[]{-3, 3}) {
                fill(world, o, MIN_X + 2, 11, z + dz, MIN_X + 2, 13, z + dz, Blocks.BAMBOO_FENCE.getDefaultState());
                fill(world, o, MAX_X - 2, 11, z + dz, MAX_X - 2, 13, z + dz, Blocks.BAMBOO_FENCE.getDefaultState());
            }
        }

        // Exterior lamps are mounted to actual masonry shelves.
        for (int z = -50; z <= 50; z += 10) {
            world.setBlockState(o.add(MIN_X + 3, 8, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(MIN_X + 4, 8, z), litLantern(false));
            world.setBlockState(o.add(MAX_X - 3, 8, z), Blocks.BAMBOO_TRAPDOOR.getDefaultState());
            world.setBlockState(o.add(MAX_X - 4, 8, z), litLantern(false));
        }
    }

    private static void buildMansardAndSkylight(ServerWorld world, BlockPos o) {
        BlockState roof = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState roofAccent = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
        BlockState glassA = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        BlockState glassB = Blocks.LIME_STAINED_GLASS.getDefaultState();

        // Eight two-block-thick stepped rings create a visibly supported mansard volume.
        for (int level = 0; level < 9; level++) {
            int y = 31 + level;
            int inset = level;
            int minX = MIN_X + inset;
            int maxX = MAX_X - inset;
            int minZ = MIN_Z + inset;
            int maxZ = MAX_Z - inset;
            BlockState state = level == 0 || level == 4 || level == 8 ? roofAccent : roof;
            fill(world, o, minX, y, minZ, maxX, y, minZ + 1, state);
            fill(world, o, minX, y, maxZ - 1, maxX, y, maxZ, state);
            fill(world, o, minX, y, minZ + 2, minX + 1, y, maxZ - 2, state);
            fill(world, o, maxX - 1, y, minZ + 2, maxX, y, maxZ - 2, state);
        }

        // Fully enclosed stained-glass roof field. Frame lines terminate into the mansard ring.
        int y = 39;
        for (int x = MIN_X + 9; x <= MAX_X - 9; x++) {
            for (int z = MIN_Z + 9; z <= MAX_Z - 9; z++) {
                boolean frame = Math.floorMod(x + 34, 7) == 0 || Math.floorMod(z + 48, 7) == 0;
                BlockState state;
                if (frame) {
                    state = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
                } else {
                    state = Math.floorMod(x * 5 + z * 7, 13) == 0 ? glassB : glassA;
                }
                world.setBlockState(o.add(x, y, z), state);
            }
        }

        // Deep interior roof beams connect the glass field to room walls and upper gallery piers.
        for (int x = -35; x <= 35; x += 7) {
            fill(world, o, x, 30, -48, x, 38, -48, Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
            fill(world, o, x, 38, -48, x, 38, 48, log(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState(), Direction.Axis.Z));
        }
        for (int z = -48; z <= 48; z += 7) {
            fill(world, o, -35, 38, z, 35, 38, z, log(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState(), Direction.Axis.X));
        }
    }

    private static void buildRoofLanterns(ServerWorld world, BlockPos o) {
        // Three compact glazed lanterns punctuate the roof without creating open-air cupolas.
        for (int z : new int[]{-28, 0, 28}) {
            int y0 = 40;
            fill(world, o, -5, y0, z - 5, 5, y0, z + 5, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            for (int y = y0 + 1; y <= y0 + 4; y++) {
                for (int x : new int[]{-5, 5}) {
                    fill(world, o, x, y, z - 4, x, y, z + 4, Blocks.GLASS.getDefaultState());
                }
                for (int zz : new int[]{z - 5, z + 5}) {
                    fill(world, o, -4, y, zz, 4, y, zz, Blocks.GLASS.getDefaultState());
                }
            }
            for (int x : new int[]{-5, 0, 5}) {
                fill(world, o, x, y0 + 1, z - 5, x, y0 + 4, z + 5,
                        log(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState(), Direction.Axis.Y));
            }
            fill(world, o, -6, y0 + 5, z - 6, 6, y0 + 5, z + 6, Blocks.OXIDIZED_CUT_COPPER.getDefaultState());
            fill(world, o, -4, y0 + 6, z - 4, 4, y0 + 6, z + 4, Blocks.OXIDIZED_CUT_COPPER.getDefaultState());
            world.setBlockState(o.add(0, y0 + 7, z), Blocks.LIGHTNING_ROD.getDefaultState().with(Properties.FACING, Direction.UP));
        }
    }

    public record BuildResult(BlockPos origin, int width, int height, int depth, int authoredSpaces) {}
}