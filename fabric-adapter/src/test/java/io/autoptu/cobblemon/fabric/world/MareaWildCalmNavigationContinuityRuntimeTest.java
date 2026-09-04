package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmNavigationContinuityRuntimeTest {
    @Test
    void sameCalmSegmentCanRehydrateAnObservedNativeRoute() {
        long segment = MareaWildCalmNavigationContinuityRuntime.calmSegment(42L);
        assertTrue(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                segment, segment, true, false));
    }

    @Test
    void restWindowRevokesContinuity() {
        long segment = MareaWildCalmNavigationContinuityRuntime.calmSegment(65L);
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                segment, segment, false, false));
    }

    @Test
    void nearbyPlayerRevokesContinuity() {
        long segment = MareaWildCalmNavigationContinuityRuntime.calmSegment(42L);
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                segment, segment, true, true));
    }

    @Test
    void deterministicDestinationSegmentChangeRevokesOldRoute() {
        long oldSegment = MareaWildCalmNavigationContinuityRuntime.calmSegment(79L);
        long newSegment = MareaWildCalmNavigationContinuityRuntime.calmSegment(80L);
        assertEquals(0L, oldSegment);
        assertEquals(1L, newSegment);
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                oldSegment, newSegment, true, false));
    }

    @Test
    void unobservedRouteCannotBeManufactured() {
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                null, 3L, true, false));
    }

    @Test
    void activeRouteRevalidationIgnoresTerrainAlreadyTraversed() {
        assertTrue(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(
                2, 64, 70, 65, 66, 65));
    }

    @Test
    void activeRouteRevalidationRejectsNewAbruptTerrainAhead() {
        assertFalse(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(
                1, 64, 65, 68, 67));
        assertFalse(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(
                0, 64, 61));
    }

    @Test
    void activeRouteRevalidationFailsClosedForInvalidCursor() {
        assertFalse(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(-1, 64));
        assertFalse(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(1, 64));
        assertFalse(MareaWildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0));
    }
}
