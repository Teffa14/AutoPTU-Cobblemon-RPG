package io.autoptu.cobblemon.fabric.world;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceV4Plan.*;

/**
 * Grounds only genuinely unsupported authored room components.
 *
 * A component is already structurally anchored when it touches any part of the room shell: floor,
 * ceiling or perimeter wall. The previous implementation only recognized the floor and therefore
 * added full-height dark-oak fence posts beneath correctly ceiling-mounted chandeliers, wall
 * candelabra and hanging decoration. That passed connectivity while visibly turning rooms into
 * scaffolding. This pass keeps the structural invariant without manufacturing fake supports.
 */
final class OurosGrandPalaceV4RoomAnchoringPass {
    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private OurosGrandPalaceV4RoomAnchoringPass() {}

    static void apply(ServerWorld world, BlockPos o) {
        for (OurosGrandPalaceBuildKit.Room room : ceremonialRooms()) anchorRoom(world, o, room);
        for (OurosGrandPalaceBuildKit.Room room : groundSideRooms()) anchorRoom(world, o, room);
        for (OurosGrandPalaceBuildKit.Room room : upperSideRooms()) anchorRoom(world, o, room);
    }

    private static void anchorRoom(ServerWorld world, BlockPos o, OurosGrandPalaceBuildKit.Room room) {
        Set<Node> visited = new HashSet<>();
        for (int x = room.minX(); x <= room.maxX(); x++) {
            for (int y = room.floorY(); y <= room.ceilingY(); y++) {
                for (int z = room.minZ(); z <= room.maxZ(); z++) {
                    Node start = new Node(x, y, z);
                    if (visited.contains(start) || isAir(world, o, start)) continue;
                    Component component = collect(world, o, room, start, visited);
                    if (!component.touchesShell() && component.lowest().y() > room.floorY() + 1) {
                        addSupport(world, o, room.floorY(), component.lowest());
                    }
                }
            }
        }
    }

    private static Component collect(ServerWorld world, BlockPos o, OurosGrandPalaceBuildKit.Room room,
                                     Node start, Set<Node> visited) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        Node lowest = start;
        boolean touchesShell = touchesShell(room, start);

        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (current.y() < lowest.y()) lowest = current;
            if (touchesShell(room, current)) touchesShell = true;
            for (int[] d : NEIGHBORS) {
                Node next = new Node(current.x() + d[0], current.y() + d[1], current.z() + d[2]);
                if (!inside(room, next) || visited.contains(next) || isAir(world, o, next)) continue;
                visited.add(next);
                queue.addLast(next);
            }
        }
        return new Component(lowest, touchesShell);
    }

    private static boolean touchesShell(OurosGrandPalaceBuildKit.Room room, Node node) {
        return node.y() == room.floorY()
                || node.y() == room.ceilingY()
                || node.x() == room.minX()
                || node.x() == room.maxX()
                || node.z() == room.minZ()
                || node.z() == room.maxZ();
    }

    private static boolean inside(OurosGrandPalaceBuildKit.Room room, Node node) {
        return node.x() >= room.minX() && node.x() <= room.maxX()
                && node.y() >= room.floorY() && node.y() <= room.ceilingY()
                && node.z() >= room.minZ() && node.z() <= room.maxZ();
    }

    private static boolean isAir(ServerWorld world, BlockPos o, Node node) {
        return world.getBlockState(o.add(node.x(), node.y(), node.z())).isAir();
    }

    private static void addSupport(ServerWorld world, BlockPos o, int floorY, Node target) {
        for (int y = floorY + 1; y < target.y(); y++) {
            BlockPos pos = o.add(target.x(), y, target.z());
            if (world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
    }

    private record Node(int x, int y, int z) {}
    private record Component(Node lowest, boolean touchesShell) {}
}
