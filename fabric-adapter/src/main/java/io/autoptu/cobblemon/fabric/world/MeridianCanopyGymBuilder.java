package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Large authored Gym build prototype.
 *
 * This is deliberately much larger than the Cedar Meadow technical slice. It creates a multi-volume
 * public institution with an approach plaza, lobby, central planted atrium, two challenge wings,
 * elevated circulation, a service/backstage route, a leader arena and a roof overlook. It remains a
 * GAMEPLAY PROTOTYPE until visual review and playtesting satisfy docs/ouros-build-quality-bar.md.
 */
public final class MeridianCanopyGymBuilder {
    public static final int WIDTH = 67;
    public static final int DEPTH = 67;
    public static final int MAX_HEIGHT = 22;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState FOUNDATION = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState PRIMARY = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState WEATHERED = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    private static final BlockState FRAME = Blocks.DARK_OAK_LOG.getDefaultState();
    private static final BlockState WOOD = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState COPPER = Blocks.COPPER_BLOCK.getDefaultState();
    private static final BlockState GREEN = Blocks.MOSS_BLOCK.getDefaultState();
    private static final BlockState LEAVES = Blocks.AZALEA_LEAVES.getDefaultState();
    private static final BlockState FLOWERS = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();

    private MeridianCanopyGymBuilder() {}

    public static BuildResult build(ServerWorld world, BlockPos origin) {
        clearAndFoundation(world, origin);
        buildApproach(world, origin);
        buildEntryHall(world, origin);
        buildAtrium(world, origin);
        buildWestChallengeWing(world, origin);
        buildEastChallengeWing(world, origin);
        buildUpperCirculation(world, origin);
        buildLeaderArena(world, origin);
        buildServiceRoute(world, origin);
        buildRoofOverlook(world, origin);
        buildLandscapeEdges(world, origin);

        return new BuildResult(
                origin,
                origin.add(0, 1, -31),
                origin.add(0, 1, -20),
                origin.add(0, 1, 0),
                origin.add(-23, 1, 0),
                origin.add(23, 1, 0),
                origin.add(0, 9, 3),
                origin.add(0, 1, 23),
                origin.add(-29, -3, 15),
                origin.add(0, 18, 7),
                WIDTH,
                DEPTH,
                MAX_HEIGHT
        );
    }

    private static void clearAndFoundation(ServerWorld world, BlockPos o) {
        for (int x = -33; x <= 33; x++) {
            for (int z = -33; z <= 33; z++) {
                BlockPos ground = o.add(x, 0, z);
                world.setBlockState(ground.down(), Blocks.DIRT.getDefaultState());
                world.setBlockState(ground, x * x + z * z < 31 * 31
                        ? Blocks.GRASS_BLOCK.getDefaultState()
                        : Blocks.COARSE_DIRT.getDefaultState());
                for (int y = 1; y <= MAX_HEIGHT; y++) {
                    world.setBlockState(o.add(x, y, z), AIR);
                }
            }
        }

        fill(world, o, -31, -1, -27, 31, 0, 29, FOUNDATION);
        fill(world, o, -30, 0, -26, 30, 0, 28, PRIMARY);
    }

    private static void buildApproach(ServerWorld world, BlockPos o) {
        fill(world, o, -8, 0, -33, 8, 0, -27, Blocks.SMOOTH_STONE.getDefaultState());
        fill(world, o, -3, 1, -32, 3, 1, -27, Blocks.STONE_BRICK_SLAB.getDefaultState());

        for (int z = -32; z <= -27; z += 3) {
            pillar(world, o, -10, 1, z, 4, FRAME);
            pillar(world, o, 10, 1, z, 4, FRAME);
            world.setBlockState(o.add(-10, 5, z), Blocks.LANTERN.getDefaultState());
            world.setBlockState(o.add(10, 5, z), Blocks.LANTERN.getDefaultState());
        }

        fill(world, o, -18, 0, -33, -12, 0, -27, GREEN);
        fill(world, o, 12, 0, -33, 18, 0, -27, GREEN);
        grove(world, o, -15, 1, -29, 5);
        grove(world, o, 15, 1, -29, 5);

        fill(world, o, -5, 1, -27, 5, 1, -27, COPPER);
        fill(world, o, -4, 2, -27, 4, 5, -27, GLASS);
        pillar(world, o, -6, 1, -27, 7, FRAME);
        pillar(world, o, 6, 1, -27, 7, FRAME);
        beamX(world, o, -6, 6, 7, -27, FRAME);
    }

