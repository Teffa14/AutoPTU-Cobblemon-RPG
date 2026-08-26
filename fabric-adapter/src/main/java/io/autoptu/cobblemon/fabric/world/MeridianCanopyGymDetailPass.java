package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Architectural second pass for Meridian Canopy Gym.
 *
 * The base builder owns the gameplay volumes. This pass gives those volumes an authored Minecraft
 * facade, roofline, sectional identity and service detail without changing PTU authority or puzzle
 * state. It deliberately stays within the existing 67x67x22 review envelope.
 */
public final class MeridianCanopyGymDetailPass {
    private static final BlockState STONE = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState FRAME = Blocks.DARK_OAK_LOG.getDefaultState();
    private static final BlockState WOOD = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState COPPER = Blocks.COPPER_BLOCK.getDefaultState();
    private static final BlockState GREEN = Blocks.MOSS_BLOCK.getDefaultState();
    private static final BlockState LEAVES = Blocks.AZALEA_LEAVES.getDefaultState();
    private static final BlockState FLOWERS = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();

    private MeridianCanopyGymDetailPass() {}

    public static void apply(ServerWorld world, BlockPos o) {
        buildArrivalTerraces(world, o);
        buildEntryPortico(world, o);
        buildLobbyRoofline(world, o);
        buildAtriumConservatory(world, o);
        buildChallengeWingFacades(world, o);
        buildArenaButtressesAndRoof(world, o);
        buildRoofGardenArchitecture(world, o);
        buildServiceYard(world, o);
        buildNightLighting(world, o);
    }

    private static void buildArrivalTerraces(ServerWorld world, BlockPos o) {
        // Three shallow terraces make the approach read as a civic forecourt rather than a path strip.
        fill(world, o, -12, 0, -33, 12, 0, -31, Blocks.SMOOTH_STONE.getDefaultState());
        fill(world, o, -10, 1, -30, 10, 1, -29, Blocks.STONE_BRICK_SLAB.getDefaultState());
        fill(world, o, -8, 1, -28, 8, 1, -27, Blocks.STONE_BRICK_SLAB.getDefaultState());

        // Retaining edges and reflecting rills frame the entry axis.
        fill(world, o, -22, 0, -33, -20, 1, -27, DEEPSLATE);
        fill(world, o, 20, 0, -33, 22, 1, -27, DEEPSLATE);
        fill(world, o, -19, 0, -32, -17, 0, -28, Blocks.WATER.getDefaultState());
        fill(world, o, 17, 0, -32, 19, 0, -28, Blocks.WATER.getDefaultState());

        for (int x : new int[]{-24, -14, 14, 24}) {
            column(world, o, x, 1, -29, 4, FRAME);
            world.setBlockState(o.add(x, 5, -29), Blocks.LANTERN.getDefaultState());
        }
    }

    private static void buildEntryPortico(ServerWorld world, BlockPos o) {
        // Deep portico with an eleven-block central opening and layered structural rhythm.
        for (int x : new int[]{-13, -9, -5, 5, 9, 13}) {
            column(world, o, x, 1, -28, 8, FRAME);
            column(world, o, x, 1, -27, 8, STONE);
        }
        fill(world, o, -15, 8, -29, 15, 8, -26, WOOD);
        fill(world, o, -13, 9, -28, 13, 9, -27, COPPER);
        fill(world, o, -9, 6, -28, 9, 7, -28, GLASS);

        // Side fins break the long front elevation into readable bays.
        for (int x : new int[]{-14, -10, 10, 14}) {
            fill(world, o, x, 1, -26, x, 7, -24, FRAME);
        }

        // Botanical signage frame without floating text.
        fill(world, o, -4, 9, -29, 4, 10, -29, GREEN);
        fill(world, o, -3, 10, -29, 3, 10, -29, FLOWERS);
    }

    private static void buildLobbyRoofline(ServerWorld world, BlockPos o) {
        // Stepped eaves stop the lobby from reading as one rectangular prism.
        fill(world, o, -16, 8, -26, 16, 8, -14, DEEPSLATE);
        fill(world, o, -14, 9, -24, 14, 9, -15, WOOD);
        fill(world, o, -10, 10, -22, 10, 10, -16, GREEN);

        // Lantern clerestory above the public axis.
        fill(world, o, -5, 10, -22, 5, 13, -16, GLASS);
        for (int x : new int[]{-5, 0, 5}) {
            column(world, o, x, 10, -22, 4, FRAME);
            column(world, o, x, 10, -16, 4, FRAME);
        }
        fill(world, o, -6, 14, -23, 6, 14, -15, COPPER);
        fill(world, o, -4, 14, -21, 4, 14, -17, GLASS);
    }

