package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Small-scale architectural and decorative pass for Meridian Canopy Gym.
 *
 * The visual language is transformed from a user-provided layered homestead/bastion reference:
 * heavy masonry feet, cantilevered timber galleries, deep dark eaves, bracket rhythm, small windows,
 * railings, lanterns, roof finials, planters and working-service clutter. The Gym keeps its botanical
 * civic identity; this pass borrows composition/detail density rather than copying the source build.
 *
 * Every block in this pass is Minecraft presentation only. Decorative materials create no PTU
 * terrain, movement, Accuracy, Evasion, damage or status rules.
 */
public final class MeridianCanopyGymDecorativePass {
    private static final BlockState DARK_ROOF = Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState();
    private static final BlockState DARK_ROOF_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
    private static final BlockState RIDGE = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();
    private static final BlockState FRAME = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState WARM_FRAME = Blocks.STRIPPED_OAK_LOG.getDefaultState();
    private static final BlockState RAIL = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState BRACKET = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState SHUTTER = Blocks.DARK_OAK_TRAPDOOR.getDefaultState();
    private static final BlockState GLASS_PANE = Blocks.GLASS_PANE.getDefaultState();
    private static final BlockState STONE_WALL = Blocks.STONE_BRICK_WALL.getDefaultState();
    private static final BlockState MOSSY_WALL = Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState();
    private static final BlockState COPPER_SCREEN = Blocks.OXIDIZED_COPPER_GRATE.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();
    private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState();
    private static final BlockState MOSS = Blocks.MOSS_CARPET.getDefaultState();

    private MeridianCanopyGymDecorativePass() {}

    public static void apply(ServerWorld world, BlockPos origin) {
        detailEntryEaves(world, origin);
        buildWestKeeperPavilion(world, origin);
        detailAtriumGalleries(world, origin);
        detailEastWaterWing(world, origin);
        detailArenaInstitution(world, origin);
        weatherFoundations(world, origin);
        detailServiceYard(world, origin);
    }

    /** Deep layered entry eaves and bracket/lantern rhythm replace the single-band portico read. */
    private static void detailEntryEaves(ServerWorld world, BlockPos o) {
        BlockState southRoof = facing(DARK_ROOF, Direction.SOUTH);
        BlockState northRoof = facing(DARK_ROOF, Direction.NORTH);

        for (int x = -16; x <= 16; x++) {
            placeIfAir(world, o.add(x, 9, -30), southRoof);
            if (Math.abs(x) <= 13) {
                placeIfAir(world, o.add(x, 10, -29), DARK_ROOF_SLAB);
            }
        }

        for (int x = -11; x <= 11; x++) {
            placeIfAir(world, o.add(x, 13, -25), southRoof);
            if (Math.abs(x) <= 8) {
                placeIfAir(world, o.add(x, 14, -24), northRoof);
            }
        }

        for (int x = -14; x <= 14; x += 4) {
            placeIfAir(world, o.add(x, 8, -30), BRACKET);
            placeIfAir(world, o.add(x, 7, -30), BRACKET);
            if (Math.floorMod(x, 8) == 2 || Math.floorMod(x, 8) == 6) {
                placeIfAir(world, o.add(x, 6, -30), LANTERN);
            }
        }

        // Warm secondary timber breaks the dark structural frame without changing the main palette.
        for (int x : new int[]{-12, -8, 8, 12}) {
            columnIfAir(world, o, x, 3, -29, 4, WARM_FRAME);
        }
    }