    private static void buildEntryHall(ServerWorld world, BlockPos o) {
        shell(world, o, -14, 0, -26, 14, 8, -14, PRIMARY, FRAME);
        clear(world, o, -12, 1, -25, 12, 7, -15);

        // Public lobby axis.
        fill(world, o, -4, 0, -26, 4, 0, -14, Blocks.SMOOTH_STONE.getDefaultState());
        fill(world, o, -13, 0, -25, -6, 0, -18, WOOD);
        fill(world, o, 6, 0, -25, 13, 0, -18, WOOD);

        // Reception and waiting alcoves.
        fill(world, o, -11, 1, -22, -7, 2, -21, Blocks.BOOKSHELF.getDefaultState());
        fill(world, o, 7, 1, -22, 11, 1, -21, Blocks.BARREL.getDefaultState());
        fill(world, o, -12, 1, -16, -8, 1, -15, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, 8, 1, -16, 12, 1, -15, Blocks.DARK_OAK_SLAB.getDefaultState());

        // Clerestory glazing and deep frame rhythm.
        for (int x = -11; x <= 11; x += 4) {
            pillar(world, o, x, 1, -26, 7, FRAME);
            fill(world, o, x + 1, 3, -26, Math.min(x + 3, 12), 6, -26, GLASS);
        }

        // Wide transition into atrium.
        clear(world, o, -6, 1, -14, 6, 6, -14);
        beamX(world, o, -7, 7, 7, -14, FRAME);
    }

    private static void buildAtrium(ServerWorld world, BlockPos o) {
        shell(world, o, -16, 0, -13, 16, 15, 12, PRIMARY, FRAME);
        clear(world, o, -14, 1, -12, 14, 14, 11);

        // Ring circulation.
        fill(world, o, -14, 0, -12, 14, 0, 11, WOOD);
        fill(world, o, -10, 0, -9, 10, 0, 9, GREEN);
        fill(world, o, -6, 0, -6, 6, 0, 6, Blocks.WATER.getDefaultState());
        fill(world, o, -4, 0, -4, 4, 0, 4, GREEN);

        // Central canopy tree / visual anchor.
        for (int y = 1; y <= 12; y++) {
            int width = y < 5 ? 1 : 0;
            fill(world, o, -width, y, -width, width, y, width, FRAME);
        }
        canopy(world, o, 0, 12, 0, 7);
        canopy(world, o, 0, 15, 0, 5);

        // Four planted islands create distinct walkable loops.
        plantedIsland(world, o, -10, 1, -6);
        plantedIsland(world, o, 10, 1, -6);
        plantedIsland(world, o, -10, 1, 7);
        plantedIsland(world, o, 10, 1, 7);

        // Tall window bays.
        for (int z = -9; z <= 9; z += 6) {
            fill(world, o, -16, 3, z - 2, -16, 10, z + 2, GLASS);
            fill(world, o, 16, 3, z - 2, 16, 10, z + 2, GLASS);
            pillar(world, o, -15, 1, z - 3, 11, FRAME);
            pillar(world, o, 15, 1, z - 3, 11, FRAME);
        }

        // Skylight spine.
        fill(world, o, -6, 15, -8, 6, 15, 8, GLASS);
        beamZ(world, o, -8, 8, 15, -7, FRAME);
        beamZ(world, o, -8, 8, 15, 7, FRAME);
    }

    private static void buildWestChallengeWing(ServerWorld world, BlockPos o) {
        shell(world, o, -30, 0, -10, -17, 10, 12, WEATHERED, FRAME);
        clear(world, o, -28, 1, -8, -18, 9, 10);

        // Three stepped gardens force route choice around planted masses.
        fill(world, o, -28, 0, -8, -18, 0, -3, GREEN);
        fill(world, o, -28, 1, -1, -22, 1, 4, GREEN);
        fill(world, o, -25, 2, 6, -18, 2, 10, GREEN);

        // Walkable perimeter loop.
        fill(world, o, -21, 1, -7, -18, 1, 9, WOOD);
        fill(world, o, -28, 1, 7, -18, 1, 9, WOOD);
        fill(world, o, -28, 1, -7, -26, 1, 7, WOOD);

        // Sight-blocking planting and observation niches.
        for (int z = -6; z <= 8; z += 7) {
            grove(world, o, -25, 2, z, 3);
        }
        fill(world, o, -29, 3, -5, -29, 7, -1, GLASS);
        fill(world, o, -29, 3, 4, -29, 7, 8, GLASS);

        clear(world, o, -17, 1, -4, -17, 5, 2);
        clear(world, o, -17, 1, 7, -17, 5, 9);
    }

