package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.fill;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.litLantern;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.stair;

/**
 * Destructive exterior architecture pass for OI-107.
 *
 * The first Palace prototype proved the nineteen interiors and exact-server pipeline, but its
 * exterior was a rectangular masonry shell with one continuous stepped roof. This pass deliberately
 * removes that prototype skin after the rooms have been authored and rebuilds a palace silhouette
 * around them: pavilion hierarchy, recessed facade bays, a real entrance corps-de-logis, corner
 * towers, balconies, dormers, multiple closed roofs, chimneys and a glazed central lantern.
 *
 * Everything remains ordinary Minecraft BlockState geometry. The structural audit still runs after
 * this pass and rejects disconnected ornament.
 */
final class OurosGrandPalaceExteriorRebuildPass {
    private static final BlockState ASHLAR = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState STONE = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState DEEP_STONE = Blocks.DEEPSLATE_BRICKS.getDefaultState();
    private static final BlockState DARK_TRIM = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState WARM_TRIM = Blocks.BAMBOO_MOSAIC.getDefaultState();
    private static final BlockState COPPER = Blocks.WAXED_OXIDIZED_COPPER.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState PANE = Blocks.GLASS_PANE.getDefaultState();
    private static final BlockState ROOF = Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState();
    private static final BlockState ROOF_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
    private static final BlockState COPPER_ROOF = Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.getDefaultState();
    private static final BlockState COPPER_SLAB = Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.getDefaultState();

    private OurosGrandPalaceExteriorRebuildPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        removePrototypeRoof(world, o);
        carvePrototypeFacade(world, o);
        buildDeepPlinth(world, o);
        buildFrontCorpsDeLogis(world, o);
        buildFrontPavilions(world, o);
        buildSideWings(world, o);
        buildRearPavilion(world, o);
        buildCornerTowers(world, o);
        buildLayeredRoofscape(world, o);
        buildCentralGlazedLantern(world, o);
        buildDormersAndChimneys(world, o);
        buildExteriorLightingAndBalconies(world, o);
    }

    private static void removePrototypeRoof(ServerWorld world, BlockPos o) {
        // Interior authored ceilings end at Y=29. Everything above is exterior roof territory.
        clear(world, o, OurosGrandPalace.MIN_X, 31, OurosGrandPalace.MIN_Z,
                OurosGrandPalace.MAX_X, OurosGrandPalace.MAX_Y, OurosGrandPalace.MAX_Z);
    }

    private static void carvePrototypeFacade(ServerWorld world, BlockPos o) {
        // Remove only the old outer masonry bands. The authored room shells begin farther inward.
        clear(world, o, -43, 4, -56, -40, 30, 56);
        clear(world, o, 40, 4, -56, 43, 30, 56);
        clear(world, o, -39, 4, -57, 39, 30, -54);
        clear(world, o, -39, 4, 54, 39, 30, 57);
    }

    private static void buildDeepPlinth(ServerWorld world, BlockPos o) {
        // A continuous heavy base visually anchors the many pavilions without turning the facade
        // back into one flat wall.
        fill(world, o, -43, 1, -57, 43, 3, -55, DEEP_STONE);
        fill(world, o, -43, 1, 55, 43, 3, 57, DEEP_STONE);
        fill(world, o, -43, 1, -54, -41, 3, 54, DEEP_STONE);
        fill(world, o, 41, 1, -54, 43, 3, 54, DEEP_STONE);

        // Stone water table and projecting sill course.
        fill(world, o, -42, 4, -56, 42, 4, -55, ASHLAR);
        fill(world, o, -42, 4, 55, 42, 4, 56, ASHLAR);
        fill(world, o, -42, 4, -54, -41, 4, 54, ASHLAR);
        fill(world, o, 41, 4, -54, 42, 4, 54, ASHLAR);
    }

    private static void buildFrontCorpsDeLogis(ServerWorld world, BlockPos o) {
        // Central entrance pavilion. The middle projects visually through paired piers, deep portal,
        // balcony and a pediment instead of reading as a door cut into a warehouse wall.
        facadeWallZ(world, o, -18, 18, -56, 4, 30, STONE);

        for (int x : new int[]{-16, -10, 10, 16}) {
            fill(world, o, x - 1, 4, -56, x + 1, 29, -54, ASHLAR);
            fill(world, o, x - 2, 4, -56, x + 2, 5, -53, DARK_TRIM);
            fill(world, o, x - 2, 27, -56, x + 2, 29, -53, WARM_TRIM);
        }

        // Deep portal and fanlight.
        clear(world, o, -5, 4, -56, 5, 14, -53);
        fill(world, o, -7, 4, -56, -6, 16, -52, ASHLAR);
        fill(world, o, 6, 4, -56, 7, 16, -52, ASHLAR);
        fill(world, o, -7, 15, -56, 7, 17, -52, WARM_TRIM);
        fill(world, o, -4, 14, -54, 4, 16, -54, GLASS);
        for (int x : new int[]{-4, 0, 4}) {
            fill(world, o, x, 14, -55, x, 16, -53, DARK_TRIM);
        }

        // Upper state balcony over the portal.
        fill(world, o, -11, 17, -57, 11, 18, -52, DARK_TRIM);
        fill(world, o, -10, 18, -57, 10, 18, -53, Blocks.DARK_OAK_PLANKS.getDefaultState());
        for (int x = -10; x <= 10; x += 2) {
            fill(world, o, x, 19, -57, x, 20, -57, Blocks.DARK_OAK_FENCE.getDefaultState());
        }
        fill(world, o, -10, 20, -57, 10, 20, -57, Blocks.DARK_OAK_FENCE.getDefaultState());

        // Tall upper windows framed as individual bays.
        deepWindowZ(world, o, -12, -55, 20, 8);
        deepWindowZ(world, o, 0, -55, 20, 8);
        deepWindowZ(world, o, 12, -55, 20, 8);

        // Triangular stepped pediment backed with masonry so the silhouette has mass.
        for (int layer = 0; layer <= 8; layer++) {
            int half = 17 - layer * 2;
            if (half < 1) break;
            fill(world, o, -half, 30 + layer, -56, half, 30 + layer, -53, STONE);
            fill(world, o, -half, 30 + layer, -57, -half, 30 + layer, -52,
                    stair(Blocks.POLISHED_DIORITE_STAIRS.getDefaultState(), Direction.EAST));
            fill(world, o, half, 30 + layer, -57, half, 30 + layer, -52,
                    stair(Blocks.POLISHED_DIORITE_STAIRS.getDefaultState(), Direction.WEST));
        }
        fill(world, o, -2, 34, -52, 2, 38, -52, WARM_TRIM);
        fill(world, o, -1, 35, -51, 1, 37, -51, Blocks.COPPER_GRATE.getDefaultState());
    }

    private static void buildFrontPavilions(ServerWorld world, BlockPos o) {
        // Two broad flanking pavilions are recessed from the central block and broken into window
        // bays. Their corners thicken into quoins, avoiding the original 87-block-long flat plane.
        for (int side : new int[]{-1, 1}) {
            int x1 = side < 0 ? -39 : 20;
            int x2 = side < 0 ? -20 : 39;
            facadeWallZ(world, o, x1, x2, -55, 4, 30, STONE);

            for (int x = x1 + 2; x <= x2 - 2; x += 7) {
                deepWindowZ(world, o, x, -54, 6, 8);
                deepWindowZ(world, o, x, -54, 20, 7);
            }
            for (int x : new int[]{x1, x2}) {
                fill(world, o, x - 1, 4, -56, x + 1, 30, -53, ASHLAR);
                for (int y = 5; y <= 28; y += 4) {
                    fill(world, o, x - 2, y, -56, x + 2, y, -53, WARM_TRIM);
                }
            }
            fill(world, o, x1, 14, -56, x2, 15, -53, DARK_TRIM);
            fill(world, o, x1, 29, -56, x2, 30, -53, ASHLAR);
        }
    }

    private static void buildSideWings(ServerWorld world, BlockPos o) {
        // Long sides become a rhythm of recessed bays separated by projecting pavilions.
        for (int side : new int[]{-1, 1}) {
            int x = side < 0 ? -42 : 42;
            Direction inward = side < 0 ? Direction.EAST : Direction.WEST;
            for (int band = -50; band <= 50; band += 20) {
                int z1 = Math.max(-53, band - 8);
                int z2 = Math.min(53, band + 8);
                facadeWallX(world, o, x, z1, z2, 4, 30, STONE);
                deepWindowX(world, o, x - side, band - 4, 6, 8, inward);
                deepWindowX(world, o, x - side, band + 4, 6, 8, inward);
                deepWindowX(world, o, x - side, band - 4, 20, 7, inward);
                deepWindowX(world, o, x - side, band + 4, 20, 7, inward);

                fill(world, o, x - side * 2, 4, z1, x, 30, z1 + 1, ASHLAR);
                fill(world, o, x - side * 2, 4, z2 - 1, x, 30, z2, ASHLAR);
                fill(world, o, x - side * 2, 14, z1, x, 15, z2, DARK_TRIM);
                fill(world, o, x - side * 2, 29, z1, x, 30, z2, ASHLAR);
            }
        }
    }

    private static void buildRearPavilion(ServerWorld world, BlockPos o) {
        // Rear facade gets its own state-room pavilion instead of mirroring the front warehouse wall.
        facadeWallZ(world, o, -24, 24, 55, 4, 30, STONE);
        for (int x : new int[]{-21, -14, -7, 0, 7, 14, 21}) {
            deepWindowZ(world, o, x, 54, 6, 8);
            deepWindowZ(world, o, x, 54, 20, 7);
        }
        for (int x : new int[]{-24, -12, 12, 24}) {
            fill(world, o, x - 1, 4, 53, x + 1, 30, 56, ASHLAR);
        }
        fill(world, o, -24, 14, 53, 24, 15, 56, DARK_TRIM);
        fill(world, o, -24, 29, 53, 24, 30, 56, WARM_TRIM);
    }

    private static void buildCornerTowers(ServerWorld world, BlockPos o) {
        for (int x : new int[]{-38, 38}) {
            for (int z : new int[]{-52, 52}) {
                // Massive square tower base tied directly into both adjacent facades.
                fill(world, o, x - 5, 4, z - 5, x + 5, 32, z + 5, STONE);
                fill(world, o, x - 6, 4, z - 6, x + 6, 5, z + 6, DARK_TRIM);
                fill(world, o, x - 6, 15, z - 6, x + 6, 16, z + 6, WARM_TRIM);
                fill(world, o, x - 6, 30, z - 6, x + 6, 32, z + 6, ASHLAR);

                // Carve windows back into the mass on outward faces.
                deepWindowZ(world, o, x, z < 0 ? z - 5 : z + 5, 8, 9);
                deepWindowX(world, o, x < 0 ? x - 5 : x + 5, z, 8, 9,
                        x < 0 ? Direction.EAST : Direction.WEST);
                deepWindowZ(world, o, x, z < 0 ? z - 5 : z + 5, 21, 7);
            }
        }
    }

    private static void buildLayeredRoofscape(ServerWorld world, BlockPos o) {
        // Roofs are independent masses resting on Y=30/32 walls. No continuous lid spans the palace.
        hippedRoof(world, o, -39, -19, -53, -31, 31, 10, false);
        hippedRoof(world, o, 19, 39, -53, -31, 31, 10, false);
        hippedRoof(world, o, -39, -19, -25, -3, 31, 9, false);
        hippedRoof(world, o, 19, 39, -25, -3, 31, 9, false);
        hippedRoof(world, o, -39, -19, 3, 25, 31, 9, false);
        hippedRoof(world, o, 19, 39, 3, 25, 31, 9, false);
        hippedRoof(world, o, -39, -19, 31, 53, 31, 10, false);
        hippedRoof(world, o, 19, 39, 31, 53, 31, 10, false);

        // Three taller central ceremonial roofs make the axial sequence legible from outside.
        hippedRoof(world, o, -13, 13, -29, -1, 31, 12, true);
        hippedRoof(world, o, -13, 13, 1, 29, 31, 13, true);
        hippedRoof(world, o, -13, 13, 29, 55, 31, 12, true);

        // The front corps is capped by a steeper mansard behind the pediment.
        hippedRoof(world, o, -19, 19, -55, -29, 31, 14, true);

        // Tower caps rise above the principal eaves.
        for (int x : new int[]{-38, 38}) {
            for (int z : new int[]{-52, 52}) {
                hippedRoof(world, o, x - 6, x + 6, z - 6, z + 6, 33, 10, true);
            }
        }
    }

    private static void buildCentralGlazedLantern(ServerWorld world, BlockPos o) {
        // Closed lantern above Themis Hall. It is deliberately glazed rather than an open cupola.
        fill(world, o, -8, 43, 6, 8, 44, 22, DARK_TRIM);
        for (int x : new int[]{-8, 8}) {
            fill(world, o, x, 45, 6, x, 55, 22, ASHLAR);
            fill(world, o, x, 46, 8, x, 53, 20, PANE);
            for (int z = 8; z <= 20; z += 4) {
                fill(world, o, x, 45, z, x, 55, z, WARM_TRIM);
            }
        }
        for (int z : new int[]{6, 22}) {
            fill(world, o, -8, 45, z, 8, 55, z, ASHLAR);
            fill(world, o, -6, 46, z, 6, 53, z, PANE);
            for (int x = -6; x <= 6; x += 4) {
                fill(world, o, x, 45, z, x, 55, z, WARM_TRIM);
            }
        }
        fill(world, o, -9, 55, 5, 9, 56, 23, DARK_TRIM);
        hippedRoof(world, o, -9, 9, 5, 23, 57, 8, true);
        fill(world, o, -1, 64, 13, 1, 67, 15, COPPER);
        world.setBlockState(o.add(0, 68, 14), Blocks.LIGHTNING_ROD.getDefaultState());
    }

    private static void buildDormersAndChimneys(ServerWorld world, BlockPos o) {
        // Dormers puncture broad roof masses and make upper rooms visible in the silhouette.
        for (int x : new int[]{-32, -24, 24, 32}) {
            dormerNorth(world, o, x, -37, 36);
            dormerSouth(world, o, x, 37, 36);
        }
        for (int z : new int[]{-18, 10, 38}) {
            dormerWest(world, o, -29, z, 36);
            dormerEast(world, o, 29, z, 36);
        }

        // Chimneys are grouped rather than evenly spammed.
        for (int[] p : new int[][]{{-31,-21},{31,-21},{-31,19},{31,19},{-17,43},{17,43}}) {
            fill(world, o, p[0] - 1, 37, p[1] - 1, p[0] + 1, 44, p[1] + 1, Blocks.BRICKS.getDefaultState());
            fill(world, o, p[0] - 2, 44, p[1] - 2, p[0] + 2, 45, p[1] + 2, DARK_TRIM);
            fill(world, o, p[0] - 1, 46, p[1] - 1, p[0] + 1, 46, p[1] + 1, Blocks.BRICK_WALL.getDefaultState());
        }
    }

    private static void buildExteriorLightingAndBalconies(ServerWorld world, BlockPos o) {
        // State balconies break side facades at important rooms.
        for (int side : new int[]{-1, 1}) {
            int x = side < 0 ? -43 : 43;
            for (int z : new int[]{-14, 14, 42}) {
                fill(world, o, x - side * 3, 17, z - 5, x, 18, z + 5, Blocks.DARK_OAK_PLANKS.getDefaultState());
                for (int dz = -5; dz <= 5; dz += 2) {
                    fill(world, o, x, 19, z + dz, x, 20, z + dz, Blocks.DARK_OAK_FENCE.getDefaultState());
                }
                fill(world, o, x, 20, z - 5, x, 20, z + 5, Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }

        // Entrance lanterns are physically supported by facade piers.
        for (int x : new int[]{-12, 12}) {
            world.setBlockState(o.add(x, 10, -57), WARM_TRIM);
            world.setBlockState(o.add(x, 9, -57), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(x, 8, -57), litLantern(true));
        }
    }

    private static void facadeWallZ(ServerWorld world, BlockPos o, int x1, int x2, int z,
                                    int y1, int y2, BlockState state) {
        fill(world, o, x1, y1, z, x2, y2, z + (z < 0 ? 2 : -2), state);
    }

    private static void facadeWallX(ServerWorld world, BlockPos o, int x, int z1, int z2,
                                    int y1, int y2, BlockState state) {
        fill(world, o, x, y1, z1, x + (x < 0 ? 2 : -2), y2, z2, state);
    }

    private static void deepWindowZ(ServerWorld world, BlockPos o, int centerX, int z, int baseY, int height) {
        int outward = z < 0 ? -1 : 1;
        clear(world, o, centerX - 2, baseY, z - outward, centerX + 2, baseY + height, z + outward);
        fill(world, o, centerX - 3, baseY - 1, z, centerX + 3, baseY, z - outward, ASHLAR);
        fill(world, o, centerX - 3, baseY + height, z, centerX + 3, baseY + height + 1, z - outward, ASHLAR);
        fill(world, o, centerX - 3, baseY, z, centerX - 3, baseY + height, z - outward, ASHLAR);
        fill(world, o, centerX + 3, baseY, z, centerX + 3, baseY + height, z - outward, ASHLAR);
        fill(world, o, centerX - 2, baseY + 1, z, centerX + 2, baseY + height - 1, z, PANE);
        fill(world, o, centerX, baseY + 1, z - outward, centerX, baseY + height - 1, z, WARM_TRIM);
        fill(world, o, centerX - 2, baseY + height / 2, z - outward,
                centerX + 2, baseY + height / 2, z, WARM_TRIM);
    }

    private static void deepWindowX(ServerWorld world, BlockPos o, int x, int centerZ, int baseY,
                                    int height, Direction inward) {
        int outward = x < 0 ? -1 : 1;
        clear(world, o, x - outward, baseY, centerZ - 2, x + outward, baseY + height, centerZ + 2);
        fill(world, o, x, baseY - 1, centerZ - 3, x - outward, baseY, centerZ + 3, ASHLAR);
        fill(world, o, x, baseY + height, centerZ - 3, x - outward, baseY + height + 1, centerZ + 3, ASHLAR);
        fill(world, o, x, baseY, centerZ - 3, x - outward, baseY + height, centerZ - 3, ASHLAR);
        fill(world, o, x, baseY, centerZ + 3, x - outward, baseY + height, centerZ + 3, ASHLAR);
        fill(world, o, x, baseY + 1, centerZ - 2, x, baseY + height - 1, centerZ + 2, PANE);
        fill(world, o, x - outward, baseY + 1, centerZ, x, baseY + height - 1, centerZ, WARM_TRIM);
        fill(world, o, x - outward, baseY + height / 2, centerZ - 2,
                x, baseY + height / 2, centerZ + 2, WARM_TRIM);
    }

    private static void hippedRoof(ServerWorld world, BlockPos o,
                                   int x1, int x2, int z1, int z2, int baseY, int maxRise, boolean copperAccent) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int layers = Math.min(maxRise, Math.min((maxX - minX) / 2, (maxZ - minZ) / 2));
        for (int layer = 0; layer <= layers; layer++) {
            int lx1 = minX + layer;
            int lx2 = maxX - layer;
            int lz1 = minZ + layer;
            int lz2 = maxZ - layer;
            int y = baseY + layer;
            BlockState edge = copperAccent && layer % 4 == 0 ? COPPER_ROOF : ROOF;
            for (int x = lx1; x <= lx2; x++) {
                world.setBlockState(o.add(x, y, lz1), stair(edge, Direction.SOUTH));
                world.setBlockState(o.add(x, y, lz2), stair(edge, Direction.NORTH));
            }
            for (int z = lz1 + 1; z <= lz2 - 1; z++) {
                world.setBlockState(o.add(lx1, y, z), stair(edge, Direction.EAST));
                world.setBlockState(o.add(lx2, y, z), stair(edge, Direction.WEST));
            }
            if (layer == layers) {
                fill(world, o, lx1, y, lz1, lx2, y, lz2, copperAccent ? COPPER_SLAB : ROOF_SLAB);
            }
        }
    }

    private static void dormerNorth(ServerWorld world, BlockPos o, int x, int z, int y) {
        fill(world, o, x - 2, y, z, x + 2, y + 5, z + 3, STONE);
        fill(world, o, x - 1, y + 1, z - 1, x + 1, y + 4, z - 1, PANE);
        fill(world, o, x - 2, y + 5, z - 2, x + 2, y + 5, z + 3, DARK_TRIM);
        fill(world, o, x - 1, y + 6, z - 1, x + 1, y + 6, z + 2, COPPER_SLAB);
    }

    private static void dormerSouth(ServerWorld world, BlockPos o, int x, int z, int y) {
        fill(world, o, x - 2, y, z - 3, x + 2, y + 5, z, STONE);
        fill(world, o, x - 1, y + 1, z + 1, x + 1, y + 4, z + 1, PANE);
        fill(world, o, x - 2, y + 5, z - 3, x + 2, y + 5, z + 2, DARK_TRIM);
        fill(world, o, x - 1, y + 6, z - 2, x + 1, y + 6, z + 1, COPPER_SLAB);
    }

    private static void dormerWest(ServerWorld world, BlockPos o, int x, int z, int y) {
        fill(world, o, x, y, z - 2, x + 3, y + 5, z + 2, STONE);
        fill(world, o, x - 1, y + 1, z - 1, x - 1, y + 4, z + 1, PANE);
        fill(world, o, x - 2, y + 5, z - 2, x + 3, y + 5, z + 2, DARK_TRIM);
    }

    private static void dormerEast(ServerWorld world, BlockPos o, int x, int z, int y) {
        fill(world, o, x - 3, y, z - 2, x, y + 5, z + 2, STONE);
        fill(world, o, x + 1, y + 1, z - 1, x + 1, y + 4, z + 1, PANE);
        fill(world, o, x - 3, y + 5, z - 2, x + 2, y + 5, z + 2, DARK_TRIM);
    }
}
