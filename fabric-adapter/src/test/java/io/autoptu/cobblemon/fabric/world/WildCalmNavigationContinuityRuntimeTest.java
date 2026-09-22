package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmNavigationContinuityRuntimeTest {
    private static final long SEGMENT_TICKS = 80L;

    @Test
    void sameCalmSegmentCanRehydrateAnObservedNativeRoute() {
        long segment = WildCalmNavigationContinuityRuntime.calmSegment(42L, SEGMENT_TICKS);
        assertTrue(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, true, false));
    }

    @Test
    void restWindowRevokesContinuity() {
        long segment = WildCalmNavigationContinuityRuntime.calmSegment(65L, SEGMENT_TICKS);
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, false, false));
    }

    @Test
    void nearbyPlayerRevokesContinuity() {
        long segment = WildCalmNavigationContinuityRuntime.calmSegment(42L, SEGMENT_TICKS);
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(segment, segment, true, true));
    }

    @Test
    void deterministicDestinationSegmentChangeRevokesOldRoute() {
        long oldSegment = WildCalmNavigationContinuityRuntime.calmSegment(79L, SEGMENT_TICKS);
        long newSegment = WildCalmNavigationContinuityRuntime.calmSegment(80L, SEGMENT_TICKS);
        assertEquals(0L, oldSegment);
        assertEquals(1L, newSegment);
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(oldSegment, newSegment, true, false));
    }

    @Test
    void unobservedRouteCannotBeManufactured() {
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(null, 3L, true, false));
    }

    @Test
    void activeRouteRevalidationIgnoresTerrainAlreadyTraversed() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(2, 64, 70, 65, 66, 65));
    }

    @Test
    void lastRemainingNodeDoesNotRevalidateTraversedTerrain() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(2, 64, 99, 65));
    }

    @Test
    void activeRouteRevalidationRejectsNewAbruptTerrainAhead() {
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(1, 64, 65, 68, 67));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0, 64, 61));
    }

    @Test
    void activeRouteCollisionRevalidationRejectsNewWallAhead() {
        assertFalse(WildCalmCollisionNavigationRuntime.presentationNodeClear(false, false));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(1, false, true, false, true));
    }

    @Test
    void activeRouteCollisionRevalidationRejectsAnotherActiveWildActorAhead() {
        assertFalse(WildCalmCollisionNavigationRuntime.presentationNodeClear(true, true));
    }

    @Test
    void activeRouteCollisionRevalidationAcceptsClearPresentationVolume() {
        assertTrue(WildCalmCollisionNavigationRuntime.presentationNodeClear(true, false));
    }

    @Test
    void activeRouteCollisionRevalidationIgnoresBlockedVolumeAlreadyTraversed() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(2, false, false, true, true));
    }

    @Test
    void activeRouteCollisionRevalidationAcceptsClearRemainingVolume() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(0, true, true, true));
    }

    @Test
    void activeRouteRevalidationFailsClosedForInvalidCursor() {
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(-1, 64));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(1, 64));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(-1, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(1, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(0));
    }

    @Test
    void segmentLengthMustComeFromBehaviorProfile() {
        assertEquals(2L, WildCalmNavigationContinuityRuntime.calmSegment(160L, SEGMENT_TICKS));
        boolean rejected = false;
        try {
            WildCalmNavigationContinuityRuntime.calmSegment(160L, 0L);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }
}