    private static void buildEastChallengeWing(ServerWorld world, BlockPos o) {
        shell(world, o, 17, 0, -10, 30, 10, 12, PRIMARY, FRAME);
        clear(world, o, 18, 1, -8, 28, 9, 10);

        // Water/control court: spatial puzzle-ready without assigning PTU terrain effects.
        fill(world, o, 19, 0, -7, 27, 0, 9, Blocks.SMOOTH_STONE.getDefaultState());
        fill(world, o, 20, 0, -6, 22, 0, 8, Blocks.WATER.getDefaultState());
        fill(world, o, 25, 0, -6, 27, 0, 8, Blocks.WATER.getDefaultState());
        fill(world, o, 23, 0, -6, 24, 0, 8, WOOD);

        // Cross bridges and control islands.
        fill(world, o, 19, 1, -1, 27, 1, 1, Blocks.DARK_OAK_SLAB.getDefaultState());
        fill(world, o, 23, 1, -7, 24, 1, 9, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int z : new int[]{-5, 4, 8}) {
            world.setBlockState(o.add(23, 2, z), COPPER);
            world.setBlockState(o.add(24, 2, z), LIGHT);
        }

        fill(world, o, 29, 3, -5, 29, 7, -1, GLASS);
        fill(world, o, 29, 3, 4, 29, 7, 8, GLASS);
        clear(world, o, 17, 1, -4, 17, 5, 2);
        clear(world, o, 17, 1, 7, 17, 5, 9);
    }

    private static void buildUpperCirculation(ServerWorld world, BlockPos o) {
        // Atrium balcony ring.
        fill(world, o, -14, 8, -11, 14, 8, -9, WOOD);
        fill(world, o, -14, 8, 9, 14, 8, 11, WOOD);
        fill(world, o, -14, 8, -8, -12, 8, 8, WOOD);
        fill(world, o, 12, 8, -8, 14, 8, 8, WOOD);

        // Central bridge gives a strong second-level reveal.
        fill(world, o, -12, 8, 2, 12, 8, 4, Blocks.DARK_OAK_SLAB.getDefaultState());
        beamX(world, o, -12, 12, 10, 2, FRAME);
        beamX(world, o, -12, 12, 10, 4, FRAME);

        // Two broad stair ramps built as stepped platforms.
        for (int i = 0; i < 8; i++) {
            fill(world, o, -14, 1 + i, -12 + i, -11, 1 + i, -10 + i, Blocks.STONE_BRICK_SLAB.getDefaultState());
            fill(world, o, 11, 1 + i, 10 - i, 14, 1 + i, 12 - i, Blocks.STONE_BRICK_SLAB.getDefaultState());
        }

        // Upper links into both wings.
        fill(world, o, -20, 8, -2, -14, 8, 0, WOOD);
        fill(world, o, 14, 8, -2, 20, 8, 0, WOOD);
        clear(world, o, -17, 8, -2, -16, 10, 0);
        clear(world, o, 16, 8, -2, 17, 10, 0);
    }

