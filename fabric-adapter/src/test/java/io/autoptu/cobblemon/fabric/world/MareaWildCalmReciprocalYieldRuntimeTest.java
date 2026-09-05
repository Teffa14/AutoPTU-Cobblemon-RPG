package io.autoptu.cobblemon.fabric.world;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void nativePathCorridorPreservesTurnsInsteadOfFillingTheCorner() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        var segments = MareaWildCalmReciprocalYieldRuntime.buildPathCorridorSegments(
                actorBounds,
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 1.0D)),
                2.4D);

        assertEquals(2, segments.size());
        assertEquals(1.0D, segments.get(0).directionX(), 0.000001D);
        assertEquals(0.0D, segments.get(0).directionZ(), 0.000001D);
        assertEquals(0.0D, segments.get(1).directionX(), 0.000001D);
        assertEquals(1.0D, segments.get(1).directionZ(), 0.000001D);
        assertFalse(segments.get(0).corridor().intersects(
                new Box(0.75D, 0.0D, 0.75D, 1.25D, 1.0D, 1.25D)));
        assertTrue(segments.get(1).corridor().intersects(
                new Box(0.75D, 0.0D, 0.75D, 1.25D, 1.0D, 1.25D)));
    }

    @Test
    void nativePathCorridorStopsAtTheShortIntentHorizon() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        var segments = MareaWildCalmReciprocalYieldRuntime.buildPathCorridorSegments(
                actorBounds,
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(new Vec3d(5.0D, 0.0D, 0.0D)),
                2.4D);

        assertEquals(1, segments.size());
        assertEquals(2.65D, segments.get(0).corridor().maxX, 0.000001D);
    }

    @Test
    void segmentedIntentDetectsConflictOnALaterTurn() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        var actorSegments = MareaWildCalmReciprocalYieldRuntime.buildPathCorridorSegments(
                actorBounds,
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 1.0D)),
                2.4D);
        var peerSegments = MareaWildCalmReciprocalYieldRuntime.buildPathCorridorSegments(
                actorBounds,
                new Vec3d(2.0D, 0.0D, 1.0D),
                List.of(new Vec3d(1.0D, 0.0D, 1.0D)),
                2.4D);

        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorSegmentsConflict(
                actorSegments, peerSegments));
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
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.buildPathCorridorSegments(
                        new Box(0, 0, 0, 1, 1, 1),
                        new Vec3d(0.0D, 0.0D, 0.0D),
                        List.of(new Vec3d(Double.NaN, 0.0D, 0.0D)),
                        2.4D));
    }
}