    private static void buildAtriumConservatory(ServerWorld world, BlockPos o) {
        // External timber ribs and stone feet give the atrium a conservatory expression.
        for (int z = -12; z <= 12; z += 4) {
            fill(world, o, -17, 0, z, -17, 2, z, STONE);
            fill(world, o, 17, 0, z, 17, 2, z, STONE);
            column(world, o, -17, 3, z, 11, FRAME);
            column(world, o, 17, 3, z, 11, FRAME);
        }

        // High glazed clerestory bands reveal the central tree from outside.
        fill(world, o, -17, 11, -10, -17, 14, 10, GLASS);
        fill(world, o, 17, 11, -10, 17, 14, 10, GLASS);

        // Stepped greenhouse roof. Ribs rise toward the center instead of making a flat lid.
        for (int step = 0; step <= 6; step++) {
            int x = 16 - step * 2;
            int y = 15 + step / 2;
            fill(world, o, -x, y, -11, -x, y, 11, step % 2 == 0 ? FRAME : COPPER);
            fill(world, o, x, y, -11, x, y, 11, step % 2 == 0 ? FRAME : COPPER);
            if (x > 1) {
                fill(world, o, -x + 1, y, -10, x - 1, y, -10, GLASS);
                fill(world, o, -x + 1, y, 10, x - 1, y, 10, GLASS);
            }
        }
        fill(world, o, -3, 19, -9, 3, 19, 9, GLASS);
        fill(world, o, 0, 20, -9, 0, 20, 9, FRAME);

        // Hanging lights establish an interior vertical datum around the canopy.
        for (int[] p : new int[][]{{-11, -7}, {11, -7}, {-11, 7}, {11, 7}}) {
            column(world, o, p[0], 10, p[1], 3, Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(p[0], 9, p[1]), Blocks.LANTERN.getDefaultState());
        }
    }

    private static void buildChallengeWingFacades(ServerWorld world, BlockPos o) {
        // West garden wing: layered masonry feet, deep timber bays and planted roof edge.
        for (int z = -9; z <= 11; z += 5) {
            fill(world, o, -31, 0, z, -30, 2, z, MOSSY);
            column(world, o, -31, 3, z, 7, FRAME);
            fill(world, o, -30, 4, z + 1, -30, 7, Math.min(z + 3, 11), GLASS);
        }
        fill(world, o, -31, 10, -11, -17, 10, 13, WOOD);
        fill(world, o, -30, 11, -9, -19, 11, 10, GREEN);
        for (int z : new int[]{-6, 2, 9}) {
            column(world, o, -27, 12, z, 2, FRAME);
            leafCluster(world, o, -27, 14, z, 2);
        }

        // East water wing: stronger civic masonry with copper/glass control lanterns.
        for (int z = -9; z <= 11; z += 5) {
            fill(world, o, 30, 0, z, 31, 2, z, DEEPSLATE);
            column(world, o, 31, 3, z, 7, STONE);
            fill(world, o, 30, 4, z + 1, 30, 7, Math.min(z + 3, 11), GLASS);
        }
        fill(world, o, 17, 10, -11, 31, 10, 13, DEEPSLATE);
        for (int z : new int[]{-6, 1, 8}) {
            fill(world, o, 24, 11, z - 1, 28, 14, z + 1, GLASS);
            column(world, o, 23, 11, z, 4, COPPER);
            column(world, o, 29, 11, z, 4, COPPER);
            fill(world, o, 23, 15, z - 1, 29, 15, z + 1, COPPER);
        }
    }

    private static void buildArenaButtressesAndRoof(ServerWorld world, BlockPos o) {
        // Deep buttresses make the arena read as the structural climax of the complex.
        for (int x : new int[]{-22, -16, -10, 10, 16, 22}) {
            fill(world, o, x - 1, 0, 13, x + 1, 4, 14, DEEPSLATE);
            column(world, o, x, 5, 13, 8, FRAME);
            fill(world, o, x - 1, 0, 29, x + 1, 4, 30, DEEPSLATE);
            column(world, o, x, 5, 30, 8, FRAME);
        }

        // Clerestory reveals the arena volume at night.
        fill(world, o, -18, 10, 13, 18, 12, 13, GLASS);
        fill(world, o, -18, 10, 30, 18, 12, 30, GLASS);

        // Ribbed stepped roof over the battle hall. The peak remains below y=22.
        for (int step = 0; step <= 7; step++) {
            int x = 22 - step * 3;
            int y = 14 + step / 2;
            if (x < 1) {
                break;
            }
            fill(world, o, -x, y, 14, -x, y, 29, step % 2 == 0 ? FRAME : COPPER);
            fill(world, o, x, y, 14, x, y, 29, step % 2 == 0 ? FRAME : COPPER);
        }
        fill(world, o, -3, 18, 15, 3, 18, 29, GLASS);
        fill(world, o, 0, 19, 16, 0, 19, 28, FRAME);

        // Corner towers and gallery lanterns make the rear elevation authored too.
        for (int x : new int[]{-23, 23}) {
            fill(world, o, x - 1, 0, 27, x + 1, 13, 30, STONE);
            fill(world, o, x - 2, 14, 26, x + 2, 14, 31, COPPER);
            world.setBlockState(o.add(x, 15, 28), LIGHT);
        }
    }

