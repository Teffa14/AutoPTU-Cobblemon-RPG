package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.world.build.OurosVoxelGeometry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Complete replacement build for Meridian Canopy Gym.
 *
 * This class intentionally does not consume the legacy Meridian builder/detail pass stack. The
 * geometry is authored as one structure so roofs, columns, balconies, arena tiers, service spaces,
 * vegetation and circulation are designed together instead of being pasted onto a rectangular base.
 * Minecraft blocks remain presentation only and do not invent PTU terrain or battle mechanics.
 */
public final class MeridianCanopyGymRebuild {
    public static final int WIDTH = 67;
    public static final int DEPTH = 67;
    public static final int MAX_HEIGHT = 22;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState STONE = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState MOSSY_STONE = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    private static final BlockState TUFF = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState MUD = Blocks.MUD_BRICKS.getDefaultState();
    private static final BlockState CALCITE = Blocks.CALCITE.getDefaultState();
    private static final BlockState DARK_PLANK = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState OAK_PLANK = Blocks.OAK_PLANKS.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState GLASS_PANE = Blocks.GLASS_PANE.getDefaultState();
    private static final BlockState COPPER = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_GRATE = Blocks.OXIDIZED_COPPER_GRATE.getDefaultState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.getDefaultState();
    private static final BlockState MOSS_CARPET = Blocks.MOSS_CARPET.getDefaultState();
    private static final BlockState SAND = Blocks.SMOOTH_SANDSTONE.getDefaultState();
    private static final BlockState PACKED_MUD = Blocks.PACKED_MUD.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private MeridianCanopyGymRebuild() {}

    public static BuildResult build(ServerWorld world, BlockPos origin) {
        clearAndSculptSite(world, origin);
        buildApproachGarden(world, origin);
        buildGatehouse(world, origin);
        buildGrandConservatory(world, origin);
        buildWestBotanicalWing(world, origin);
        buildEastHydroWing(world, origin);
        buildBattleSanctum(world, origin);
        buildBackstageAndService(world, origin);
        buildUpperCirculation(world, origin);
        buildLandscapeAndSpecimenTrees(world, origin);
        addInteriorLife(world, origin);

        return new BuildResult(origin, WIDTH, DEPTH, MAX_HEIGHT);
    }

    private static void clearAndSculptSite(ServerWorld world, BlockPos o) {
        for (int x = -33; x <= 33; x++) {
            for (int z = -33; z <= 33; z++) {
                for (int y = 1; y <= MAX_HEIGHT; y++) {
                    world.setBlockState(o.add(x, y, z), AIR);
                }

                int edge = Math.max(Math.abs(x), Math.abs(z));
                BlockState ground;
                if (edge >= 31) {
                    ground = Math.floorMod(x * 17 + z * 31, 5) == 0
                            ? Blocks.COARSE_DIRT.getDefaultState()
                            : Blocks.GRASS_BLOCK.getDefaultState();
                } else {
                    ground = Blocks.GRASS_BLOCK.getDefaultState();
                }
                world.setBlockState(o.add(x, 0, z), ground);
                world.setBlockState(o.add(x, -1, z), Blocks.DIRT.getDefaultState());
                world.setBlockState(o.add(x, -2, z), Blocks.STONE.getDefaultState());
            }
        }

        // Foundations follow each volume instead of forming one giant rectangular plinth.
        foundation(world, o, -15, -25, 15, -17);
        foundation(world, o, -17, -18, 17, 11);
        foundation(world, o, -31, -14, -17, 12);
        foundation(world, o, 17, -14, 31, 12);
        ellipseFoundation(world, o.add(0, 0, 22), 25, 11);
        foundation(world, o, -31, 13, -24, 30);
    }

    private static void buildApproachGarden(ServerWorld world, BlockPos o) {
        // A widening processional walk creates a real approach instead of a strip to a wall.
        for (int z = -33; z <= -25; z++) {
            int progress = z + 33;
            int half = 4 + progress / 3;
            for (int x = -half; x <= half; x++) {
                BlockState paving = Math.floorMod(x * 11 + z * 7, 9) == 0
                        ? Blocks.POLISHED_ANDESITE.getDefaultState()
                        : Blocks.ANDESITE.getDefaultState();
                world.setBlockState(o.add(x, 0, z), paving);
            }
        }

        // Two shallow bioswale rills make the arrival feel landscaped and engineered.
        for (int z = -33; z <= -27; z++) {
            for (int x : new int[]{-10, 10}) {
                world.setBlockState(o.add(x, 0, z), WATER);
                world.setBlockState(o.add(x - 1, 0, z), Blocks.MOSSY_STONE_BRICK_SLAB.getDefaultState());
                world.setBlockState(o.add(x + 1, 0, z), Blocks.MOSSY_STONE_BRICK_SLAB.getDefaultState());
            }
        }

        // Arrival terraces and low retaining walls.
        for (int x = -14; x <= 14; x++) {
            world.setBlockState(o.add(x, 0, -25), Blocks.STONE_BRICK_SLAB.getDefaultState());
            if (Math.abs(x) > 7) {
                world.setBlockState(o.add(x, 1, -24), texturedStone(x, 1, -24));
            }
        }
        for (int x : new int[]{-15, 15}) {
            for (int z = -31; z <= -25; z++) {
                world.setBlockState(o.add(x, 1, z), Blocks.STONE_BRICK_WALL.getDefaultState());
            }
        }

        // Human-scale approach furniture.
        for (int x : new int[]{-18, 18}) {
            world.setBlockState(o.add(x, 1, -29), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(x + (x < 0 ? 1 : -1), 1, -29), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(x, 1, -27), Blocks.COMPOSTER.getDefaultState());
            world.setBlockState(o.add(x, 2, -27), x < 0
                    ? Blocks.FLOWERING_AZALEA.getDefaultState()
                    : Blocks.AZALEA.getDefaultState());
        }

        for (int x : new int[]{-8, 8}) {
            column(world, o, x, 1, -31, 4, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            world.setBlockState(o.add(x, 5, -31), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x, 4, -31), LANTERN);
        }
    }

