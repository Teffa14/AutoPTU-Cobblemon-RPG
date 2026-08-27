package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Browser-review-driven interior cleanup for Palace V4.
 *
 * The first authored room pass deliberately over-supplied material samples and generic furniture so
 * every one of the nineteen review spaces could prove density and palette coverage. Those fixtures
 * were useful as a gate, but they read as test geometry in the actual Palace: rainbow sample strips,
 * raised solid-color bars, cloned table pairs and oversized chandeliers. This pass preserves the
 * density contract while turning those diagnostics into architectural detail and room-specific use.
 */
final class OurosGrandPalaceV4InteriorRefinementPass {
    private static final BlockState LIGHT_WALL = Blocks.CALCITE.getDefaultState();
    private static final BlockState WHITE_WALL = Blocks.SMOOTH_QUARTZ.getDefaultState();
    private static final BlockState PILASTER = Blocks.QUARTZ_PILLAR.getDefaultState();
    private static final BlockState WARM_TRIM = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState COOL_TRIM = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();
    private static final BlockState DARK_WOOD = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState DARK_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState SPRUCE_SLAB = Blocks.SPRUCE_SLAB.getDefaultState();

    private OurosGrandPalaceV4InteriorRefinementPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (Room room : ceremonialRooms()) refineCommon(world, o, room);
        for (Room room : groundSideRooms()) refineCommon(world, o, room);
        for (Room room : upperSideRooms()) refineCommon(world, o, room);