    /**
     * A compact roof pavilion above the west garden wing is the strongest direct adaptation of the
     * reference: masonry below, timber gallery in the middle, layered dark roof and fine roofline.
     */
    private static void buildWestKeeperPavilion(ServerWorld world, BlockPos o) {
        // Existing planted roof at y=11 becomes the terrace/deck for a small keeper/observation room.
        for (int x : new int[]{-29, -21}) {
            for (int z : new int[]{-5, 5}) {
                columnIfAir(world, o, x, 12, z, 4, FRAME);
            }
        }
        columnIfAir(world, o, -25, 12, -5, 4, WARM_FRAME);
        columnIfAir(world, o, -25, 12, 5, 4, WARM_FRAME);

        // Narrow glazed bays and open trapdoor shutters add human-scale facade detail.
        for (int z : new int[]{-2, 0, 2}) {
            placeIfAir(world, o.add(-29, 13, z), GLASS_PANE);
            placeIfAir(world, o.add(-29, 14, z), GLASS_PANE);
        }
        placeIfAir(world, o.add(-30, 13, -3), openShutter(Direction.EAST));
        placeIfAir(world, o.add(-30, 13, 3), openShutter(Direction.EAST));
        placeIfAir(world, o.add(-20, 13, -3), openShutter(Direction.WEST));
        placeIfAir(world, o.add(-20, 13, 3), openShutter(Direction.WEST));

        // Timber balcony wraps three sides but leaves the atrium-facing connection readable.
        for (int x = -30; x <= -20; x++) {
            if (x < -26 || x > -24) {
                placeIfAir(world, o.add(x, 12, -6), RAIL);
            }
            placeIfAir(world, o.add(x, 12, 6), RAIL);
        }
        for (int z = -5; z <= 5; z++) {
            placeIfAir(world, o.add(-30, 12, z), RAIL);
        }

        // Deep two-stage gable roof. Stairs make the edge read at close range; slabs keep the ridge thin.
        for (int step = 0; step < 4; step++) {
            int y = 16 + step;
            int northZ = -7 + step * 2;
            int southZ = 7 - step * 2;
            int minX = -31 + step;
            int maxX = -19 - step;
            for (int x = minX; x <= maxX; x++) {
                placeIfAir(world, o.add(x, y, northZ), facing(DARK_ROOF, Direction.SOUTH));
                placeIfAir(world, o.add(x, y, southZ), facing(DARK_ROOF, Direction.NORTH));
            }
        }
        for (int x = -27; x <= -23; x++) {
            placeIfAir(world, o.add(x, 20, 0), DARK_ROOF_SLAB);
        }

        // Brackets, lanterns and ridge finials are intentionally small blocks with large visual payoff.
        for (int x : new int[]{-29, -25, -21}) {
            placeIfAir(world, o.add(x, 15, -6), BRACKET);
            placeIfAir(world, o.add(x, 15, 6), BRACKET);
        }
        hangLantern(world, o.add(-29, 15, -4), 1);
        hangLantern(world, o.add(-21, 15, 4), 1);
        for (int x : new int[]{-27, -25, -23}) {
            placeIfAir(world, o.add(x, 21, 0), RIDGE);
            placeIfAir(world, o.add(x, 22, 0), Blocks.LIGHTNING_ROD.getDefaultState());
        }

        placeIfAir(world, o.add(-29, 12, -4), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        placeIfAir(world, o.add(-29, 12, 4), Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
    }

    /** Railings, planter boxes and suspended lights make the atrium circulation read as occupied space. */
    private static void detailAtriumGalleries(ServerWorld world, BlockPos o) {
        for (int x = -13; x <= 13; x++) {
            if (Math.abs(x) > 4) {
                placeIfAir(world, o.add(x, 9, -9), RAIL);
            }
            placeIfAir(world, o.add(x, 9, 11), RAIL);
        }
        for (int z = -8; z <= 8; z++) {
            if (Math.abs(z - 3) > 2) {
                placeIfAir(world, o.add(-12, 9, z), RAIL);
                placeIfAir(world, o.add(14, 9, z), RAIL);
            }
        }

        for (int[] p : new int[][]{{-11, -8}, {11, -8}, {-11, 9}, {11, 9}}) {
            placeIfAir(world, o.add(p[0], 9, p[1]), Blocks.COMPOSTER.getDefaultState());
            placeIfAir(world, o.add(p[0], 10, p[1]),
                    (p[0] + p[1]) % 2 == 0
                            ? Blocks.POTTED_AZALEA_BUSH.getDefaultState()
                            : Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        }

        for (int[] p : new int[][]{{-8, -5}, {8, -5}, {-8, 6}, {8, 6}}) {
            hangLantern(world, o.add(p[0], 14, p[1]), 2);
        }
    }

    /** Copper screens and small service objects distinguish the water/control wing from the garden wing. */
    private static void detailEastWaterWing(ServerWorld world, BlockPos o) {
        for (int z : new int[]{-7, -3, 3, 7}) {
            for (int y = 3; y <= 6; y++) {
                placeIfAir(world, o.add(29, y, z), COPPER_SCREEN);
            }
        }
        for (int z : new int[]{-5, 0, 5}) {
            placeIfAir(world, o.add(28, 2, z), Blocks.COPPER_GRATE.getDefaultState());
            placeIfAir(world, o.add(28, 3, z), Blocks.LIGHTNING_ROD.getDefaultState());
        }
        placeIfAir(world, o.add(19, 2, 9), Blocks.BARREL.getDefaultState());
        placeIfAir(world, o.add(20, 2, 9), Blocks.CAULDRON.getDefaultState());
        placeIfAir(world, o.add(27, 2, 9), Blocks.BARREL.getDefaultState());
        placeIfAir(world, o.add(26, 2, 9), Blocks.COMPOSTER.getDefaultState());
    }

    /** Arena exterior gets deep eaves, corbels, gallery rails and a pronounced civic roof ridge. */
    private static void detailArenaInstitution(ServerWorld world, BlockPos o) {
        for (int x = -23; x <= 23; x++) {
            placeIfAir(world, o.add(x, 14, 12), facing(DARK_ROOF, Direction.SOUTH));
            placeIfAir(world, o.add(x, 14, 31), facing(DARK_ROOF, Direction.NORTH));
        }
        for (int x = -20; x <= 20; x += 4) {
            placeIfAir(world, o.add(x, 13, 12), BRACKET);
            placeIfAir(world, o.add(x, 13, 31), BRACKET);
            if (Math.floorMod(x, 8) == 4) {
                placeIfAir(world, o.add(x, 12, 12), LANTERN);
                placeIfAir(world, o.add(x, 12, 31), LANTERN);
            }
        }

        // Interior upper-gallery rails make spectator circulation believable without closing sightlines.
        for (int x = -19; x <= 19; x++) {
            if (Math.abs(x) > 5) {
                placeIfAir(world, o.add(x, 10, 16), RAIL);
            }
            placeIfAir(world, o.add(x, 10, 26), RAIL);
        }
        for (int z = 17; z <= 25; z++) {
            placeIfAir(world, o.add(-18, 10, z), RAIL);
            placeIfAir(world, o.add(18, 10, z), RAIL);
        }

        // The long central ridge gets small vertical punctuation similar to the reference roof spires.
        for (int z : new int[]{17, 22, 27}) {
            placeIfAir(world, o.add(0, 20, z), RIDGE);
            placeIfAir(world, o.add(0, 21, z), Blocks.LIGHTNING_ROD.getDefaultState());
        }

        // Leader end receives small functional props instead of another broad material fill.
        placeIfAir(world, o.add(-6, 3, 28), Blocks.LECTERN.getDefaultState());
        placeIfAir(world, o.add(6, 3, 28), Blocks.CHISELED_BOOKSHELF.getDefaultState());
        placeIfAir(world, o.add(-8, 3, 28), Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
        placeIfAir(world, o.add(8, 3, 28), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
    }

    /** Irregular masonry punctuation and planting soften the base without hiding circulation. */
    private static void weatherFoundations(ServerWorld world, BlockPos o) {
        int[][] stoneFeet = {
                {-15, -25}, {-15, -20}, {15, -25}, {15, -20},
                {-16, -10}, {-16, 8}, {16, -10}, {16, 8},
                {-23, 15}, {-23, 27}, {23, 15}, {23, 27}
        };
        for (int i = 0; i < stoneFeet.length; i++) {
            int x = stoneFeet[i][0];
            int z = stoneFeet[i][1];
            placeIfAir(world, o.add(x, 1, z), i % 3 == 0 ? MOSSY_WALL : STONE_WALL);
            if (i % 2 == 0) {
                placeIfAir(world, o.add(x + Integer.signum(x), 0, z), MOSS);
            }
        }

        for (int[] p : new int[][]{{-18, -24}, {18, -24}, {-18, 12}, {18, 12}, {-24, 30}, {24, 30}}) {
            placeIfAir(world, o.add(p[0], 1, p[1]), Blocks.AZALEA.getDefaultState());
        }
    }

    /** Back-of-house gets believable horticultural and maintenance clutter, kept off the main route. */
    private static void detailServiceYard(ServerWorld world, BlockPos o) {
        for (int y = 1; y <= 4; y++) {
            placeIfAir(world, o.add(-16, y, 32), Blocks.SCAFFOLDING.getDefaultState());
        }
        placeIfAir(world, o.add(-18, 1, 32), Blocks.COMPOSTER.getDefaultState());
        placeIfAir(world, o.add(-19, 1, 32), Blocks.BARREL.getDefaultState());
        placeIfAir(world, o.add(-20, 1, 32), Blocks.DECORATED_POT.getDefaultState());
        placeIfAir(world, o.add(16, 1, 32), Blocks.STONECUTTER.getDefaultState());
        placeIfAir(world, o.add(17, 1, 32), Blocks.CRAFTING_TABLE.getDefaultState());
        placeIfAir(world, o.add(18, 1, 32), Blocks.BARREL.getDefaultState());
        placeIfAir(world, o.add(19, 1, 32), Blocks.COMPOSTER.getDefaultState());

        hangLantern(world, o.add(-11, 6, 31), 2);
        hangLantern(world, o.add(11, 6, 31), 2);
    }

    private static BlockState facing(BlockState state, Direction direction) {
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.with(Properties.HORIZONTAL_FACING, direction);
        }
        return state;
    }

    private static BlockState openShutter(Direction direction) {
        return SHUTTER
                .with(Properties.OPEN, true)
                .with(Properties.HORIZONTAL_FACING, direction);
    }

    private static void hangLantern(ServerWorld world, BlockPos ceiling, int chainLength) {
        for (int i = 0; i < chainLength; i++) {
            placeIfAir(world, ceiling.down(i), CHAIN);
        }
        placeIfAir(world, ceiling.down(chainLength), LANTERN);
    }

    private static void columnIfAir(
            ServerWorld world,
            BlockPos origin,
            int x,
            int y,
            int z,
            int height,
            BlockState state
    ) {
        for (int i = 0; i < height; i++) {
            placeIfAir(world, origin.add(x, y + i, z), state);
        }
    }

    private static void placeIfAir(ServerWorld world, BlockPos pos, BlockState state) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, state);
        }
    }
}
