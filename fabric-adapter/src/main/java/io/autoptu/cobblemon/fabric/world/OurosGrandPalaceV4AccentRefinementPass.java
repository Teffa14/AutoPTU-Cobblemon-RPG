package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.Room;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/** Removes the last hash-selected material bands from the authored V4 room grammar. */
final class OurosGrandPalaceV4AccentRefinementPass {
    private static final BlockState WARM_TRIM = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState COOL_TRIM = Blocks.WAXED_OXIDIZED_CUT_COPPER.getDefaultState();

    private OurosGrandPalaceV4AccentRefinementPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (Room room : ceremonialRooms()) curateRoom(world, o, room);
        for (Room room : groundSideRooms()) curateRoom(world, o, room);
        for (Room room : upperSideRooms()) curateRoom(world, o, room);
    }

    private static void curateRoom(ServerWorld world, BlockPos o, Room room) {
        BlockState trim = trimFor(room);

        // `layerInteriorWalls` used accentFor(room.name()) for this lower cornice. Keep the authored
        // position but remove the hash-driven color lottery visible as gold/red/purple horizontal
        // bands in browser review.
        replaceAccentRing(world, o, room, room.minX() + 1, room.maxX() - 1,
                room.minZ() + 1, room.maxZ() - 1, room.floorY() + 5, trim);

        // `insetCeiling` used the same random accent on its middle ring. Curate that exact ring too so
        // an overhead first-person view does not reveal the old diagnostic palette.
        replaceAccentRing(world, o, room, room.minX() + 2, room.maxX() - 2,
                room.minZ() + 2, room.maxZ() - 2, room.ceilingY(), trim);
    }

    private static void replaceAccentRing(ServerWorld world, BlockPos o, Room room,
                                          int x1, int x2, int z1, int z2, int y, BlockState trim) {
        for (int x = x1; x <= x2; x++) {
            replaceIfDiagnosticAccent(world, o.add(x, y, z1), trim);
            replaceIfDiagnosticAccent(world, o.add(x, y, z2), trim);
        }
        for (int z = z1; z <= z2; z++) {
            replaceIfDiagnosticAccent(world, o.add(x1, y, z), trim);
            replaceIfDiagnosticAccent(world, o.add(x2, y, z), trim);
        }
    }

    private static void replaceIfDiagnosticAccent(ServerWorld world, BlockPos pos, BlockState trim) {
        BlockState state = world.getBlockState(pos);
        if (isDiagnosticAccent(state)) world.setBlockState(pos, trim);
    }

    private static boolean isDiagnosticAccent(BlockState state) {
        return state.isOf(Blocks.WAXED_CUT_COPPER)
                || state.isOf(Blocks.BLUE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.GREEN_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.RED_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.PURPLE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.CHISELED_QUARTZ_BLOCK)
                || state.isOf(Blocks.AMETHYST_BLOCK)
                || state.isOf(Blocks.GOLD_BLOCK);
    }

    private static BlockState trimFor(Room room) {
        String name = room.name();
        if (name.equals(BLUE_SALON.name())
                || name.equals(GEOGRAPHY_CABINET.name())
                || name.equals(PORCELAIN_HALL.name())) {
            return COOL_TRIM;
        }
        return WARM_TRIM;
    }
}