    private static void buildGatehouse(ServerWorld world, BlockPos o) {
        // Four masonry towers and a recessed timber gate create an institutional threshold.
        for (int x : new int[]{-13, -7, 7, 13}) {
            fillTexturedStone(world, o, x - 1, 1, -24, x + 1, 4, -18);
            column(world, o, x, 5, -23, 4, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, x, 5, -19, 4, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }

        // Side waiting bays are actual rooms rather than facade recesses.
        for (int side : new int[]{-1, 1}) {
            int minX = side < 0 ? -12 : 7;
            int maxX = side < 0 ? -7 : 12;
            fill(world, o, minX, 1, -23, maxX, 1, -19, DARK_PLANK);
            fillTexturedStone(world, o, minX, 2, -18, maxX, 4, -18);
            clear(world, o, minX + 1, 2, -22, maxX - 1, 7, -19);
            for (int x = minX + 1; x <= maxX - 1; x += 2) {
                world.setBlockState(o.add(x, 3, -24), GLASS_PANE);
                world.setBlockState(o.add(x, 4, -24), GLASS_PANE);
            }
            world.setBlockState(o.add(side < 0 ? -10 : 10, 2, -20), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(side < 0 ? -9 : 9, 2, -20), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(side < 0 ? -8 : 8, 2, -20), side < 0
                    ? Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState()
                    : Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        }

        // Open central portal, strongly framed and deep enough to cast shadow.
        for (int x : new int[]{-5, 5}) {
            fill(world, o, x - 1, 1, -25, x + 1, 2, -18, DEEPSLATE);
            column(world, o, x, 3, -23, 6, logY(Blocks.STRIPPED_OAK_LOG.getDefaultState()));
            column(world, o, x, 3, -19, 6, logY(Blocks.STRIPPED_OAK_LOG.getDefaultState()));
        }
        beamX(world, o, -6, 6, 9, -23, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamX(world, o, -6, 6, 9, -19, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamZ(world, o, -23, -19, 8, -5, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamZ(world, o, -23, -19, 8, 5, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));

        // Deep bracket rhythm under the eaves.
        for (int x = -13; x <= 13; x += 2) {
            world.setBlockState(o.add(x, 8, -25), Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(o.add(x, 9, -25), Blocks.DARK_OAK_SLAB.getDefaultState());
        }

        buildGableRoof(world, o, 0, -21, 15, 5, 9, Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState());

        // Ridge details and hanging entry lamps are supported from the roof beam.
        for (int z = -24; z <= -18; z++) {
            world.setBlockState(o.add(0, 15, z), logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        for (int x : new int[]{-3, 3}) {
            world.setBlockState(o.add(x, 8, -20), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x, 7, -20), LANTERN);
        }
    }

    private static void buildGrandConservatory(ServerWorld world, BlockPos o) {
        // Stone arcade base with timber/glass upper level. The center is intentionally open and tall.
        for (int x = -16; x <= 16; x++) {
            for (int z = -17; z <= 10; z++) {
                boolean perimeter = Math.abs(x) >= 14 || z <= -16 || z >= 9;
                if (!perimeter) {
                    continue;
                }
                for (int y = 1; y <= 3; y++) {
                    world.setBlockState(o.add(x, y, z), texturedStone(x, y, z));
                }
            }
        }
        clear(world, o, -12, 1, -17, 12, 8, -15);
        clear(world, o, -6, 1, 9, 6, 8, 10);

        // Structural bays. Logs carry the roof all the way to the foundation.
        for (int x : new int[]{-14, -9, -4, 4, 9, 14}) {
            column(world, o, x, 1, -16, 13, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, x, 1, 9, 13, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        for (int z : new int[]{-12, -6, 0, 6}) {
            column(world, o, -15, 1, z, 13, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, 15, 1, z, 13, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }

        // Tall glazing sits between frames, with stone sills and copper ventilation grilles.
        for (int z : new int[]{-12, -6, 0, 6}) {
            for (int y = 4; y <= 10; y++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.setBlockState(o.add(-15, y, z + dz), y == 6 ? COPPER_GRATE : GLASS_PANE);
                    world.setBlockState(o.add(15, y, z + dz), y == 6 ? COPPER_GRATE : GLASS_PANE);
                }
            }
            world.setBlockState(o.add(-15, 3, z), Blocks.STONE_BRICK_SLAB.getDefaultState());
            world.setBlockState(o.add(15, 3, z), Blocks.STONE_BRICK_SLAB.getDefaultState());
        }

        // Ground-floor circulation forms a broad ellipse around the central biosphere.
        placeEllipse(world, o.add(0, 1, -2), 13, 10, DARK_PLANK);
        placeEllipse(world, o.add(0, 1, -2), 9, 7, MOSS);
        placeEllipse(world, o.add(0, 1, -2), 6, 5, WATER);
        placeEllipse(world, o.add(0, 1, -2), 4, 3, MOSS);

        // Four bridges cross the water ring and are supported by low stone abutments.
        fill(world, o, -13, 2, -3, -4, 2, -1, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, 4, 2, -3, 13, 2, -1, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, -1, 2, -10, 1, 2, -5, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, -1, 2, 1, 1, 2, 8, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int x : new int[]{-4, 4}) {
            fill(world, o, x - 1, 1, -3, x + 1, 1, -1, DEEPSLATE);
        }
        for (int z : new int[]{-5, 1}) {
            fill(world, o, -1, 1, z - 1, 1, 1, z + 1, DEEPSLATE);
        }

        buildCentralSpecimenTree(world, o.add(0, 2, -2));

        // Barrel-vault roof. Every copper rib terminates on the timber wall plate below.
        for (int z = -15; z <= 8; z += 4) {
            for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.parabolicArchX(14, 6, 1, 0)) {
                BlockPos p = o.add(voxel.x(), 13 + voxel.y(), z);
                world.setBlockState(p, COPPER);
            }
        }
        // Glass strips fill the spaces between ribs while leaving a dark ridge spine.
        for (int z = -14; z <= 8; z++) {
            if (Math.floorMod(z + 15, 4) == 0) {
                continue;
            }
            for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.parabolicArchX(13, 5, 1, 0)) {
                BlockPos p = o.add(voxel.x(), 13 + voxel.y(), z);
                if (world.getBlockState(p).isAir()) {
                    world.setBlockState(p, GLASS);
                }
            }
        }
        beamZ(world, o, -16, 9, 20, 0, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));

        // Vent lantern on the ridge gives the conservatory a second-scale roof object.
        for (int z = -4; z <= 2; z++) {
            world.setBlockState(o.add(-2, 20, z), COPPER_GRATE);
            world.setBlockState(o.add(2, 20, z), COPPER_GRATE);
        }
        beamZ(world, o, -4, 2, 21, -2, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamZ(world, o, -4, 2, 21, 2, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        for (int z : new int[]{-3, 1}) {
            world.setBlockState(o.add(0, 21, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(0, 20, z), LANTERN);
        }
    }

    private static void buildWestBotanicalWing(ServerWorld world, BlockPos o) {
        // Three staggered terrace houses create setbacks instead of a single wing box.
        int[][] terraces = {
                {-31, -14, -23, -4, 5},
                {-29, -3, -18, 5, 7},
                {-27, 6, -17, 12, 9}
        };
        for (int[] t : terraces) {
            int minX = t[0];
            int minZ = t[1];
            int maxX = t[2];
            int maxZ = t[3];
            int wallHeight = t[4];
            fill(world, o, minX, 1, minZ, maxX, 1, maxZ, MUD);
            for (int y = 2; y <= wallHeight; y++) {
                for (int x = minX; x <= maxX; x++) {
                    world.setBlockState(o.add(x, y, minZ), texturedGardenWall(x, y, minZ));
                    world.setBlockState(o.add(x, y, maxZ), texturedGardenWall(x, y, maxZ));
                }
                for (int z = minZ; z <= maxZ; z++) {
                    world.setBlockState(o.add(minX, y, z), texturedGardenWall(minX, y, z));
                    world.setBlockState(o.add(maxX, y, z), texturedGardenWall(maxX, y, z));
                }
            }
            clear(world, o, minX + 1, 2, minZ + 1, maxX - 1, wallHeight, maxZ - 1);
        }

        // Timber frames and supported galleries stitch the terraces into a deliberate challenge route.
        for (int z : new int[]{-12, -7, -2, 3, 8}) {
            column(world, o, -29, 2, z, 8, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, -19, 2, z, 8, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            beamX(world, o, -29, -19, 9, z, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        fill(world, o, -28, 8, -11, -18, 8, -9, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, -28, 8, -2, -18, 8, 0, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, -26, 8, 7, -17, 8, 9, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int x = -28; x <= -18; x++) {
            world.setBlockState(o.add(x, 9, -11), Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(o.add(x, 9, -9), Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        // Roof cluster: three proper gables, each with wall support and ridge, not floating fragments.
        buildGableRoof(world, o, -26, -9, 6, 5, 6, Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState());
        buildGableRoof(world, o, -23, 1, 6, 5, 8, Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState());
        buildGableRoof(world, o, -22, 9, 5, 4, 10, Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState());

        // Botanical challenge islands, nursery benches and observation points.
        for (int z : new int[]{-9, -4, 2, 8}) {
            fill(world, o, -27, 2, z - 1, -24, 2, z + 1, MOSS);
            world.setBlockState(o.add(-26, 3, z), Math.floorMod(z, 2) == 0
                    ? Blocks.FLOWERING_AZALEA.getDefaultState()
                    : Blocks.AZALEA.getDefaultState());
            world.setBlockState(o.add(-24, 3, z), Blocks.FERN.getDefaultState());
        }
        for (int z : new int[]{-7, 0, 7}) {
            fill(world, o, -21, 2, z - 1, -19, 2, z + 1, Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(-20, 3, z), Blocks.POTTED_FERN.getDefaultState());
        }
    }

    private static void buildEastHydroWing(ServerWorld world, BlockPos o) {
        // Heavy lower aqueduct base with lighter greenhouse superstructure.
        fill(world, o, 17, 1, -14, 31, 1, 12, DEEPSLATE);
        for (int z = -13; z <= 11; z++) {
            for (int y = 2; y <= 4; y++) {
                world.setBlockState(o.add(17, y, z), texturedStone(17, y, z));
                world.setBlockState(o.add(31, y, z), texturedStone(31, y, z));
            }
        }
        for (int x = 18; x <= 30; x++) {
            for (int y = 2; y <= 4; y++) {
                world.setBlockState(o.add(x, y, -14), texturedStone(x, y, -14));
                world.setBlockState(o.add(x, y, 12), texturedStone(x, y, 12));
            }
        }
        clear(world, o, 18, 2, -13, 30, 11, 11);

        // Water channels are bounded by stone/copper and crossed by actual bridges.
        for (int z = -11; z <= 9; z++) {
            for (int x : new int[]{20, 21, 27, 28}) {
                world.setBlockState(o.add(x, 2, z), WATER);
            }
            world.setBlockState(o.add(19, 2, z), Blocks.STONE_BRICK_SLAB.getDefaultState());
            world.setBlockState(o.add(22, 2, z), Blocks.COPPER_GRATE.getDefaultState());
            world.setBlockState(o.add(26, 2, z), Blocks.COPPER_GRATE.getDefaultState());
            world.setBlockState(o.add(29, 2, z), Blocks.STONE_BRICK_SLAB.getDefaultState());
        }
        for (int z : new int[]{-8, 0, 8}) {
            fill(world, o, 19, 3, z - 1, 29, 3, z + 1, Blocks.DARK_OAK_SLAB.getDefaultState());
            for (int x : new int[]{19, 29}) {
                fill(world, o, x, 2, z - 1, x, 2, z + 1, STONE);
            }
        }

        // Greenhouse wall framing.
        for (int z : new int[]{-12, -6, 0, 6, 11}) {
            column(world, o, 18, 2, z, 9, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, 30, 2, z, 9, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            beamX(world, o, 18, 30, 10, z, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        for (int z = -11; z <= 10; z++) {
            for (int y = 5; y <= 9; y++) {
                if (Math.floorMod(z, 6) != 0) {
                    world.setBlockState(o.add(18, y, z), GLASS_PANE);
                    world.setBlockState(o.add(30, y, z), GLASS_PANE);
                }
            }
        }

        // Sawtooth roof creates a strongly different profile from the botanical wing.
        for (int bayZ : new int[]{-10, -4, 2, 8}) {
            buildHydroSawtoothBay(world, o, bayZ);
        }

        // Pumps/control benches read as actual work areas.
        for (int z : new int[]{-9, -3, 3, 9}) {
            world.setBlockState(o.add(24, 3, z), COPPER);
            world.setBlockState(o.add(25, 3, z), Blocks.CAULDRON.getDefaultState());
            world.setBlockState(o.add(24, 4, z), Blocks.LEVER.getDefaultState());
            world.setBlockState(o.add(25, 4, z), Blocks.LANTERN.getDefaultState());
        }
    }

    private static void buildBattleSanctum(ServerWorld world, BlockPos o) {
        BlockPos c = o.add(0, 0, 22);

        // Elliptical masonry bowl replaces the legacy arena box entirely.
        placeEllipse(world, c.add(0, 1, 0), 24, 10, DEEPSLATE);
        placeEllipse(world, c.add(0, 2, 0), 23, 9, texturedArenaFloor(0, 0));
        placeEllipse(world, c.add(0, 2, 0), 18, 7, PACKED_MUD);
        placeEllipseRing(world, c.add(0, 2, 0), 20, 8, 18, 7, SAND);
        placeEllipseRing(world, c.add(0, 2, 0), 5, 3, 3, 2, MOSS);

        // Formal center axis and two player staging marks.
        for (int x = -13; x <= 13; x++) {
            world.setBlockState(c.add(x, 2, 0), Math.abs(x) <= 1 ? MOSS : SAND);
        }
        for (int x : new int[]{-10, 10}) {
            placeEllipseRing(world, c.add(x, 2, 0), 2, 2, 1, 1, CALCITE);
        }

        // Spectator tiers are concentric and interrupted at the processional/leader axes.
        for (int tier = 0; tier < 4; tier++) {
            int outerX = 24 - tier;
            int outerZ = 10 - tier;
            int innerX = 22 - tier;
            int innerZ = 8 - tier;
            for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(
                    outerX, outerZ, innerX, innerZ, 3 + tier)) {
                if (Math.abs(voxel.x()) <= 4 || Math.abs(voxel.z()) >= 9) {
                    continue;
                }
                world.setBlockState(c.add(voxel.x(), voxel.y(), voxel.z()),
                        tier % 2 == 0 ? Blocks.STONE_BRICK_SLAB.getDefaultState()
                                : Blocks.POLISHED_TUFF_SLAB.getDefaultState());
            }
        }

        // Perimeter piers support the roof ring. Their positions follow the bowl instead of a rectangle.
        int[][] piers = {
                {-22, -5}, {-18, -8}, {-10, -10}, {0, -10}, {10, -10}, {18, -8}, {22, -5},
                {-22, 5}, {-18, 8}, {-10, 10}, {0, 10}, {10, 10}, {18, 8}, {22, 5}
        };
        for (int[] p : piers) {
            fill(world, c, p[0] - 1, 3, p[1] - 1, p[0] + 1, 4, p[1] + 1, texturedStone(p[0], 3, p[1]));
            column(world, c, p[0], 5, p[1], 8, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            world.setBlockState(c.add(p[0], 13, p[1]), Blocks.CHISELED_TUFF_BRICKS.getDefaultState());
        }

        // Roof ring: broad dark eaves over spectators with an open botanical oculus above the field.
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(25, 11, 19, 7, 13)) {
            world.setBlockState(c.add(voxel.x(), voxel.y(), voxel.z()), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
        }
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(24, 10, 20, 7, 14)) {
            world.setBlockState(c.add(voxel.x(), voxel.y(), voxel.z()), Blocks.DEEPSLATE_TILES.getDefaultState());
        }
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(22, 9, 19, 7, 15)) {
            world.setBlockState(c.add(voxel.x(), voxel.y(), voxel.z()), COPPER);
        }

        // Cross-ties keep the roof ring visually supported and frame the oculus.
        for (int x : new int[]{-18, -9, 0, 9, 18}) {
            beamZ(world, c, -8, 8, 12, x, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }

        // Leader dais, rooted into a masonry apse at the north end.
        fill(world, c, -6, 3, 7, 6, 3, 9, SAND);
        fill(world, c, -5, 4, 8, 5, 4, 10, TUFF);
        for (int x : new int[]{-6, 6}) {
            column(world, c, x, 4, 9, 7, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        beamX(world, c, -7, 7, 11, 9, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        world.setBlockState(c.add(0, 5, 9), Blocks.LECTERN.getDefaultState());
        for (int x : new int[]{-4, 4}) {
            world.setBlockState(c.add(x, 6, 9), Blocks.CHAIN.getDefaultState());
            world.setBlockState(c.add(x, 5, 9), LANTERN);
        }

        // Arena planting is architectural and kept outside the battle floor.
        for (int[] p : new int[][]{{-19, -6}, {19, -6}, {-19, 6}, {19, 6}}) {
            fill(world, c, p[0] - 1, 3, p[1] - 1, p[0] + 1, 3, p[1] + 1, MOSS);
            world.setBlockState(c.add(p[0], 4, p[1]), Blocks.FLOWERING_AZALEA.getDefaultState());
            world.setBlockState(c.add(p[0] + 1, 4, p[1]), Blocks.FERN.getDefaultState());
        }
    }

    private static void buildBackstageAndService(ServerWorld world, BlockPos o) {
        // A narrow west service bar remains visually secondary but fully functional.
        fill(world, o, -31, 1, 14, -25, 1, 30, DEEPSLATE);
        for (int y = 2; y <= 6; y++) {
            for (int z = 14; z <= 30; z++) {
                world.setBlockState(o.add(-31, y, z), texturedStone(-31, y, z));
                world.setBlockState(o.add(-25, y, z), texturedStone(-25, y, z));
            }
            for (int x = -30; x <= -26; x++) {
                world.setBlockState(o.add(x, y, 14), texturedStone(x, y, 14));
                world.setBlockState(o.add(x, y, 30), texturedStone(x, y, 30));
            }
        }
        clear(world, o, -30, 2, 15, -26, 5, 29);
        fill(world, o, -30, 2, 15, -26, 2, 29, Blocks.SPRUCE_PLANKS.getDefaultState());

        // Proper lean-to roof with continuous wall plate and supports.
        beamZ(world, o, 14, 30, 7, -31, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamZ(world, o, 14, 30, 7, -25, logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        BlockState eastSlope = stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.EAST);
        for (int z = 13; z <= 31; z++) {
            world.setBlockState(o.add(-32, 8, z), eastSlope);
            world.setBlockState(o.add(-31, 8, z), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
            world.setBlockState(o.add(-30, 9, z), eastSlope);
            world.setBlockState(o.add(-29, 9, z), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
            world.setBlockState(o.add(-28, 10, z), eastSlope);
            world.setBlockState(o.add(-27, 10, z), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
            world.setBlockState(o.add(-26, 11, z), eastSlope);
        }

        // Storage, repair and groundskeeping props.
        int[][] stations = {{-29, 16}, {-27, 16}, {-29, 20}, {-27, 20}, {-29, 24}, {-27, 24}};
        BlockState[] props = {
                Blocks.BARREL.getDefaultState(), Blocks.COMPOSTER.getDefaultState(),
                Blocks.CRAFTING_TABLE.getDefaultState(), Blocks.STONECUTTER.getDefaultState(),
                Blocks.CAULDRON.getDefaultState(), Blocks.ANVIL.getDefaultState()
        };
        for (int i = 0; i < stations.length; i++) {
            world.setBlockState(o.add(stations[i][0], 3, stations[i][1]), props[i]);
        }
        fill(world, o, -30, 4, 27, -26, 4, 27, Blocks.BARREL.getDefaultState());
        world.setBlockState(o.add(-28, 5, 27), Blocks.LANTERN.getDefaultState());

        // Service yard outside the bar.
        fill(world, o, -33, 0, 17, -32, 0, 28, Blocks.GRAVEL.getDefaultState());
        for (int z : new int[]{18, 22, 26}) {
            world.setBlockState(o.add(-33, 1, z), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(-32, 1, z), Blocks.COMPOSTER.getDefaultState());
        }
    }

    private static void buildUpperCirculation(ServerWorld world, BlockPos o) {
        // Conservatory balcony follows an ellipse; bridge landings connect both challenge wings.
        BlockPos center = o.add(0, 0, -2);
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(14, 10, 11, 7, 8)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), Blocks.DARK_OAK_SLAB.getDefaultState());
        }
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(15, 11, 14, 10, 9)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        // Two elevated bridges are supported by timber bents below.
        fill(world, o, -20, 8, -4, -13, 8, -2, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, 13, 8, -4, 20, 8, -2, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int x : new int[]{-18, -14, 14, 18}) {
            column(world, o, x, 3, -3, 5, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }

        // Grand north bridge reveals the battle sanctum from above before descent.
        fill(world, o, -5, 9, 8, 5, 9, 14, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int x : new int[]{-5, 5}) {
            column(world, o, x, 3, 10, 6, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
            column(world, o, x, 3, 14, 6, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
        for (int z = 8; z <= 14; z++) {
            world.setBlockState(o.add(-6, 10, z), Blocks.DARK_OAK_FENCE.getDefaultState());
            world.setBlockState(o.add(6, 10, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        // Stair runs with full width and landings, not disconnected slabs.
        buildStairRunZ(world, o, -11, 2, -13, 4, 7, Direction.SOUTH);
        buildStairRunZ(world, o, 8, 2, 7, 4, 7, Direction.NORTH);
    }

    private static void buildLandscapeAndSpecimenTrees(ServerWorld world, BlockPos o) {
        // Retaining beds make building/terrain transitions intentional.
        for (int x = -30; x <= 30; x++) {
            if (Math.abs(x) < 8) {
                continue;
            }
            world.setBlockState(o.add(x, 1, -24), Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState());
            if (Math.floorMod(x, 3) == 0) {
                world.setBlockState(o.add(x, 2, -23), MOSS_CARPET);
            }
        }
        for (int z = -20; z <= 10; z += 5) {
            for (int x : new int[]{-32, 32}) {
                world.setBlockState(o.add(x, 1, z), Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
                world.setBlockState(o.add(x + (x < 0 ? 1 : -1), 1, z), MOSS);
                world.setBlockState(o.add(x + (x < 0 ? 1 : -1), 2, z), Blocks.FERN.getDefaultState());
            }
        }

        // Four approach specimens plus two arena-side trees. Each has roots, trunk, branches and crown masses.
        buildSpecimenTree(world, o.add(-23, 1, -29), 8, -1);
        buildSpecimenTree(world, o.add(23, 1, -29), 9, 1);
        buildSpecimenTree(world, o.add(-22, 1, -19), 7, 1);
        buildSpecimenTree(world, o.add(22, 1, -19), 8, -1);
        buildSpecimenTree(world, o.add(-29, 1, 9), 9, -1);
        buildSpecimenTree(world, o.add(29, 1, 9), 8, 1);

        // Groundcover is clustered around structural/landscape nodes rather than scattered randomly.
        int[][] beds = {
                {-19, -25}, {-14, -27}, {14, -27}, {19, -25},
                {-13, 12}, {13, 12}, {-20, 14}, {20, 14}
        };
        for (int[] bed : beds) {
            placeEllipse(world, o.add(bed[0], 1, bed[1]), 3, 2, MOSS);
            world.setBlockState(o.add(bed[0], 2, bed[1]), Blocks.FLOWERING_AZALEA.getDefaultState());
            world.setBlockState(o.add(bed[0] - 1, 2, bed[1]), Blocks.FERN.getDefaultState());
            world.setBlockState(o.add(bed[0] + 1, 2, bed[1] + 1), Blocks.AZALEA.getDefaultState());
        }
    }

    private static void addInteriorLife(ServerWorld world, BlockPos o) {
        // Reception desk and public waiting zone inside the gatehouse/conservatory transition.
        fill(world, o, -6, 2, -15, -2, 2, -14, Blocks.DARK_OAK_SLAB.getDefaultState());
        world.setBlockState(o.add(-5, 3, -14), Blocks.LECTERN.getDefaultState());
        world.setBlockState(o.add(-3, 3, -14), Blocks.CHISELED_BOOKSHELF.getDefaultState());
        fill(world, o, 4, 2, -15, 8, 2, -14, Blocks.DARK_OAK_SLAB.getDefaultState());
        world.setBlockState(o.add(5, 3, -14), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        world.setBlockState(o.add(7, 3, -14), Blocks.BOOKSHELF.getDefaultState());

        // Balcony observation stations.
        for (int[] p : new int[][]{{-11, -6}, {11, -6}, {-10, 5}, {10, 5}}) {
            world.setBlockState(o.add(p[0], 9, p[1]), Blocks.DARK_OAK_SLAB.getDefaultState());
            world.setBlockState(o.add(p[0], 10, p[1]), Blocks.POTTED_FERN.getDefaultState());
            world.setBlockState(o.add(p[0] + (p[0] < 0 ? 1 : -1), 10, p[1]), Blocks.LANTERN.getDefaultState());
        }

        // Hanging conservatory lights and botanical details. Every chain starts under an existing roof/beam zone.
        for (int[] p : new int[][]{{-8, -8}, {8, -8}, {-8, 4}, {8, 4}}) {
            for (int y = 12; y >= 10; y--) {
                world.setBlockState(o.add(p[0], y, p[1]), Blocks.CHAIN.getDefaultState());
            }
            world.setBlockState(o.add(p[0], 9, p[1]), LANTERN);
        }

        // Small backstage/public clues to real institutional use.
        world.setBlockState(o.add(-24, 3, 17), Blocks.BELL.getDefaultState());
        world.setBlockState(o.add(-24, 3, 20), Blocks.SMITHING_TABLE.getDefaultState());
        world.setBlockState(o.add(-24, 3, 23), Blocks.LOOM.getDefaultState());
        world.setBlockState(o.add(-24, 3, 26), Blocks.CARTOGRAPHY_TABLE.getDefaultState());
    }

    private static void buildCentralSpecimenTree(ServerWorld world, BlockPos root) {
        // Buttressed 3x3 lower trunk tapering to a single structural core.
        for (int y = 0; y <= 8; y++) {
            int radius = y < 3 ? 1 : 0;
            fill(world, root, -radius, y, -radius, radius, y, radius,
                    logY(Blocks.DARK_OAK_WOOD.getDefaultState()));
        }
        int[][] roots = {{-4, 0, -2}, {4, 0, -1}, {-3, 0, 4}, {3, 0, 4}, {0, 0, -5}};
        for (int[] end : roots) {
            placeLine(world, root, 0, 1, 0, end[0], end[1], end[2], Blocks.MANGROVE_ROOTS.getDefaultState());
        }
        int[][] branches = {{-6, 11, -3}, {6, 12, -2}, {-5, 13, 5}, {5, 11, 5}, {0, 15, -4}};
        for (int[] end : branches) {
            placeLine(world, root, 0, 7, 0, end[0], end[1], end[2], logY(Blocks.DARK_OAK_WOOD.getDefaultState()));
        }
        canopyMass(world, root.add(-6, 12, -3), 4, 2, 3);
        canopyMass(world, root.add(6, 13, -2), 4, 2, 3);
        canopyMass(world, root.add(-5, 14, 5), 4, 2, 3);
        canopyMass(world, root.add(5, 12, 5), 4, 2, 3);
        canopyMass(world, root.add(0, 16, -4), 5, 2, 3);
        canopyMass(world, root.add(0, 15, 2), 5, 2, 4);
    }

    private static void buildSpecimenTree(ServerWorld world, BlockPos root, int height, int lean) {
        // Root flare.
        world.setBlockState(root, Blocks.MANGROVE_ROOTS.getDefaultState());
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            world.setBlockState(root.add(d[0], 0, d[1]), Blocks.MANGROVE_ROOTS.getDefaultState());
        }

        int trunkX = 0;
        for (int y = 0; y < height; y++) {
            if (y == height / 2) {
                trunkX += lean;
            }
            world.setBlockState(root.add(trunkX, y, 0), logY(Blocks.DARK_OAK_LOG.getDefaultState()));
            if (y < 3) {
                world.setBlockState(root.add(trunkX + 1, y, 0), logY(Blocks.DARK_OAK_LOG.getDefaultState()));
            }
        }

        int branchY = height - 3;
        int[][] ends = {
                {trunkX - 4, branchY + 2, -2}, {trunkX + 4, branchY + 1, -2},
                {trunkX - 3, branchY + 3, 3}, {trunkX + 3, branchY + 2, 3}
        };
        for (int[] end : ends) {
            placeLine(world, root, trunkX, branchY, 0, end[0], end[1], end[2], logY(Blocks.DARK_OAK_LOG.getDefaultState()));
            canopyMass(world, root.add(end[0], end[1] + 1, end[2]), 3, 2, 3);
        }
        canopyMass(world, root.add(trunkX, height + 2, 0), 4, 2, 3);
    }

    private static void buildHydroSawtoothBay(ServerWorld world, BlockPos o, int centerZ) {
        BlockState west = stair(Blocks.OXIDIZED_CUT_COPPER_STAIRS.getDefaultState(), Direction.EAST);
        BlockState east = stair(Blocks.OXIDIZED_CUT_COPPER_STAIRS.getDefaultState(), Direction.WEST);
        for (int x = 18; x <= 30; x++) {
            int distance = Math.abs(x - 24);
            int y = 11 + Math.max(0, 4 - distance / 2);
            BlockState roof = x < 24 ? west : x > 24 ? east : Blocks.OXIDIZED_CUT_COPPER_SLAB.getDefaultState();
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                world.setBlockState(o.add(x, y, z), roof);
                if (distance <= 4 && z == centerZ) {
                    world.setBlockState(o.add(x, y - 1, z), GLASS);
                }
            }
        }
        column(world, o, 18, 5, centerZ - 2, 6, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        column(world, o, 30, 5, centerZ - 2, 6, logY(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        beamX(world, o, 18, 30, 10, centerZ - 2, logX(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
    }

    private static void buildGableRoof(
            ServerWorld world,
            BlockPos o,
            int centerX,
            int centerZ,
            int halfWidth,
            int halfDepth,
            int baseY,
            BlockState stairBase
    ) {
        BlockState west = stair(stairBase, Direction.EAST);
        BlockState east = stair(stairBase, Direction.WEST);
        for (int step = 0; step <= halfWidth; step++) {
            int y = baseY + step;
            int left = centerX - halfWidth + step;
            int right = centerX + halfWidth - step;
            for (int z = centerZ - halfDepth; z <= centerZ + halfDepth; z++) {
                world.setBlockState(o.add(left, y, z), west);
                world.setBlockState(o.add(right, y, z), east);
                if (step < halfWidth) {
                    if (left + 1 < right) {
                        world.setBlockState(o.add(left + 1, y, z), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
                        world.setBlockState(o.add(right - 1, y, z), Blocks.DEEPSLATE_TILE_SLAB.getDefaultState());
                    }
                }
            }
        }
        for (int z = centerZ - halfDepth; z <= centerZ + halfDepth; z++) {
            world.setBlockState(o.add(centerX, baseY + halfWidth + 1, z), logZ(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState()));
        }
    }

    private static void buildStairRunZ(
            ServerWorld world,
            BlockPos o,
            int startX,
            int startY,
            int startZ,
            int width,
            int steps,
            Direction facing
    ) {
        int zSign = facing == Direction.SOUTH ? 1 : -1;
        BlockState stair = stair(Blocks.STONE_BRICK_STAIRS.getDefaultState(), facing);
        for (int i = 0; i < steps; i++) {
            int z = startZ + i * zSign;
            int y = startY + i;
            for (int x = startX; x < startX + width; x++) {
                world.setBlockState(o.add(x, y, z), stair);
                world.setBlockState(o.add(x, y - 1, z), texturedStone(x, y - 1, z));
            }
        }
        int landingZ = startZ + steps * zSign;
        fill(world, o, startX, startY + steps, landingZ - 1, startX + width - 1, startY + steps, landingZ + 1,
                Blocks.STONE_BRICK_SLAB.getDefaultState());
    }

    private static void foundation(ServerWorld world, BlockPos o, int minX, int minZ, int maxX, int maxZ) {
        fill(world, o, minX, -1, minZ, maxX, 0, maxZ, DEEPSLATE);
    }

    private static void ellipseFoundation(ServerWorld world, BlockPos center, int radiusX, int radiusZ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipse(radiusX, radiusZ, 0)) {
            world.setBlockState(center.add(voxel.x(), -1, voxel.z()), DEEPSLATE);
            world.setBlockState(center.add(voxel.x(), 0, voxel.z()), texturedStone(voxel.x(), 0, voxel.z()));
        }
    }

    private static void fillTexturedStone(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.setBlockState(o.add(x, y, z), texturedStone(x, y, z));
                }
            }
        }
    }

    private static BlockState texturedStone(int x, int y, int z) {
        int pattern = Math.floorMod(x * 31 + y * 17 + z * 13, 17);
        if (pattern == 0 || pattern == 9) {
            return MOSSY_STONE;
        }
        if (pattern == 4 || pattern == 12) {
            return TUFF;
        }
        return STONE;
    }

    private static BlockState texturedGardenWall(int x, int y, int z) {
        int pattern = Math.floorMod(x * 19 + y * 23 + z * 11, 13);
        if (pattern == 0 || pattern == 7) {
            return MOSSY_STONE;
        }
        if (pattern == 3 || pattern == 10) {
            return MUD;
        }
        return TUFF;
    }

    private static BlockState texturedArenaFloor(int x, int z) {
        return Math.floorMod(x * 7 + z * 11, 6) == 0
                ? Blocks.POLISHED_TUFF.getDefaultState()
                : Blocks.SMOOTH_STONE.getDefaultState();
    }

    private static void placeEllipse(ServerWorld world, BlockPos center, int radiusX, int radiusZ, BlockState state) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipse(radiusX, radiusZ, 0)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }

    private static void placeEllipseRing(
            ServerWorld world,
            BlockPos center,
            int outerX,
            int outerZ,
            int innerX,
            int innerZ,
            BlockState state
    ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.ellipseRing(outerX, outerZ, innerX, innerZ, 0)) {
            world.setBlockState(center.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }

    private static void placeLine(
            ServerWorld world,
            BlockPos root,
            int sx,
            int sy,
            int sz,
            int ex,
            int ey,
            int ez,
            BlockState state
    ) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.line3d(
                new OurosVoxelGeometry.Voxel(sx, sy, sz),
                new OurosVoxelGeometry.Voxel(ex, ey, ez))) {
            world.setBlockState(root.add(voxel.x(), voxel.y(), voxel.z()), state);
        }
    }

    private static void canopyMass(ServerWorld world, BlockPos center, int rx, int ry, int rz) {
        for (OurosVoxelGeometry.Voxel voxel : OurosVoxelGeometry.filledEllipsoid(rx, ry, rz)) {
            BlockPos p = center.add(voxel.x(), voxel.y(), voxel.z());
            BlockState existing = world.getBlockState(p);
            if (!existing.isAir()
                    && !existing.isOf(Blocks.AZALEA_LEAVES)
                    && !existing.isOf(Blocks.FLOWERING_AZALEA_LEAVES)) {
                continue;
            }
            int pattern = Math.floorMod(voxel.x() * 31 + voxel.y() * 17 + voxel.z() * 13, 9);
            world.setBlockState(p, pattern == 0 || pattern == 5
                    ? Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState()
                    : Blocks.AZALEA_LEAVES.getDefaultState());
        }
    }

    private static BlockState stair(BlockState state, Direction facing) {
        return state.with(Properties.HORIZONTAL_FACING, facing);
    }

    private static BlockState logX(BlockState state) {
        return state.with(Properties.AXIS, Direction.Axis.X);
    }

    private static BlockState logY(BlockState state) {
        return state.with(Properties.AXIS, Direction.Axis.Y);
    }

    private static BlockState logZ(BlockState state) {
        return state.with(Properties.AXIS, Direction.Axis.Z);
    }

    private static void fill(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.setBlockState(o.add(x, y, z), state);
                }
            }
        }
    }

    private static void clear(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        fill(world, o, minX, minY, minZ, maxX, maxY, maxZ, AIR);
    }

    private static void column(
            ServerWorld world,
            BlockPos o,
            int x,
            int startY,
            int z,
            int height,
            BlockState state
    ) {
        for (int y = startY; y < startY + height; y++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }

    private static void beamX(
            ServerWorld world,
            BlockPos o,
            int minX,
            int maxX,
            int y,
            int z,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }

    private static void beamZ(
            ServerWorld world,
            BlockPos o,
            int minZ,
            int maxZ,
            int y,
            int x,
            BlockState state
    ) {
        for (int z = minZ; z <= maxZ; z++) {
            world.setBlockState(o.add(x, y, z), state);
        }
    }

    public record BuildResult(BlockPos origin, int width, int depth, int height) {}
}
