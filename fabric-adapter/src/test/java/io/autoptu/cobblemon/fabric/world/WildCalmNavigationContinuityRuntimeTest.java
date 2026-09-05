package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmNavigationContinuityRuntimeTest {
    @Test
    void authoredCadenceControlsRouteSegment() {
        assertEquals(0L, WildCalmNavigationContinuityRuntime.calmSegment(59L, 60L));
        assertEquals(1L, WildCalmNavigationContinuityRuntime.calmSegment(60L, 60L));
        assertEquals(0L, WildCalmNavigationContinuityRuntime.calmSegment(79L, 80L));
        assertEquals(1L, WildCalmNavigationContinuityRuntime.calmSegment(80L, 80L));
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmNavigationContinuityRuntime.calmSegment(1L, 0L));
    }

    @Test
    void sameCalmSegmentCanRehydrateAnObservedNativeRoute() {
        long segment = WildCalmNavigationContinuityRuntime.calmSegment(42L, 80L);
        assertTrue(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, true, false));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, false, false));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, true, true));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(null, segment, true, false));
    }

    @Test
    void destinationSegmentChangeRevokesOldRoute() {
        long oldSegment = WildCalmNavigationContinuityRuntime.calmSegment(79L, 80L);
        long newSegment = WildCalmNavigationContinuityRuntime.calmSegment(80L, 80L);
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(oldSegment, newSegment, true, false));
    }

    @Test
    void routeSafetyIgnoresTraversedNodesButRejectsUnsafeRemainingNodes() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(
                2, 64, 70, 65, 66, 65));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(
                1, 64, 65, 68, 67));
        assertTrue(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(
                2, false, false, true, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(
                1, false, true, false, true));
    }

    @Test
    void presentationVolumeSeparatesBlocksFromActiveWildOverlap() {
        assertTrue(WildCalmNavigationContinuityRuntime.presentationNodeClear(true, false));
        assertFalse(WildCalmNavigationContinuityRuntime.presentationNodeClear(false, false));
        assertFalse(WildCalmNavigationContinuityRuntime.presentationNodeClear(true, true));
    }

    @Test
    void invalidRouteCursorsFailClosed() {
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(-1, 64));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(1, 64));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(-1, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(1, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(0));
    }
}