    private static void buildLeaderArena(ServerWorld world, BlockPos o) {
        shell(world, o, -22, 0, 13, 22, 13, 30, PRIMARY, FRAME);
        clear(world, o, -20, 1, 14, 20, 12, 28);

        // Arena floor is large enough for future explicit battle-grid transforms.
        fill(world, o, -15, 0, 17, 15, 0, 27, Blocks.SMOOTH_STONE.getDefaultState());
        fill(world, o, -13, 0, 19, 13, 0, 25, GREEN);
        fill(world, o, -9, 0, 21, 9, 0, 23, Blocks.SMOOTH_STONE.getDefaultState());

        // Spectator terraces, not just flat walls.
        for (int tier = 0; tier < 4; tier++) {
            int y = 1 + tier;
            int xOuter = 20 - tier * 2;
            fill(world, o, -xOuter, y, 15 + tier, -16, y, 27 - tier, Blocks.STONE_BRICK_SLAB.getDefaultState());
            fill(world, o, 16, y, 15 + tier, xOuter, y, 27 - tier, Blocks.STONE_BRICK_SLAB.getDefaultState());
        }

        // Leader dais and rear botanical wall.
        fill(world, o, -5, 1, 26, 5, 2, 29, WOOD);
        fill(world, o, -12, 1, 29, 12, 8, 29, LEAVES);
        fill(world, o, -8, 3, 29, 8, 7, 29, FLOWERS);
        fill(world, o, -3, 1, 29, 3, 6, 29, GLASS);

        // Upper viewing gallery reconnects to atrium circulation.
        fill(world, o, -20, 9, 14, 20, 9, 16, WOOD);
        fill(world, o, -20, 9, 26, 20, 9, 28, WOOD);
        fill(world, o, -20, 9, 17, -18, 9, 25, WOOD);
        fill(world, o, 18, 9, 17, 20, 9, 25, WOOD);
        clear(world, o, -5, 9, 13, 5, 11, 13);

        for (int x = -18; x <= 18; x += 6) {
            pillar(world, o, x, 1, 13, 11, FRAME);
            pillar(world, o, x, 1, 30, 11, FRAME);
        }
    }

    private static void buildServiceRoute(ServerWorld world, BlockPos o) {
        // Back-of-house underground loop from west exterior to arena backstage.
        fill(world, o, -31, -4, -5, -25, -1, 23, FOUNDATION);
        clear(world, o, -30, -3, -4, -26, -1, 22);
        fill(world, o, -30, -4, -4, -26, -4, 22, Blocks.DEEPSLATE_BRICKS.getDefaultState());

        fill(world, o, -25, -4, 19, -6, -1, 23, FOUNDATION);
        clear(world, o, -24, -3, 20, -7, -1, 22);
        fill(world, o, -24, -4, 20, -7, -4, 22, Blocks.DEEPSLATE_BRICKS.getDefaultState());

        // Maintenance rooms make the route functional rather than a secret hallway only.
        shell(world, o, -24, -4, 11, -17, 0, 18, FOUNDATION, FRAME);
        clear(world, o, -23, -3, 12, -18, -1, 17);
        world.setBlockState(o.add(-22, -3, 13), Blocks.CRAFTING_TABLE.getDefaultState());
        world.setBlockState(o.add(-20, -3, 13), Blocks.FURNACE.getDefaultState());
        world.setBlockState(o.add(-18, -3, 13), Blocks.BARREL.getDefaultState());
        world.setBlockState(o.add(-22, -3, 16), Blocks.CAULDRON.getDefaultState());

        // Vertical backstage connection represented as an open shaft for later ladder/elevator logic.
        clear(world, o, -8, -3, 20, -6, 3, 22);
        pillar(world, o, -8, -3, 20, 7, FRAME);
        pillar(world, o, -6, -3, 22, 7, FRAME);
    }

    private static void buildRoofOverlook(ServerWorld world, BlockPos o) {
        // Roof garden above lobby/atrium threshold.
        fill(world, o, -10, 16, -12, 10, 16, -7, WOOD);
        fill(world, o, -8, 17, -11, 8, 17, -8, GREEN);
        for (int x = -7; x <= 7; x += 7) {
            grove(world, o, x, 18, -9, 2);
        }
        fill(world, o, -10, 17, -12, 10, 17, -12, Blocks.IRON_BARS.getDefaultState());
        fill(world, o, -10, 17, -7, 10, 17, -7, Blocks.IRON_BARS.getDefaultState());
        fill(world, o, -10, 17, -12, -10, 17, -7, Blocks.IRON_BARS.getDefaultState());
        fill(world, o, 10, 17, -12, 10, 17, -7, Blocks.IRON_BARS.getDefaultState());

        // Tall beacon-like botanical crown makes the building readable from approach distance.
        for (int y = 16; y <= 21; y++) {
            int radius = Math.max(1, 5 - (y - 16));
            for (int x = -radius; x <= radius; x++) {
                world.setBlockState(o.add(x, y, 7), y % 2 == 0 ? COPPER : FRAME);
            }
        }
        world.setBlockState(o.add(0, 22, 7), LIGHT);
    }

