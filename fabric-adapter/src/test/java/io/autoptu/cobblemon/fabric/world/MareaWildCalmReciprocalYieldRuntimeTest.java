package io.autoptu.cobblemon.fabric.world;

import net.minecraft.util.math.Box;
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
    void publishedCorridorsDetectFutureCrossingBeforeActorBoxesTouch() {
        Box actorCorridor = new Box(0.0D, 0.0D, 0.0D, 2.5D, 1.0D, 1.0D);
        Box peerCorridor = new Box(1.5D, 0.0D, -1.0D, 2.5D, 1.0D, 2.0D);

        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorsConflict(
                1.0D, 0.0D, actorCorridor,
                0.0D, 1.0D, peerCorridor));
    }

    @Test
    void publishedPeerIntentKeepsYieldBeforePhysicalContact() {
        Box yieldCorridor = new Box(0.0D, 0.0D, 0.0D, 2.5D, 1.0D, 1.0D);
        Box peerBounds = new Box(3.0D, 0.0D, 1.5D, 3.5D, 1.0D, 2.0D);
        Box peerIntent = new Box(1.5D, 0.0D, 0.5D, 3.5D, 1.0D, 2.0D);

        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorOccupiedOrClaimed(
                yieldCorridor, peerBounds, peerIntent));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorOccupiedOrClaimed(
                yieldCorridor, peerBounds, null));
    }

    @Test
    void parallelCorridorsDoNotCreateArtificialYield() {
        Box actorCorridor = new Box(0.0D, 0.0D, 0.0D, 3.0D, 1.0D, 1.0D);
        Box peerCorridor = new Box(0.5D, 0.0D, 0.2D, 3.5D, 1.0D, 1.2D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorsConflict(
                1.0D, 0.0D, actorCorridor,
                1.0D, 0.0D, peerCorridor));
    }

    @Test
    void separatedCorridorsDoNotConflict() {
        Box actorCorridor = new Box(0.0D, 0.0D, 0.0D, 2.0D, 1.0D, 1.0D);
        Box peerCorridor = new Box(3.0D, 0.0D, 3.0D, 5.0D, 1.0D, 4.0D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorsConflict(
                1.0D, 0.0D, actorCorridor,
                -1.0D, 0.0D, peerCorridor));
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
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.corridorsConflict(
                        Double.NaN, 0.0D, new Box(0, 0, 0, 1, 1, 1),
                        0.0D, 1.0D, new Box(0, 0, 0, 1, 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.corridorOccupiedOrClaimed(
                        null, new Box(0, 0, 0, 1, 1, 1), null));
    }
}
