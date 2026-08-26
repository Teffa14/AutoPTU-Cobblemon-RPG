package io.autoptu.cobblemon.fabric.world.build;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Deterministic project-owned voxel geometry for authored Ouros locations.
 *
 * These primitives create coordinates only. They do not assign PTU terrain effects or any battle
 * semantics. Keeping geometry pure makes complex shapes testable before they are projected into a
 * Minecraft world.
 */
public final class OurosVoxelGeometry {
    private OurosVoxelGeometry() {}

    public record Voxel(int x, int y, int z) {}

    public static Set<Voxel> filledEllipse(int radiusX, int radiusZ, int y) {
        requirePositive(radiusX, "radiusX");
        requirePositive(radiusZ, "radiusZ");
        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        double rx2 = (double) radiusX * radiusX;
        double rz2 = (double) radiusZ * radiusZ;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double normalized = x * x / rx2 + z * z / rz2;
                if (normalized <= 1.0D) {
                    voxels.add(new Voxel(x, y, z));
                }
            }
        }
        return immutable(voxels);
    }

    public static Set<Voxel> ellipseRing(
            int outerRadiusX,
            int outerRadiusZ,
            int innerRadiusX,
            int innerRadiusZ,
            int y
    ) {
        requirePositive(outerRadiusX, "outerRadiusX");
        requirePositive(outerRadiusZ, "outerRadiusZ");
        requireNonNegative(innerRadiusX, "innerRadiusX");
        requireNonNegative(innerRadiusZ, "innerRadiusZ");
        if (innerRadiusX >= outerRadiusX || innerRadiusZ >= outerRadiusZ) {
            throw new IllegalArgumentException("inner ellipse must be smaller than outer ellipse");
        }

        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>(filledEllipse(outerRadiusX, outerRadiusZ, y));
        if (innerRadiusX > 0 && innerRadiusZ > 0) {
            voxels.removeAll(filledEllipse(innerRadiusX, innerRadiusZ, y));
        }
        return immutable(voxels);
    }

    public static Set<Voxel> filledEllipsoid(int radiusX, int radiusY, int radiusZ) {
        requirePositive(radiusX, "radiusX");
        requirePositive(radiusY, "radiusY");
        requirePositive(radiusZ, "radiusZ");
        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        double rx2 = (double) radiusX * radiusX;
        double ry2 = (double) radiusY * radiusY;
        double rz2 = (double) radiusZ * radiusZ;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double normalized = x * x / rx2 + y * y / ry2 + z * z / rz2;
                    if (normalized <= 1.0D) {
                        voxels.add(new Voxel(x, y, z));
                    }
                }
            }
        }
        return immutable(voxels);
    }

    public static Set<Voxel> taperedColumn(
            int baseRadiusX,
            int baseRadiusZ,
            int topRadiusX,
            int topRadiusZ,
            int height
    ) {
        requirePositive(baseRadiusX, "baseRadiusX");
        requirePositive(baseRadiusZ, "baseRadiusZ");
        requirePositive(topRadiusX, "topRadiusX");
        requirePositive(topRadiusZ, "topRadiusZ");
        requirePositive(height, "height");

        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        for (int y = 0; y < height; y++) {
            double t = height == 1 ? 1.0D : (double) y / (height - 1);
            int radiusX = Math.max(1, (int) Math.round(lerp(baseRadiusX, topRadiusX, t)));
            int radiusZ = Math.max(1, (int) Math.round(lerp(baseRadiusZ, topRadiusZ, t)));
            voxels.addAll(filledEllipse(radiusX, radiusZ, y));
        }
        return immutable(voxels);
    }

    public static Set<Voxel> line3d(Voxel start, Voxel end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("line endpoints are required");
        }
        int dx = end.x() - start.x();
        int dy = end.y() - start.y();
        int dz = end.z() - start.z();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        if (steps == 0) {
            voxels.add(start);
            return immutable(voxels);
        }
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            voxels.add(new Voxel(
                    (int) Math.round(lerp(start.x(), end.x(), t)),
                    (int) Math.round(lerp(start.y(), end.y(), t)),
                    (int) Math.round(lerp(start.z(), end.z(), t))
            ));
        }
        return immutable(voxels);
    }

    public static Set<Voxel> parabolicArchX(int halfSpan, int rise, int thickness, int z) {
        requirePositive(halfSpan, "halfSpan");
        requirePositive(rise, "rise");
        requirePositive(thickness, "thickness");
        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        double span2 = (double) halfSpan * halfSpan;
        for (int x = -halfSpan; x <= halfSpan; x++) {
            int y = (int) Math.round(rise * (1.0D - x * x / span2));
            for (int t = 0; t < thickness; t++) {
                voxels.add(new Voxel(x, Math.max(0, y) + t, z));
            }
        }
        return immutable(voxels);
    }

    public static Set<Voxel> translate(Set<Voxel> source, int dx, int dy, int dz) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        LinkedHashSet<Voxel> voxels = new LinkedHashSet<>();
        for (Voxel voxel : source) {
            voxels.add(new Voxel(voxel.x() + dx, voxel.y() + dy, voxel.z() + dz));
        }
        return immutable(voxels);
    }

    private static Set<Voxel> immutable(LinkedHashSet<Voxel> voxels) {
        return Collections.unmodifiableSet(voxels);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