    private static void buildLandscapeEdges(ServerWorld world, BlockPos o) {
        // Exterior paths create two side approaches instead of a single front door funnel.
        for (int z = -25; z <= 24; z++) {
            fill(world, o, -33, 0, z, -31, 0, z, Blocks.DIRT_PATH.getDefaultState());
            fill(world, o, 31, 0, z, 33, 0, z, Blocks.DIRT_PATH.getDefaultState());
        }

        for (int z = -22; z <= 25; z += 8) {
            grove(world, o, -32, 1, z, 3);
            grove(world, o, 32, 1, z + 3, 3);
        }

        // Rear garden / service yard keeps the backside authored.
        fill(world, o, -28, 0, 31, 28, 0, 33, Blocks.DIRT_PATH.getDefaultState());
        fill(world, o, -30, 0, 29, -23, 0, 33, GREEN);
        fill(world, o, 23, 0, 29, 30, 0, 33, GREEN);
        grove(world, o, -26, 1, 31, 4);
        grove(world, o, 26, 1, 31, 4);
    }

    private static void shell(
            ServerWorld world,
            BlockPos o,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState wall,
            BlockState frame
    ) {
        fill(world, o, minX, minY, minZ, maxX, minY, maxZ, FOUNDATION);
        fill(world, o, minX, maxY, minZ, maxX, maxY, maxZ, wall);
        fill(world, o, minX, minY, minZ, minX, maxY, maxZ, wall);
        fill(world, o, maxX, minY, minZ, maxX, maxY, maxZ, wall);
        fill(world, o, minX, minY, minZ, maxX, maxY, minZ, wall);
        fill(world, o, minX, minY, maxZ, maxX, maxY, maxZ, wall);

        pillar(world, o, minX, minY, minZ, maxY - minY + 1, frame);
        pillar(world, o, maxX, minY, minZ, maxY - minY + 1, frame);
        pillar(world, o, minX, minY, maxZ, maxY - minY + 1, frame);
        pillar(world, o, maxX, minY, maxZ, maxY - minY + 1, frame);
    }

    private static void plantedIsland(ServerWorld world, BlockPos o, int x, int y, int z) {
        fill(world, o, x - 2, y, z - 2, x + 2, y, z + 2, GREEN);
        world.setBlockState(o.add(x, y + 1, z), FLOWERS);
        world.setBlockState(o.add(x - 1, y + 1, z), LEAVES);
        world.setBlockState(o.add(x + 1, y + 1, z), LEAVES);
        world.setBlockState(o.add(x, y + 1, z - 1), LEAVES);
        world.setBlockState(o.add(x, y + 1, z + 1), LEAVES);
        world.setBlockState(o.add(x, y, z), LIGHT);
    }

    private static void grove(ServerWorld world, BlockPos o, int x, int y, int z, int radius) {
        pillar(world, o, x, y, z, Math.max(3, radius + 1), FRAME);
        canopy(world, o, x, y + radius + 1, z, radius);
    }

    private static void canopy(ServerWorld world, BlockPos o, int cx, int y, int cz, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distance = Math.abs(x) + Math.abs(z);
                if (distance <= radius + 2 && x * x + z * z <= radius * radius + 4) {
                    world.setBlockState(o.add(cx + x, y, cz + z), (x + z) % 5 == 0 ? FLOWERS : LEAVES);
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

    private static void pillar(ServerWorld world, BlockPos o, int x, int y, int z, int height, BlockState state) {
        for (int i = 0; i < height; i++) {
            world.setBlockState(o.add(x, y + i, z), state);
        }
    }

    private static void beamX(ServerWorld world, BlockPos o, int minX, int maxX, int y, int z, BlockState state) {
        fill(world, o, minX, y, z, maxX, y, z, state);
    }

    private static void beamZ(ServerWorld world, BlockPos o, int minZ, int maxZ, int y, int x, BlockState state) {
        fill(world, o, x, y, minZ, x, y, maxZ, state);
    }

    public record BuildResult(
            BlockPos origin,
            BlockPos approach,
            BlockPos lobby,
            BlockPos atrium,
            BlockPos westChallenge,
            BlockPos eastChallenge,
            BlockPos upperBridge,
            BlockPos leaderArena,
            BlockPos maintenanceShortcut,
            BlockPos roofOverlook,
            int width,
            int depth,
            int height
    ) {}
}
