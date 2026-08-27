package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalace.*;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.Room;
import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Browser-review-derived visual quality checks for Grand Palace V4.
 *
 * Structural validity is not enough for a signature build. This audit protects the failures that
 * were only obvious once the exact low-memory mesh could be inspected: diagnostic material strips,
 * hash-selected accent bands, room-wide obstacle fields and a return to over-tall per-room roofs.
 */
final class OurosGrandPalaceV4VisualQualityAudit {
    private static final double MIN_CONNECTED_HEADROOM_RATIO = 0.50;

    private OurosGrandPalaceV4VisualQualityAudit() {}

    static Report assertValid(ServerWorld world, BlockPos o) {
        List<String> failures = new ArrayList<>();
        List<RoomMetric> rooms = new ArrayList<>();

        for (Room room : allRooms()) {
            requireNoDiagnosticSampler(failures, world, o, room);
            requireCuratedAccentRings(failures, world, o, room);
            double connectedHeadroom = connectedHeadroomRatio(world, o, room);
            if (connectedHeadroom + 1.0e-9 < MIN_CONNECTED_HEADROOM_RATIO) {
                failures.add(room.name() + " connected headroom="
                        + String.format("%.1f%%", connectedHeadroom * 100.0));
            }
            rooms.add(new RoomMetric(room.name(), connectedHeadroom));
        }

        requireLowSideRoofs(failures, world, o);
        requireCentralCupolas(failures, world, o);

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Grand Palace V4 visual quality audit failed: "
                    + String.join("; ", failures));
        }
        return new Report(List.copyOf(rooms));
    }

    private static List<Room> allRooms() {
        List<Room> rooms = new ArrayList<>(19);
        rooms.addAll(ceremonialRooms());
        rooms.addAll(groundSideRooms());
        rooms.addAll(upperSideRooms());
        return rooms;
    }

    /** The exact ten-block CI-era sampler may never survive into the authored room. */
    private static void requireNoDiagnosticSampler(List<String> failures, ServerWorld world,
                                                   BlockPos o, Room room) {
        BlockState[] sampler = {
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
        boolean exactMatch = true;
        for (int i = 0; i < sampler.length; i++) {
            BlockState actual = world.getBlockState(o.add(room.centerX() - 5 + i, y, z));
            if (!actual.equals(sampler[i])) {
                exactMatch = false;
                break;
            }
        }
        if (exactMatch) failures.add(room.name() + " still contains diagnostic material sampler");
    }

    /**
     * Hash-selected glazed/gold/amethyst accents were the strongest remaining test-fixture cue.
     * The curated V4 grammar uses copper or oxidized copper on these two architectural rings.
     */
    private static void requireCuratedAccentRings(List<String> failures, ServerWorld world,
                                                  BlockPos o, Room room) {
        scanRingForDiagnosticAccent(failures, world, o, room,
                room.minX() + 1, room.maxX() - 1, room.minZ() + 1, room.maxZ() - 1,
                room.floorY() + 5, "lower cornice");
        scanRingForDiagnosticAccent(failures, world, o, room,
                room.minX() + 2, room.maxX() - 2, room.minZ() + 2, room.maxZ() - 2,
                room.ceilingY(), "ceiling ring");
    }

    private static void scanRingForDiagnosticAccent(List<String> failures, ServerWorld world,
                                                    BlockPos o, Room room,
                                                    int x1, int x2, int z1, int z2, int y,
                                                    String label) {
        for (int x = x1; x <= x2; x++) {
            if (isForbiddenAccent(world.getBlockState(o.add(x, y, z1)))
                    || isForbiddenAccent(world.getBlockState(o.add(x, y, z2)))) {
                failures.add(room.name() + " has diagnostic accent on " + label);
                return;
            }
        }
        for (int z = z1; z <= z2; z++) {
            if (isForbiddenAccent(world.getBlockState(o.add(x1, y, z)))
                    || isForbiddenAccent(world.getBlockState(o.add(x2, y, z)))) {
                failures.add(room.name() + " has diagnostic accent on " + label);
                return;
            }
        }
    }

    private static boolean isForbiddenAccent(BlockState state) {
        return state.isOf(Blocks.BLUE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.GREEN_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.RED_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.PURPLE_GLAZED_TERRACOTTA)
                || state.isOf(Blocks.CHISELED_QUARTZ_BLOCK)
                || state.isOf(Blocks.AMETHYST_BLOCK)
                || state.isOf(Blocks.GOLD_BLOCK);
    }

    /**
     * Measure a connected two-block-high circulation field rather than merely counting furniture.
     * y=floor+2 corresponds to torso/head space above carpet and low floor treatment. The largest
     * four-neighbor air component must cover at least half of the usable room plan.
     */
    private static double connectedHeadroomRatio(ServerWorld world, BlockPos o, Room room) {
        int x1 = room.minX() + 2;
        int x2 = room.maxX() - 2;
        int z1 = room.minZ() + 2;
        int z2 = room.maxZ() - 2;
        int y = room.floorY() + 2;
        int total = Math.max(1, (x2 - x1 + 1) * (z2 - z1 + 1));

        Set<Node> open = new HashSet<>();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (world.getBlockState(o.add(x, y, z)).isAir()) open.add(new Node(x, z));
            }
        }

        int largest = 0;
        ArrayDeque<Node> queue = new ArrayDeque<>();
        while (!open.isEmpty()) {
            Node first = open.iterator().next();
            open.remove(first);
            queue.add(first);
            int count = 0;
            while (!queue.isEmpty()) {
                Node node = queue.removeFirst();
                count++;
                for (Node next : new Node[]{
                        new Node(node.x() + 1, node.z()), new Node(node.x() - 1, node.z()),
                        new Node(node.x(), node.z() + 1), new Node(node.x(), node.z() - 1)}) {
                    if (next.x() < x1 || next.x() > x2 || next.z() < z1 || next.z() > z2) continue;
                    if (open.remove(next)) queue.addLast(next);
                }
            }
            largest = Math.max(largest, count);
        }
        return largest / (double) total;
    }

    /** Side-room roof cores may not climb back into the old y=40+ mountain profile. */
    private static void requireLowSideRoofs(List<String> failures, ServerWorld world, BlockPos o) {
        for (Room room : groundSideRooms()) {
            for (int x = room.minX() + 3; x <= room.maxX() - 3; x++) {
                for (int z = room.minZ() + 3; z <= room.maxZ() - 3; z++) {
                    for (int y = 36; y <= 48; y++) {
                        if (!world.getBlockState(o.add(x, y, z)).isAir()) {
                            failures.add(room.name() + " roof core exceeds authored low-mansard height at y=" + y);
                            x = room.maxX();
                            z = room.maxZ();
                            y = 49;
                        }
                    }
                }
            }
        }
    }

    /** The central audience sequence, not every pavilion, owns the high roof markers. */
    private static void requireCentralCupolas(List<String> failures, ServerWorld world, BlockPos o) {
        if (world.getBlockState(o.add(AUDIENCE_CHAMBER.centerX(), 43, AUDIENCE_CHAMBER.centerZ())).isAir()) {
            failures.add("Audience Chamber cupola crown missing");
        }
        if (world.getBlockState(o.add(THEMIS_HALL.centerX(), 44, THEMIS_HALL.centerZ())).isAir()) {
            failures.add("Themis Hall cupola crown missing");
        }
    }

    private record Node(int x, int z) {}
    record RoomMetric(String name, double connectedHeadroomRatio) {}
    record Report(List<RoomMetric> rooms) {}
}
