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
    void cueDensityScalesWithObservedDistanceBeyondAuthoredCohesion() {
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueParticleCount(5.0D, 5.0D));
        assertEquals(2, WildHerdRegroupingPresentationRuntime.cueParticleCount(8.9D, 5.0D));
        assertEquals(3, WildHerdRegroupingPresentationRuntime.cueParticleCount(9.0D, 5.0D));
        assertEquals(4, WildHerdRegroupingPresentationRuntime.cueParticleCount(13.0D, 5.0D));
        assertEquals(6, WildHerdRegroupingPresentationRuntime.cueParticleCount(500.0D, 5.0D));
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
    void cueCenterPointsTowardObservedCanonicalLeader() {
        var north = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, -12.0D);
        assertEquals(0.0D, north.x(), EPSILON);
        assertEquals(-0.35D, north.z(), EPSILON);

        var diagonal = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(3.0D, 4.0D);
        assertEquals(0.21D, diagonal.x(), EPSILON);
        assertEquals(0.28D, diagonal.z(), EPSILON);
        assertEquals(0.35D, Math.hypot(diagonal.x(), diagonal.z()), EPSILON);

        var coincident = WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, 0.0D);
        assertEquals(0.0D, coincident.x(), EPSILON);
        assertEquals(0.0D, coincident.z(), EPSILON);
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
                () -> WildHerdRegroupingPresentationRuntime.cueParticleCount(Double.NaN, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueParticleCount(10.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueVerticalOffset(Double.POSITIVE_INFINITY, 5.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueVerticalOffset(10.0D, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.cueOffsetTowardLeader(0.0D, Double.NEGATIVE_INFINITY));
    }
}
