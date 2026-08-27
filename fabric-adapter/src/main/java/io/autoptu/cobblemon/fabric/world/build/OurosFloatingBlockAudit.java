package io.autoptu.cobblemon.fabric.world.build;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detects authored block components that have no structural path to the ground band.
 *
 * Connectivity includes face and edge neighbors. Edge connectivity is intentional because stepped
 * Minecraft roofs often touch through stair/slab diagonals in section. Pure corner-only contact is
 * rejected. Fluids, foliage and decoration are included: if an authored object floats as its own
 * disconnected component, the exact build-review export should fail instead of hiding it.
 */
public final class OurosFloatingBlockAudit {
    private OurosFloatingBlockAudit() {}

    public record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        public Bounds {
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("invalid audit bounds");
            }
        }
    }

    public record FloatingComponent(
            int blockCount,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {}

    public record Report(int componentCount, int anchoredComponentCount, List<FloatingComponent> floatingComponents) {
        public Report {
            floatingComponents = Collections.unmodifiableList(new ArrayList<>(floatingComponents));
        }

        public boolean passed() {
            return floatingComponents.isEmpty();
        }

        public int floatingBlockCount() {
            return floatingComponents.stream().mapToInt(FloatingComponent::blockCount).sum();
        }
    }

    public static Report scan(ServerWorld world, BlockPos origin, Bounds bounds, int anchorMaxY) {
        if (world == null || origin == null || bounds == null) {
            throw new IllegalArgumentException("world, origin and bounds are required");
        }

        int sizeX = bounds.maxX() - bounds.minX() + 1;
        int sizeY = bounds.maxY() - bounds.minY() + 1;
        int sizeZ = bounds.maxZ() - bounds.minZ() + 1;
        boolean[] occupied = new boolean[sizeX * sizeY * sizeZ];
        boolean[] visited = new boolean[occupied.length];

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    int index = index(x, y, z, bounds, sizeY, sizeZ);
                    occupied[index] = !world.getBlockState(origin.add(x, y, z)).isAir();
                }
            }
        }

        int components = 0;
        int anchored = 0;
        List<FloatingComponent> floating = new ArrayList<>();
        ArrayDeque<LocalPos> queue = new ArrayDeque<>();

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    int startIndex = index(x, y, z, bounds, sizeY, sizeZ);
                    if (!occupied[startIndex] || visited[startIndex]) {
                        continue;
                    }

                    components++;
                    visited[startIndex] = true;
                    queue.addLast(new LocalPos(x, y, z));
                    int count = 0;
                    boolean componentAnchored = false;
                    int minX = x;
                    int maxX = x;
                    int minY = y;
                    int maxY = y;
                    int minZ = z;
                    int maxZ = z;

                    while (!queue.isEmpty()) {
                        LocalPos current = queue.removeFirst();
                        count++;
                        minX = Math.min(minX, current.x());
                        maxX = Math.max(maxX, current.x());
                        minY = Math.min(minY, current.y());
                        maxY = Math.max(maxY, current.y());
                        minZ = Math.min(minZ, current.z());
                        maxZ = Math.max(maxZ, current.z());
                        if (current.y() <= anchorMaxY) {
                            componentAnchored = true;
                        }

                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                                    if (manhattan == 0 || manhattan > 2) {
                                        continue;
                                    }
                                    int nx = current.x() + dx;
                                    int ny = current.y() + dy;
                                    int nz = current.z() + dz;
                                    if (nx < bounds.minX() || nx > bounds.maxX()
                                            || ny < bounds.minY() || ny > bounds.maxY()
                                            || nz < bounds.minZ() || nz > bounds.maxZ()) {
                                        continue;
                                    }
                                    int neighborIndex = index(nx, ny, nz, bounds, sizeY, sizeZ);
                                    if (!occupied[neighborIndex] || visited[neighborIndex]) {
                                        continue;
                                    }
                                    visited[neighborIndex] = true;
                                    queue.addLast(new LocalPos(nx, ny, nz));
                                }
                            }
                        }
                    }

                    if (componentAnchored) {
                        anchored++;
                    } else {
                        floating.add(new FloatingComponent(count, minX, maxX, minY, maxY, minZ, maxZ));
                    }
                }
            }
        }

        return new Report(components, anchored, floating);
    }

    public static String describeFailures(Report report) {
        if (report.passed()) {
            return "no floating authored components";
        }
        StringBuilder description = new StringBuilder();
        description.append(report.floatingComponents().size())
                .append(" floating components / ")
                .append(report.floatingBlockCount())
                .append(" blocks");
        int limit = Math.min(32, report.floatingComponents().size());
        for (int i = 0; i < limit; i++) {
            FloatingComponent c = report.floatingComponents().get(i);
            description.append("; #").append(i + 1)
                    .append(" count=").append(c.blockCount())
                    .append(" bounds=[")
                    .append(c.minX()).append("..").append(c.maxX()).append(',')
                    .append(c.minY()).append("..").append(c.maxY()).append(',')
                    .append(c.minZ()).append("..").append(c.maxZ()).append(']');
        }
        return description.toString();
    }

    private static int index(int x, int y, int z, Bounds bounds, int sizeY, int sizeZ) {
        int localX = x - bounds.minX();
        int localY = y - bounds.minY();
        int localZ = z - bounds.minZ();
        return (localX * sizeY + localY) * sizeZ + localZ;
    }

    private record LocalPos(int x, int y, int z) {}
}
