package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHerdRegroupingPresentationRuntimeTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void marksOnlyObservedOffsetsOutsideAuthoredCohesion() {
        assertFalse(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, 6.0D));
        assertFalse(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(3.0D, 4.0D, 5.0D));
        assertTrue(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(3.1D, 4.0D, 5.0D));
        assertTrue(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(-8.0D, 0.0D, 6.0D));
    }

    @Test
    void cueCadenceEscalatesOnlyAfterFourObservedBlocksBeyondCohesion() {
        assertEquals(20, WildHerdRegroupingPresentationRuntime.cueIntervalTicks(5.0D, 5.0D));
        assertEquals(20, WildHerdRegroupingPresentationRuntime.cueIntervalTicks(8.999D, 5.0D));
        assertEquals(10, WildHerdRegroupingPresentationRuntime.cueIntervalTicks(9.0D, 5.0D));
        assertEquals(10, WildHerdRegroupingPresentationRuntime.cueIntervalTicks(500.0D, 5.0D));
    }

    @Test
    void cueDensityScalesWithObservedDistanceBeyondAuthoredCohesion() {
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueParticleCount(5.0D, 5.0D));
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueParticleCount(8.9D, 5.0D));
        assertEquals(3, WildHerdRegroupingPresentationRuntime.cueParticleCount(9.0D, 5.0D));
        assertEquals(4, WildHerdRegroupingPresentationRuntime.cueParticleCount(13.0D, 5.0D));
        assertEquals(6, WildHerdRegroupingPresentationRuntime.cueParticleCount(500.0D, 5.0D));
    }

    @Test
    void cueTrailLengthScalesWithObservedDistanceBeyondAuthoredCohesion() {
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueTrailPointCount(5.0D, 5.0D));
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueTrailPointCount(8.9D, 5.0D));
        assertEquals(3, WildHerdRegroupingPresentationRuntime.cueTrailPointCount(9.0D, 5.0D));
        assertEquals(4, WildHerdRegroupingPresentationRuntime.cueTrailPointCount(13.0D, 5.0D));
        assertEquals(5, WildHerdRegroupingPresentationRuntime.cueTrailPointCount(500.0D, 5.0D));
    }

    @Test
    void cueHeightScalesWithObservedDistanceBeyondAuthoredCohesion() {
        assertEquals(0.18D, WildHerdRegroupingPresentationRuntime.cueVerticalOffset(5.0D, 5.0D), EPSILON);
        assertEquals(0.20D, WildHerdRegroupingPresentationRuntime.cueVerticalOffset(6.0D, 5.0D), EPSILON);
        assertEquals(0.30D, WildHerdRegroupingPresentationRuntime.cueVerticalOffset(11.0D, 5.0D), EPSILON);
        assertEquals(0.42D, WildHerdRegroupingPresentationRuntime.cueVerticalOffset(17.0D, 5.0D), EPSILON);
        assertEquals(0.42D, WildHerdRegroupingPresentationRuntime.cueVerticalOffset(500.0D, 5.0D), EPSILON);
    }

    @Test
    void cueCenterPointsTowardObservedCanonicalLeaderWithoutPassingIt() {
        var north = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, -12.0D);
        assertEquals(0.0D, north.x(), EPSILON);
        assertEquals(-0.35D, north.z(), EPSILON);

        var diagonal = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(3.0D, 4.0D);
        assertEquals(0.21D, diagonal.x(), EPSILON);
        assertEquals(0.28D, diagonal.z(), EPSILON);
        assertEquals(0.35D, Math.hypot(diagonal.x(), diagonal.z()), EPSILON);

        var close = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.12D, 0.16D);
        assertEquals(0.12D, close.x(), EPSILON);
        assertEquals(0.16D, close.z(), EPSILON);
        assertEquals(0.20D, Math.hypot(close.x(), close.z()), EPSILON);

        var coincident = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, 0.0D);
        assertEquals(0.0D, coincident.x(), EPSILON);
        assertEquals(0.0D, coincident.z(), EPSILON);
    }

    @Test
    void cueTrailCreatesObservedBreadcrumbsTowardLeaderWithoutOvershooting() {
        var north = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.0D, -12.0D);
        assertEquals(3, north.size());
        assertEquals(-0.35D, north.get(0).z(), EPSILON);
        assertEquals(-0.70D, north.get(1).z(), EPSILON);
        assertEquals(-1.05D, north.get(2).z(), EPSILON);
        assertEquals(0.0D, north.get(2).x(), EPSILON);

        var diagonal = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(3.0D, 4.0D);
        assertEquals(3, diagonal.size());
        assertEquals(0.35D, Math.hypot(diagonal.get(0).x(), diagonal.get(0).z()), EPSILON);
        assertEquals(0.70D, Math.hypot(diagonal.get(1).x(), diagonal.get(1).z()), EPSILON);
        assertEquals(1.05D, Math.hypot(diagonal.get(2).x(), diagonal.get(2).z()), EPSILON);
        assertTrue(diagonal.get(2).x() > diagonal.get(1).x());
        assertTrue(diagonal.get(2).z() > diagonal.get(1).z());

        var close = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.30D, 0.40D);
        assertEquals(2, close.size());
        assertEquals(0.35D, Math.hypot(close.get(0).x(), close.get(0).z()), EPSILON);
        assertEquals(0.50D, Math.hypot(close.get(1).x(), close.get(1).z()), EPSILON);
        assertEquals(0.30D, close.get(1).x(), EPSILON);
        assertEquals(0.40D, close.get(1).z(), EPSILON);

        var veryClose = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.12D, 0.16D);
        assertEquals(1, veryClose.size());
        assertEquals(0.12D, veryClose.get(0).x(), EPSILON);
        assertEquals(0.16D, veryClose.get(0).z(), EPSILON);

        var coincident = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.0D, 0.0D);
        assertEquals(1, coincident.size());
        assertEquals(0.0D, coincident.get(0).x(), EPSILON);
        assertEquals(0.0D, coincident.get(0).z(), EPSILON);
    }

    @Test
    void scaledCueTrailUsesBoundedPointCountAndStillStopsAtLeader() {
        var far = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.0D, -12.0D, 5);
        assertEquals(5, far.size());
        assertEquals(-1.75D, far.get(4).z(), EPSILON);

        var close = WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.30D, 0.40D, 5);
        assertEquals(2, close.size());
        assertEquals(0.30D, close.get(1).x(), EPSILON);
        assertEquals(0.40D, close.get(1).z(), EPSILON);
    }

    @Test
    void invalidMinecraftGeometryFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(Double.NaN, 0.0D, 6.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, Double.POSITIVE_INFINITY, 6.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueIntervalTicks(Double.NaN, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueIntervalTicks(10.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueParticleCount(Double.NaN, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueParticleCount(10.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailPointCount(Double.NaN, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailPointCount(10.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueVerticalOffset(Double.POSITIVE_INFINITY, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueVerticalOffset(10.0D, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(0.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(1.0D, 0.0D, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueTrailTowardLeader(1.0D, 0.0D, 6));
    }
}
