package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Silhouette/facade correction pass driven by exact browser review.
 *
 * The previous detail pass improved material variety but the south approach still read as stacked
 * rectangles with a copper picture-frame entry. This pass changes the actual architectural read:
 * a recessed three-bay gate, masonry piers, a stepped timber/deepslate gable, secondary window
 * canopies, wing corner pavilions and roof shoulders that tie isolated rooftop beams back into the
 * building.
 *
 * These blocks are world presentation only. Their Minecraft shape never creates PTU effects.
 */
public final class MeridianCanopyGymSilhouettePass {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState STONE = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState TUFF = Blocks.TUFF_BRICKS.getDefaultState();
    private static final BlockState FRAME = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState WARM_FRAME = Blocks.STRIPPED_OAK_LOG.getDefaultState();
    private static final BlockState PLANK = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState DARK_SLAB = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
    private static final BlockState WOOD_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState STONE_SLAB = Blocks.STONE_BRICK_SLAB.getDefaultState();
    private static final BlockState RAIL = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS_PANE.getDefaultState();
    private static final BlockState COPPER = Blocks.OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_GRATE = Blocks.OXIDIZED_COPPER_GRATE.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private MeridianCanopyGymSilhouettePass() {}

    public static void apply(ServerWorld world, BlockPos origin) {
        replaceLegacyEntryFrame(world, origin);
        buildRecessedArrivalGate(world, origin);
        buildSouthGableCanopy(world, origin);
        articulateLobbyFacade(world, origin);
        tieChallengeWingRoofs(world, origin);
        addArenaRoofShoulders(world, origin);
        addArchitecturalPlanting(world, origin);
    }

    /** Remove only the old copper/glass picture-frame components on the south face. */
    private static void replaceLegacyEntryFrame(ServerWorld world, BlockPos o) {
        for (int x = -7; x <= 7; x++) {
            for (int y = 1; y <= 6; y++) {
                BlockPos pos = o.add(x, y, -27);
                BlockState state = world.getBlockState(pos);
                if (state.isOf(Blocks.COPPER_BLOCK)
                        || state.isOf(Blocks.EXPOSED_COPPER)
                        || state.isOf(Blocks.WEATHERED_COPPER)
                        || state.isOf(Blocks.OXIDIZED_COPPER)
                        || state.isOf(Blocks.GLASS)
                        || state.isOf(Blocks.GLASS_PANE)) {
                    world.setBlockState(pos, AIR);
                }
            }
        }
    }

    /**
     * The entry becomes a deep, three-bay institutional gate. The central bay remains fully open;
     * side bays become sheltered waiting/display niches rather than another flat wall stripe.
     */
    private static void buildRecessedArrivalGate(ServerWorld world, BlockPos o) {
        // Broad stepped stone feet.
        for (int x : new int[]{-12, -7, 7, 12}) {
            fill(world, o, x - 1, 1, -30, x + 1, 2, -27, DEEPSLATE);
            fill(world, o, x, 3, -29, x, 7, -27, FRAME);
            world.setBlockState(o.add(x, 8, -28), STONE_SLAB);
        }

        // Warm inner posts make the doorway legible against the darker structural frame.
        for (int x : new int[]{-4, 4}) {
            column(world, o, x, 1, -28, 7, WARM_FRAME);
            world.setBlockState(o.add(x, 8, -28), Blocks.CHISELED_STONE_BRICKS.getDefaultState());
        }

        // Deep lintels create shadow and depth before the lobby wall.
        fill(world, o, -13, 8, -30, 13, 8, -28, PLANK);
        fill(world, o, -10, 9, -29, 10, 9, -27, TUFF);
        fill(world, o, -5, 10, -28, 5, 10, -27, COPPER);

        // Side niches: bench, planter, narrow glazing and hanging light.
        for (int side : new int[]{-1, 1}) {
            int minX = side < 0 ? -11 : 7;
            int maxX = side < 0 ? -7 : 11;
            int paneX = side < 0 ? -10 : 10;
            fill(world, o, minX, 3, -27, maxX, 6, -27, STONE);
            world.setBlockState(o.add(paneX, 4, -28), GLASS);
            world.setBlockState(o.add(paneX, 5, -28), GLASS);
            fill(world, o, minX + 1, 2, -29, maxX - 1, 2, -28, WOOD_SLAB);
            world.setBlockState(o.add(paneX, 3, -29),
                    side < 0 ? Blocks.POTTED_AZALEA_BUSH.getDefaultState()
                            : Blocks.POTTED_FLOWERING_AZALEA_BUSH.getDefaultState());
            world.setBlockState(o.add(paneX, 7, -29), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(paneX, 6, -29), LANTERN);
        }

        // Low masonry side returns stop the portico from looking like freestanding posts.
        fill(world, o, -15, 1, -28, -13, 4, -26, MOSSY);
        fill(world, o, 13, 1, -28, 15, 4, -26, STONE);
        world.setBlockState(o.add(-14, 5, -27), Blocks.STONE_BRICK_WALL.getDefaultState());
        world.setBlockState(o.add(14, 5, -27), Blocks.STONE_BRICK_WALL.getDefaultState());
    }