        refineBloomingSalon(world, o, physical(BLOOMING_SALON));
        refineBlueSalon(world, o, physical(BLUE_SALON));
        refineAccountingOffice(world, o, physical(ACCOUNTING_OFFICE));
        refineRailingSalon(world, o, physical(RAILING_SALON));
        buildGalleryClerestory(world, o, physical(GALLERY_OF_ART));
    }

    private static void refineCommon(ServerWorld world, BlockPos o, Room room) {
        removeDiagnosticMaterialStrip(world, o, room);
        repaintGenericInteriorLayer(world, o, room);
        normalizeBaselineRunner(world, o, room);

        int drop = room.ceilingY() - room.floorY() > 20 ? 8 : 5;
        replaceChandelier(world, o, room.centerX(), room.ceilingY(), room.centerZ(), drop);
        if (room.name().equals(ANTECHAMBER.name())) {
            replaceChandelier(world, o, room.centerX(), room.ceilingY(), room.centerZ() - 7, 9);
            replaceChandelier(world, o, room.centerX(), room.ceilingY(), room.centerZ() + 7, 9);
        }
    }

    /** Remove only the exact ten diagnostic blocks that survived the room theme pass. */
    private static void removeDiagnosticMaterialStrip(ServerWorld world, BlockPos o, Room room) {
        BlockState[] expected = {
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
        int y = room.floorY() + 4;
        int z = room.minZ() + 2;
        for (int i = 0; i < expected.length; i++) {
            int x = room.centerX() - 5 + i;
            BlockPos pos = o.add(x, y, z);
            if (world.getBlockState(pos).equals(expected[i])) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
    }

    /**
     * Replace only the generic V4 wall grammar. Authored bookshelves, art, heraldry and furniture are
     * left alone, and air openings stay air. The exterior structural shell remains untouched.
     */
    private static void repaintGenericInteriorLayer(ServerWorld world, BlockPos o, Room room) {
        BlockState field = wallField(room);
        BlockState trim = trimFor(room);
        for (int y = room.floorY() + 1; y <= room.ceilingY() - 1; y++) {
            for (int z = room.minZ() + 1; z <= room.maxZ() - 1; z++) {
                repaintIfGeneric(world, o.add(room.minX() + 1, y, z), field, trim);
                repaintIfGeneric(world, o.add(room.maxX() - 1, y, z), field, trim);
            }
            for (int x = room.minX() + 1; x <= room.maxX() - 1; x++) {
                repaintIfGeneric(world, o.add(x, y, room.minZ() + 1), field, trim);
                repaintIfGeneric(world, o.add(x, y, room.maxZ() - 1), field, trim);
            }
        }
    }

    private static void repaintIfGeneric(ServerWorld world, BlockPos pos, BlockState field, BlockState trim) {
        BlockState current = world.getBlockState(pos);
        if (current.isOf(Blocks.TUFF_BRICKS)) world.setBlockState(pos, field);
        else if (current.isOf(Blocks.POLISHED_DIORITE)) world.setBlockState(pos, PILASTER);
        else if (current.isOf(Blocks.BAMBOO_MOSAIC)) world.setBlockState(pos, trim);
    }

    private static BlockState wallField(Room room) {
        String name = room.name();
        if (name.equals(BLUE_SALON.name()) || name.equals(GEOGRAPHY_CABINET.name())) {
            return Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState();
        }
        if (name.equals(MUSIC_CHAMBER.name())) return Blocks.PINK_TERRACOTTA.getDefaultState();
        if (name.equals(HUNTING_SALON.name()) || name.equals(LIBRARY.name()) || name.equals(GLOBE_BOOK_CABINET.name())) {
            return Blocks.WHITE_TERRACOTTA.getDefaultState();
        }
        if (name.equals(PORCELAIN_HALL.name()) || name.equals(GALLERY_OF_ART.name()) || name.equals(MARBLE_SALON.name())) {
            return WHITE_WALL;
        }
        return LIGHT_WALL;
    }

    private static BlockState trimFor(Room room) {
        String name = room.name();
        if (name.equals(BLUE_SALON.name()) || name.equals(GEOGRAPHY_CABINET.name()) || name.equals(PORCELAIN_HALL.name())) {
            return COOL_TRIM;
        }
        return WARM_TRIM;
    }

    /** Turn the one-block-high accent bar into a real carpet runner when the old baseline survived. */
    private static void normalizeBaselineRunner(ServerWorld world, BlockPos o, Room room) {
        BlockState carpet = carpetFor(room);
        int y = room.floorY() + 1;
        for (int x = room.centerX() - 4; x <= room.centerX() + 4; x++) {
            for (int z = room.centerZ() - 1; z <= room.centerZ() + 1; z++) {
                BlockPos pos = o.add(x, y, z);
                if (isBaselineAccent(world.getBlockState(pos))) world.setBlockState(pos, carpet);
            }
        }
    }

    private static boolean isBaselineAccent(BlockState state) {
        return state.isOf(Blocks.WAXED_CUT_COPPER)
                || state.isOf(Blocks.BLUE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.GREEN_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.RED_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.PURPLE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.CHISELED_QUARTZ_BLOCK)
                || state.isOf(Blocks.AMETHYST_BLOCK)
                || state.isOf(Blocks.GOLD_BLOCK);
    }

    private static BlockState carpetFor(Room room) {
        String name = room.name();
        if (name.equals(BLUE_SALON.name()) || name.equals(GEOGRAPHY_CABINET.name())) return Blocks.LIGHT_BLUE_CARPET.getDefaultState();
        if (name.equals(BLOOMING_SALON.name()) || name.equals(SALLA_TERRENA.name())) return Blocks.GREEN_CARPET.getDefaultState();
        if (name.equals(HUNTING_SALON.name())) return Blocks.BROWN_CARPET.getDefaultState();
        if (name.equals(BANQUET_HALL.name()) || name.equals(AUDIENCE_CHAMBER.name()) || name.equals(ANTECHAMBER.name())) return Blocks.RED_CARPET.getDefaultState();
        if (name.equals(MUSIC_CHAMBER.name())) return Blocks.PINK_CARPET.getDefaultState();
        return Blocks.WHITE_CARPET.getDefaultState();
    }

    /** Replace the five-block-wide copper cross with a compact ceiling-hung candle crown. */
    private static void replaceChandelier(ServerWorld world, BlockPos o, int x, int ceilingY, int z, int drop) {
        int oldHubY = ceilingY - drop;
        clear(world, o, x - 2, Math.max(1, oldHubY - 1), z - 2, x + 2, ceilingY - 1, z + 2);

        int hubY = ceilingY - 4;
        for (int y = hubY + 2; y < ceilingY; y++) {
            world.setBlockState(o.add(x, y, z), Blocks.CHAIN.getDefaultState());
        }
        world.setBlockState(o.add(x, hubY + 1, z), Blocks.SEA_LANTERN.getDefaultState());
        for (Direction direction : Direction.Type.HORIZONTAL) {
            int ax = x + direction.getOffsetX();
            int az = z + direction.getOffsetZ();
            world.setBlockState(o.add(ax, hubY + 1, az), Blocks.WAXED_CUT_COPPER_SLAB.getDefaultState());
            world.setBlockState(o.add(ax, hubY + 2, az), litCandle(Blocks.WHITE_CANDLE.getDefaultState()));
        }
        world.setBlockState(o.add(x, hubY, z), Blocks.CHAIN.getDefaultState());
    }

    /** A low conservatory composition replaces the six-block trellis poles visible in review. */
    private static void refineBloomingSalon(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        clear(world, o, room.minX() + 3, floor + 2, room.minZ() + 3,
                room.maxX() - 3, floor + 8, room.minZ() + 5);

        fill(world, o, room.minX() + 4, floor + 1, room.minZ() + 4,
                room.maxX() - 4, floor + 1, room.minZ() + 4, Blocks.MOSS_BLOCK.getDefaultState());
        fill(world, o, room.minX() + 4, floor + 1, room.minZ() + 3,
                room.maxX() - 4, floor + 1, room.minZ() + 3, SPRUCE_SLAB);
        for (int x = room.minX() + 5; x <= room.maxX() - 5; x += 4) {
            world.setBlockState(o.add(x, floor + 2, room.minZ() + 4), Blocks.POTTED_PINK_TULIP.getDefaultState());
        }
        for (int x : new int[]{room.centerX() - 5, room.centerX() + 5}) {
            fill(world, o, x, floor + 2, room.minZ() + 5, x, floor + 4, room.minZ() + 5,
                    Blocks.DARK_OAK_FENCE.getDefaultState());
            fill(world, o, x - 2, floor + 5, room.minZ() + 5, x + 2, floor + 5, room.minZ() + 5,
                    Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            world.setBlockState(o.add(x, floor + 2, room.minZ() + 5), Blocks.MOSS_BLOCK.getDefaultState());
        }
    }

    /** Blue Salon keeps the color story but uses low upholstered settees instead of wool barricades. */
    private static void refineBlueSalon(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        clear(world, o, room.centerX() - 8, floor + 1, room.centerZ() - 7,
                room.centerX() + 8, floor + 4, room.centerZ() + 7);

        fill(world, o, room.centerX() - 7, floor + 1, room.centerZ() - 5,
                room.centerX() + 7, floor + 1, room.centerZ() + 5, Blocks.BLUE_CARPET.getDefaultState());
        buildUpholsteredSettee(world, o, room.centerX() - 6, room.centerX() + 6,
                floor, room.centerZ() - 5, Blocks.LIGHT_BLUE_CARPET.getDefaultState());
        buildUpholsteredSettee(world, o, room.centerX() - 6, room.centerX() + 6,
                floor, room.centerZ() + 5, Blocks.BLUE_CARPET.getDefaultState());

        table(world, o, room.centerX() - 2, room.centerZ() - 1,
                room.centerX() + 2, room.centerZ() + 1, floor + 2,
                SPRUCE_SLAB, Blocks.DARK_OAK_FENCE.getDefaultState());
        chair(world, o, room.centerX() - 7, floor + 2, room.centerZ(), Direction.EAST,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
        chair(world, o, room.centerX() + 7, floor + 2, room.centerZ(), Direction.WEST,
                Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
    }

    private static void buildUpholsteredSettee(ServerWorld world, BlockPos o, int x1, int x2,
                                               int floor, int z, BlockState upholstery) {
        fill(world, o, x1, floor + 2, z, x2, floor + 2, z, DARK_SLAB);
        fill(world, o, x1, floor + 3, z, x2, floor + 3, z, upholstery);
        world.setBlockState(o.add(x1, floor + 1, z), Blocks.DARK_OAK_FENCE.getDefaultState());
        world.setBlockState(o.add(x2, floor + 1, z), Blocks.DARK_OAK_FENCE.getDefaultState());
    }

    /** Four paired writing desks and a central aisle replace the office's repeated full-room grid. */
    private static void refineAccountingOffice(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        clear(world, o, room.minX() + 3, floor + 1, room.minZ() + 3,
                room.maxX() - 3, floor + 4, room.maxZ() - 3);
        fill(world, o, room.centerX() - 1, floor + 1, room.minZ() + 3,
                room.centerX() + 1, floor + 1, room.maxZ() - 3, Blocks.RED_CARPET.getDefaultState());

        for (int z : new int[]{room.centerZ() - 5, room.centerZ() + 5}) {
            table(world, o, room.minX() + 4, z - 1, room.centerX() - 2, z + 1, floor + 2,
                    DARK_SLAB, Blocks.DARK_OAK_FENCE.getDefaultState());
            table(world, o, room.centerX() + 2, z - 1, room.maxX() - 4, z + 1, floor + 2,
                    SPRUCE_SLAB, Blocks.SPRUCE_FENCE.getDefaultState());
            chair(world, o, room.centerX() - 4, floor + 2, z + 2, Direction.NORTH,
                    Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState());
            chair(world, o, room.centerX() + 4, floor + 2, z + 2, Direction.NORTH,
                    Blocks.SPRUCE_STAIRS.getDefaultState(), Blocks.SPRUCE_FENCE.getDefaultState());
        }

        fill(world, o, room.centerX() - 5, floor + 2, room.maxZ() - 2,
                room.centerX() + 5, floor + 5, room.maxZ() - 2, Blocks.BOOKSHELF.getDefaultState());
        fill(world, o, room.centerX() - 5, floor + 6, room.maxZ() - 2,
                room.centerX() + 5, floor + 6, room.maxZ() - 2, WARM_TRIM);
    }

    /** Railing Salon becomes a display salon, not a room-wide obstacle grid. */
    private static void refineRailingSalon(ServerWorld world, BlockPos o, Room room) {
        int floor = room.floorY();
        clear(world, o, room.minX() + 3, floor + 1, room.minZ() + 3,
                room.maxX() - 3, floor + 4, room.maxZ() - 3);
        fill(world, o, room.centerX() - 5, floor + 1, room.centerZ() - 6,
                room.centerX() + 5, floor + 1, room.centerZ() + 6, Blocks.RED_CARPET.getDefaultState());

        for (int z : new int[]{room.centerZ() - 6, room.centerZ() + 6}) {
            fill(world, o, room.centerX() - 6, floor + 2, z,
                    room.centerX() + 6, floor + 2, z, DARK_SLAB);
            for (int x = room.centerX() - 5; x <= room.centerX() + 5; x += 2) {
                world.setBlockState(o.add(x, floor + 3, z), Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
        bench(world, o, room.centerX() - 4, room.centerZ(), room.centerX() + 4, room.centerZ(),
                floor + 2, SPRUCE_SLAB, Blocks.SPRUCE_FENCE.getDefaultState());
    }

    /**
     * Gallery of Art is the only ground pavilion without a second authored room above it. Instead of
     * leaving a grey intermediate deck under a y=30 roof, make that absence intentional: the gallery
     * opens through its old ceiling into a tall clerestory with an upper perimeter balcony.
     */
    private static void buildGalleryClerestory(ServerWorld world, BlockPos o, Room room) {
        int oldCeiling = room.ceilingY();
        clear(world, o, room.minX() + 3, oldCeiling, room.minZ() + 3,
                room.maxX() - 3, oldCeiling, room.maxZ() - 3);

        int y1 = oldCeiling + 1;
        int y2 = 29;
        for (int y = y1; y <= y2; y++) {
            BlockState field = y == y1 || y >= 27 ? PILASTER : WHITE_WALL;
            fill(world, o, room.minX(), y, room.minZ(), room.maxX(), y, room.minZ(), field);
            fill(world, o, room.minX(), y, room.maxZ(), room.maxX(), y, room.maxZ(), field);
            fill(world, o, room.minX(), y, room.minZ(), room.minX(), y, room.maxZ(), field);
            fill(world, o, room.maxX(), y, room.minZ(), room.maxX(), y, room.maxZ(), field);
        }

        // Upper balcony ring is tied to the existing y=15 exterior galleries.
        fill(world, o, room.minX() + 1, y1, room.minZ() + 1, room.maxX() - 1, y1, room.minZ() + 2, DARK_WOOD);
        fill(world, o, room.minX() + 1, y1, room.maxZ() - 2, room.maxX() - 1, y1, room.maxZ() - 1, DARK_WOOD);
        fill(world, o, room.minX() + 1, y1, room.minZ() + 1, room.minX() + 2, y1, room.maxZ() - 1, DARK_WOOD);
        fill(world, o, room.maxX() - 2, y1, room.minZ() + 1, room.maxX() - 1, y1, room.maxZ() - 1, DARK_WOOD);

        // Two tall window bays on both the garden and court elevations.
        for (int z : new int[]{room.centerZ() - 6, room.centerZ() + 6}) {
            fill(world, o, room.minX(), y1 + 3, z - 2, room.minX(), y1 + 10, z + 2,
                    Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
            fill(world, o, room.maxX(), y1 + 3, z - 2, room.maxX(), y1 + 10, z + 2,
                    Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
            fill(world, o, room.minX(), y1 + 2, z - 3, room.minX(), y1 + 11, z - 3, PILASTER);
            fill(world, o, room.minX(), y1 + 2, z + 3, room.minX(), y1 + 11, z + 3, PILASTER);
            fill(world, o, room.maxX(), y1 + 2, z - 3, room.maxX(), y1 + 11, z - 3, PILASTER);
            fill(world, o, room.maxX(), y1 + 2, z + 3, room.maxX(), y1 + 11, z + 3, PILASTER);
        }
        fill(world, o, room.minX(), 27, room.minZ(), room.maxX(), 29, room.minZ(), WARM_TRIM);
        fill(world, o, room.minX(), 27, room.maxZ(), room.maxX(), 29, room.maxZ(), WARM_TRIM);
    }
}
