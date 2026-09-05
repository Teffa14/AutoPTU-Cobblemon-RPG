package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmNavigationRuntimeTest {
    @Test
    void stalledRecoveryIsGenericAndProgressResetsTheCounter() {
        var first = WildCalmNavigationRecoveryRuntime.sample(null, 4.0D, 8.0D);
        var stalled1 = WildCalmNavigationRecoveryRuntime.sample(first, 4.01D, 8.01D);
        var stalled2 = WildCalmNavigationRecoveryRuntime.sample(stalled1, 4.02D, 8.02D);
        var stalled3 = WildCalmNavigationRecoveryRuntime.sample(stalled2, 4.03D, 8.03D);

        assertFalse(WildCalmNavigationRecoveryRuntime.shouldRecover(stalled2));
        assertTrue(WildCalmNavigationRecoveryRuntime.shouldRecover(stalled3));

        var progressed = WildCalmNavigationRecoveryRuntime.sample(stalled3, 4.5D, 8.0D);
        assertEquals(0, progressed.stalledSamples());
        assertFalse(WildCalmNavigationRecoveryRuntime.shouldRecover(progressed));
    }

    @Test
    void playerGuardAndCalmWindowGateRecoveryWithoutRegionKnowledge() {
        assertTrue(WildCalmNavigationRecoveryRuntime.presentationEligible(true, false));
        assertFalse(WildCalmNavigationRecoveryRuntime.presentationEligible(false, false));
        assertFalse(WildCalmNavigationRecoveryRuntime.presentationEligible(true, true));
    }

    @Test
    void continuityUsesEachBehaviorProfilesSegmentCadence() {
        assertEquals(2L, WildCalmNavigationContinuityRuntime.calmSegment(160L, 80L));
        assertEquals(4L, WildCalmNavigationContinuityRuntime.calmSegment(160L, 40L));

        assertTrue(WildCalmNavigationContinuityRuntime.shouldRehydrate(4L, 4L, true, false));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(4L, 5L, true, false));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(4L, 4L, false, false));
        assertFalse(WildCalmNavigationContinuityRuntime.shouldRehydrate(4L, 4L, true, true));
    }

    @Test
    void remainingRouteProfilesRejectNewLedgesAndOccupiedNodesAhead() {
        assertTrue(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0, 64, 65, 65, 64));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingSurfaceProfileContinuous(0, 64, 66));

        assertTrue(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(1, false, true, true));
        assertFalse(WildCalmNavigationContinuityRuntime.remainingCollisionProfileClear(1, true, true, false));
    }
}