    /** A real stepped gable replaces the broad flat horizontal roof read above the entrance. */
    private static void buildSouthGableCanopy(ServerWorld world, BlockPos o) {
        BlockState westSlope = stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.EAST);
        BlockState eastSlope = stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.WEST);

        // Five roof steps rise toward a narrow ridge. Each step has enough depth to cast shadow.
        for (int step = 0; step <= 5; step++) {
            int y = 10 + step;
            int left = -15 + step * 3;
            int right = 15 - step * 3;
            if (left > right) {
                break;
            }
            for (int z = -31; z <= -26; z++) {
                world.setBlockState(o.add(left, y, z), westSlope);
                world.setBlockState(o.add(right, y, z), eastSlope);
                if (step < 4 && left + 1 < right) {
                    world.setBlockState(o.add(left + 1, y, z), DARK_SLAB);
                    world.setBlockState(o.add(right - 1, y, z), DARK_SLAB);
                }
            }
        }

        // Thin timber ridge and copper punctuation, deliberately much smaller than the old frame.
        for (int z = -31; z <= -26; z++) {
            world.setBlockState(o.add(0, 16, z), FRAME);
        }
        for (int z : new int[]{-30, -27}) {
            world.setBlockState(o.add(0, 17, z), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
            world.setBlockState(o.add(0, 18, z), Blocks.LIGHTNING_ROD.getDefaultState());
        }

        // Brackets under the eave prevent the roof from floating visually.
        for (int x = -14; x <= 14; x += 4) {
            world.setBlockState(o.add(x, 9, -31), RAIL);
            if (Math.floorMod(x, 8) == 2) {
                world.setBlockState(o.add(x, 8, -31), LANTERN);
            }
        }
    }

    /** Break the long lobby wall into bays with window depth, sill/cap pieces and small awnings. */
    private static void articulateLobbyFacade(ServerWorld world, BlockPos o) {
        BlockState southStair = stair(Blocks.DARK_OAK_STAIRS.getDefaultState(), Direction.SOUTH);
        for (int x : new int[]{-13, -9, 9, 13}) {
            fill(world, o, x - 1, 2, -27, x + 1, 6, -27, TUFF);
            world.setBlockState(o.add(x, 3, -28), GLASS);
            world.setBlockState(o.add(x, 4, -28), GLASS);
            world.setBlockState(o.add(x, 5, -28), GLASS);
            fill(world, o, x - 1, 2, -28, x + 1, 2, -28, STONE_SLAB);
            fill(world, o, x - 1, 7, -28, x + 1, 7, -28, FRAME);
            for (int dx = -2; dx <= 2; dx++) {
                world.setBlockState(o.add(x + dx, 8, -29), southStair);
            }
            world.setBlockState(o.add(x - 2, 6, -28), RAIL);
            world.setBlockState(o.add(x + 2, 6, -28), RAIL);
        }

        // Small planted ledges interrupt the huge stone base without becoming random hedge strips.
        for (int x : new int[]{-17, -11, 11, 17}) {
            world.setBlockState(o.add(x, 1, -26), Blocks.MOSS_BLOCK.getDefaultState());
            world.setBlockState(o.add(x, 2, -26),
                    x % 2 == 0 ? Blocks.AZALEA.getDefaultState() : Blocks.FLOWERING_AZALEA.getDefaultState());
        }
    }

    /**
     * The screenshot exposed isolated rooftop beams on both challenge wings. Add edge structure,
     * cross members and corner pavilions so they read as intentional roof gardens/pergolas.
     */
    private static void tieChallengeWingRoofs(ServerWorld world, BlockPos o) {
        // West garden roof: perimeter deck/eave and a tiny corner shelter.
        for (int z = -10; z <= 12; z++) {
            world.setBlockState(o.add(-32, 11, z), DARK_SLAB);
            world.setBlockState(o.add(-17, 11, z), DARK_SLAB);
        }
        for (int z : new int[]{-8, 0, 8}) {
            columnIfAir(world, o, -30, 12, z, 4, FRAME);
            columnIfAir(world, o, -19, 12, z, 4, FRAME);
            fillIfAir(world, o, -30, 15, z, -19, 15, z, FRAME);
        }
        fillIfAir(world, o, -31, 16, -10, -24, 16, -7, DARK_SLAB);
        fillIfAir(world, o, -30, 17, -9, -25, 17, -8, WOOD_SLAB);
        world.setBlockState(o.add(-28, 18, -8), Blocks.LANTERN.getDefaultState());

        // East water roof: actual pergola frame around the formerly disconnected black rails.
        for (int z = -10; z <= 12; z++) {
            world.setBlockState(o.add(17, 11, z), DARK_SLAB);
            world.setBlockState(o.add(32, 11, z), DARK_SLAB);
        }
        for (int z : new int[]{-8, -1, 6, 11}) {
            columnIfAir(world, o, 19, 12, z, 4, FRAME);
            columnIfAir(world, o, 30, 12, z, 4, FRAME);
            fillIfAir(world, o, 19, 15, z, 30, 15, z, FRAME);
            world.setBlockState(o.add(24, 14, z), Blocks.CHAIN.getDefaultState());
            world.setBlockState(o.add(24, 13, z), LANTERN);
        }
        for (int x : new int[]{20, 24, 28}) {
            fillIfAir(world, o, x, 15, -8, x, 15, 11, FRAME);
        }

        // Copper screens and planters make the roof usable, not only structural.
        for (int z : new int[]{-6, 2, 9}) {
            world.setBlockState(o.add(20, 12, z), COPPER_GRATE);
            world.setBlockState(o.add(29, 12, z), COPPER_GRATE);
            world.setBlockState(o.add(22, 12, z), Blocks.COMPOSTER.getDefaultState());
            world.setBlockState(o.add(27, 12, z), Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        }
    }

    /** Add dark roof shoulders around the arena clerestory so the rear volume stops reading as rafters on a box. */
    private static void addArenaRoofShoulders(ServerWorld world, BlockPos o) {
        BlockState south = stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.SOUTH);
        BlockState north = stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.NORTH);

        for (int x = -22; x <= 22; x++) {
            world.setBlockState(o.add(x, 14, 12), south);
            world.setBlockState(o.add(x, 14, 31), north);
            if (Math.abs(x) <= 19) {
                world.setBlockState(o.add(x, 15, 13), DARK_SLAB);
                world.setBlockState(o.add(x, 15, 30), DARK_SLAB);
            }
        }

        // Two shallow gable markers at the spectator corners break the 45-block roof span.
        for (int centerX : new int[]{-16, 16}) {
            for (int step = 0; step < 4; step++) {
                int y = 15 + step;
                int radius = 5 - step;
                world.setBlockState(o.add(centerX - radius, y, 13),
                        stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.EAST));
                world.setBlockState(o.add(centerX + radius, y, 13),
                        stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.WEST));
                world.setBlockState(o.add(centerX - radius, y, 30),
                        stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.EAST));
                world.setBlockState(o.add(centerX + radius, y, 30),
                        stair(Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Direction.WEST));
            }
            world.setBlockState(o.add(centerX, 19, 13), FRAME);
            world.setBlockState(o.add(centerX, 19, 30), FRAME);
        }
    }

    /** Purposeful planters at structural transitions soften edges without carpeting the whole build in leaves. */
    private static void addArchitecturalPlanting(ServerWorld world, BlockPos o) {
        int[][] positions = {
                {-15, -29}, {15, -29}, {-16, -15}, {16, -15},
                {-31, -11}, {-31, 13}, {31, -11}, {31, 13},
                {-23, 14}, {23, 14}, {-23, 29}, {23, 29}
        };
        for (int i = 0; i < positions.length; i++) {
            int x = positions[i][0];
            int z = positions[i][1];
            placeIfAir(world, o.add(x, 1, z), Blocks.MOSS_BLOCK.getDefaultState());
            placeIfAir(world, o.add(x, 2, z),
                    i % 3 == 0 ? Blocks.FLOWERING_AZALEA.getDefaultState() : Blocks.AZALEA.getDefaultState());
        }
    }

    private static BlockState stair(BlockState state, Direction direction) {
        return state.with(Properties.HORIZONTAL_FACING, direction);
    }

    private static void placeIfAir(ServerWorld world, BlockPos pos, BlockState state) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, state);
        }
    }

    private static void columnIfAir(
            ServerWorld world,
            BlockPos o,
            int x,
            int y,
            int z,
            int height,
            BlockState state
    ) {
        for (int i = 0; i < height; i++) {
            placeIfAir(world, o.add(x, y + i, z), state);
        }
    }

    private static void fillIfAir(
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
                    placeIfAir(world, o.add(x, y, z), state);
                }
            }
        }
    }

    private static void column(
            ServerWorld world,
            BlockPos o,
            int x,
            int y,
            int z,
            int height,
            BlockState state
    ) {
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
