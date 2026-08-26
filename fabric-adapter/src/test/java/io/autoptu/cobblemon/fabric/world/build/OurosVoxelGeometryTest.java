package io.autoptu.cobblemon.fabric.world.build;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OurosVoxelGeometryTest {
    @Test
    void filledEllipseIsSymmetricAndBounded() {
        Set<OurosVoxelGeometry.Voxel> ellipse = OurosVoxelGeometry.filledEllipse(6, 3, 2);

        assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(0, 2, 0)));
        assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(6, 2, 0)));
        assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(-6, 2, 0)));
        assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(0, 2, 3)));
        assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(0, 2, -3)));
        assertFalse(ellipse.contains(new OurosVoxelGeometry.Voxel(6, 2, 3)));

        for (OurosVoxelGeometry.Voxel voxel : ellipse) {
            assertTrue(Math.abs(voxel.x()) <= 6);
            assertTrue(Math.abs(voxel.z()) <= 3);
            assertEquals(2, voxel.y());
            assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(-voxel.x(), voxel.y(), voxel.z())));
            assertTrue(ellipse.contains(new OurosVoxelGeometry.Voxel(voxel.x(), voxel.y(), -voxel.z())));
        }
    }

    @Test
    void ellipseRingLeavesTheCenterOpen() {
        Set<OurosVoxelGeometry.Voxel> ring = OurosVoxelGeometry.ellipseRing(8, 5, 6, 3, 0);

        assertFalse(ring.contains(new OurosVoxelGeometry.Voxel(0, 0, 0)));
        assertTrue(ring.contains(new OurosVoxelGeometry.Voxel(8, 0, 0)));
        assertTrue(ring.contains(new OurosVoxelGeometry.Voxel(0, 0, 5)));
    }

    @Test
    void ellipsoidHasRealVerticalMass() {
        Set<OurosVoxelGeometry.Voxel> ellipsoid = OurosVoxelGeometry.filledEllipsoid(4, 2, 3);

        assertTrue(ellipsoid.contains(new OurosVoxelGeometry.Voxel(0, 2, 0)));
        assertTrue(ellipsoid.contains(new OurosVoxelGeometry.Voxel(0, -2, 0)));
        assertTrue(ellipsoid.contains(new OurosVoxelGeometry.Voxel(4, 0, 0)));
        assertFalse(ellipsoid.contains(new OurosVoxelGeometry.Voxel(4, 2, 3)));
    }

    @Test
    void taperedColumnNarrowsTowardTheTop() {
        Set<OurosVoxelGeometry.Voxel> column = OurosVoxelGeometry.taperedColumn(3, 3, 1, 1, 7);

        long bottom = column.stream().filter(v -> v.y() == 0).count();
        long top = column.stream().filter(v -> v.y() == 6).count();
        assertTrue(bottom > top);
        assertTrue(column.contains(new OurosVoxelGeometry.Voxel(0, 6, 0)));
    }

    @Test
    void line3dIncludesBothEndpointsAndIsDeterministic() {
        OurosVoxelGeometry.Voxel start = new OurosVoxelGeometry.Voxel(-2, 1, 4);
        OurosVoxelGeometry.Voxel end = new OurosVoxelGeometry.Voxel(5, 6, -1);

        Set<OurosVoxelGeometry.Voxel> first = OurosVoxelGeometry.line3d(start, end);
        Set<OurosVoxelGeometry.Voxel> second = OurosVoxelGeometry.line3d(start, end);

        assertEquals(first, second);
        assertTrue(first.contains(start));
        assertTrue(first.contains(end));
    }

    @Test
    void parabolicArchHasLowFeetAndHighCenter() {
        Set<OurosVoxelGeometry.Voxel> arch = OurosVoxelGeometry.parabolicArchX(10, 6, 1, 3);

        assertTrue(arch.contains(new OurosVoxelGeometry.Voxel(-10, 0, 3)));
        assertTrue(arch.contains(new OurosVoxelGeometry.Voxel(10, 0, 3)));
        assertTrue(arch.contains(new OurosVoxelGeometry.Voxel(0, 6, 3)));
    }

    @Test
    void invalidRadiiFailFast() {
        assertThrows(IllegalArgumentException.class, () -> OurosVoxelGeometry.filledEllipse(0, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> OurosVoxelGeometry.ellipseRing(4, 4, 4, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> OurosVoxelGeometry.filledEllipsoid(2, 0, 2));
    }
}
