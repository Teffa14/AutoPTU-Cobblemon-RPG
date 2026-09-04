package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

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
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                oldSegment, newSegment, true, false));
    }

    @Test
    void unobservedRouteCannotBeManufactured() {
        assertFalse(MareaWildCalmNavigationContinuityRuntime.shouldRehydrate(
                null, 3L, true, false));
    }
}
