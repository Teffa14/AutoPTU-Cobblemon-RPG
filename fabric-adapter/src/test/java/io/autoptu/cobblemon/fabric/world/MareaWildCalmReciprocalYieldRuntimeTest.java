package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmReciprocalYieldRuntimeTest {
    @Test
    void canonicalIdentitySelectsExactlyOneYieldingActor() {
        String priority = "ouros.marea.lower_shelf.member_01";
        String yielding = "ouros.marea.lower_shelf.member_02";

        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldYield(priority, yielding));
        assertTrue(MareaWildCalmReciprocalYieldRuntime.shouldYield(yielding, priority));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldYield(priority, priority));
    }

    @Test
    void yieldLeasePersistsOnlyWhileCanonicalPresentationPeerStillOccupiesCorridor() {
        assertTrue(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                110L, 140L, true, true, true));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                110L, 140L, true, true, false));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                110L, 140L, true, false, true));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                110L, 140L, false, true, true));
    }

    @Test
    void yieldLeaseHasHardExpiryToPreventStalePresentationDeadlock() {
        assertTrue(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                139L, 140L, true, true, true));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                140L, 140L, true, true, true));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                141L, 140L, true, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.shouldRetainLease(
                        -1L, 140L, true, true, true));
    }

    @Test
    void reciprocalApproachRequiresBothActorsToMoveTowardEachOther() {
        assertTrue(MareaWildCalmReciprocalYieldRuntime.reciprocalApproach(
                0.0D, 0.0D, 0.02D, 0.0D,
                1.0D, 0.0D, -0.02D, 0.0D));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.reciprocalApproach(
                0.0D, 0.0D, 0.02D, 0.0D,
                1.0D, 0.0D, 0.02D, 0.0D));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.reciprocalApproach(
                0.0D, 0.0D, -0.02D, 0.0D,
                1.0D, 0.0D, -0.02D, 0.0D));
    }

    @Test
    void exactOverlapIsTreatedAsReciprocalSoCanonicalPriorityCanBreakDeadlock() {
        assertTrue(MareaWildCalmReciprocalYieldRuntime.reciprocalApproach(
                4.5D, 9.5D, 0.02D, 0.0D,
                4.5D, 9.5D, -0.02D, 0.0D));
    }

    @Test
    void invalidCanonicalOrGeometryInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.shouldYield("", "peer"));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.shouldYield("actor", null));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.reciprocalApproach(
                        Double.NaN, 0.0D, 0.02D, 0.0D,
                        1.0D, 0.0D, -0.02D, 0.0D));
    }
}