    private static void buildRoofGardenArchitecture(ServerWorld world, BlockPos o) {
        // Pergola around the existing roof garden.
        for (int x = -10; x <= 10; x += 5) {
            column(world, o, x, 17, -12, 4, FRAME);
            column(world, o, x, 17, -7, 4, FRAME);
        }
        fill(world, o, -11, 21, -12, 11, 21, -12, FRAME);
        fill(world, o, -11, 21, -7, 11, 21, -7, FRAME);
        for (int z = -11; z <= -8; z++) {
            fill(world, o, -10, 21, z, 10, 21, z, z % 2 == 0 ? LEAVES : FRAME);
        }

        // Expand the botanical crown into a 3D lantern rather than a single flat stripe.
        for (int y = 16; y <= 21; y++) {
            int r = Math.max(1, 4 - (y - 16) / 2);
            fill(world, o, -r, y, 7 - r, -r, y, 7 + r, COPPER);
            fill(world, o, r, y, 7 - r, r, y, 7 + r, COPPER);
            fill(world, o, -r, y, 7 - r, r, y, 7 - r, FRAME);
            fill(world, o, -r, y, 7 + r, r, y, 7 + r, FRAME);
            if (y % 2 == 0) {
                fill(world, o, -r + 1, y, 7 - r + 1, r - 1, y, 7 + r - 1, GLASS);
            }
        }
        world.setBlockState(o.add(0, 22, 7), LIGHT);
    }

    private static void buildServiceYard(ServerWorld world, BlockPos o) {
        // The rear must read as a working institution rather than dead scenery.
        fill(world, o, -29, 0, 31, -12, 0, 33, Blocks.COARSE_DIRT.getDefaultState());
        fill(world, o, 12, 0, 31, 29, 0, 33, Blocks.COARSE_DIRT.getDefaultState());

        for (int x : new int[]{-27, -24, -21}) {
            world.setBlockState(o.add(x, 1, 32), Blocks.BARREL.getDefaultState());
            world.setBlockState(o.add(x, 2, 32), Blocks.BARREL.getDefaultState());
        }
        for (int x : new int[]{21, 24, 27}) {
            world.setBlockState(o.add(x, 1, 32), Blocks.CAULDRON.getDefaultState());
        }

        // Loading canopy and screened utility bay.
        column(world, o, -11, 1, 31, 5, FRAME);
        column(world, o, 11, 1, 31, 5, FRAME);
        fill(world, o, -12, 6, 30, 12, 6, 33, DEEPSLATE);
        fill(world, o, -9, 1, 32, 9, 3, 33, Blocks.IRON_BARS.getDefaultState());
    }

    private static void buildNightLighting(ServerWorld world, BlockPos o) {
        int[][] points = {
                {-14, 3, -27}, {14, 3, -27},
                {-16, 5, -8}, {16, 5, -8}, {-16, 5, 8}, {16, 5, 8},
                {-29, 4, -6}, {-29, 4, 8}, {29, 4, -6}, {29, 4, 8},
                {-20, 5, 14}, {20, 5, 14}, {-20, 5, 29}, {20, 5, 29}
        };
        for (int[] p : points) {
            world.setBlockState(o.add(p[0], p[1], p[2]), LIGHT);
        }
    }

    private static void leafCluster(ServerWorld world, BlockPos o, int cx, int y, int cz, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius + 1) {
                    world.setBlockState(o.add(cx + x, y, cz + z), (x + z) % 3 == 0 ? FLOWERS : LEAVES);
                }
            }
        }
    }

    private static void column(ServerWorld world, BlockPos o, int x, int y, int z, int height, BlockState state) {
        for (int i = 0; i < height; i++) {
            world.setBlockState(o.add(x, y + i, z), state);
        }
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
}
