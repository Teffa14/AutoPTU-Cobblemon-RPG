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
    void nativePathIntentFollowsUpcomingTurnInsteadOfOneStraightVelocityEnvelope() {
        Box actorBounds = new Box(-0.3D, 0.0D, -0.3D, 0.3D, 1.0D, 0.3D);
        List<MareaWildCalmReciprocalYieldRuntime.DirectedCorridor> corridors =
                MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                        actorBounds,
                        new Vec3d(0.0D, 0.0D, 0.0D),
                        List.of(
                                new Vec3d(1.0D, 0.0D, 0.0D),
                                new Vec3d(1.0D, 0.0D, 2.0D)),
                        5.0D);

        assertEquals(2, corridors.size());
        assertEquals(1.0D, corridors.get(0).directionX(), 0.000001D);
        assertEquals(0.0D, corridors.get(0).directionZ(), 0.000001D);
        assertEquals(0.0D, corridors.get(1).directionX(), 0.000001D);
        assertEquals(1.0D, corridors.get(1).directionZ(), 0.000001D);
        assertFalse(corridors.get(1).corridor().contains(2.0D, 0.5D, 0.0D));
    }

    @Test
    void nativePathIntentCarriesNodeElevationIntoCorridorVolume() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        var corridors = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                actorBounds,
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(new Vec3d(2.0D, 2.0D, 0.0D)),
                5.0D);

        assertEquals(1, corridors.size());
        Box rising = corridors.getFirst().corridor();
        assertTrue(rising.maxY >= 3.0D - 0.000001D);
        assertTrue(rising.contains(1.0D, 1.5D, 0.0D));
    }

    @Test
    void verticallySeparatedCrossingPathsDoNotCreateFalseYield() {
        var bridgeCorridors = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                new Box(-0.25D, 4.0D, -0.25D, 0.25D, 5.0D, 0.25D),
                new Vec3d(0.0D, 4.0D, 0.0D),
                List.of(new Vec3d(2.0D, 4.0D, 0.0D)),
                5.0D);
        var underpassCorridors = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                new Box(0.75D, 0.0D, -1.25D, 1.25D, 1.0D, -0.75D),
                new Vec3d(1.0D, 0.0D, -1.0D),
                List.of(new Vec3d(1.0D, 0.0D, 1.0D)),
                5.0D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorsConflict(
                bridgeCorridors, underpassCorridors));
    }

    @Test
    void nativePathIntentStopsAtBoundedLookaheadDistance() {
        Box actorBounds = new Box(-0.3D, 0.0D, -0.3D, 0.3D, 1.0D, 0.3D);
        List<MareaWildCalmReciprocalYieldRuntime.DirectedCorridor> corridors =
                MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                        actorBounds,
                        new Vec3d(0.0D, 0.0D, 0.0D),
                        List.of(new Vec3d(10.0D, 0.0D, 0.0D)),
                        2.5D);

        assertEquals(1, corridors.size());
        Box bounded = corridors.getFirst().corridor();
        assertTrue(bounded.maxX <= 2.8D + 0.000001D);
        assertFalse(bounded.contains(4.0D, 0.5D, 0.0D));
    }

    @Test
    void remainingPathSegmentsDetectCurvedCrossingBeforeActorBoxesTouch() {
        var actorCorridors = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D),
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 2.0D)),
                5.0D);
        var peerCorridors = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                new Box(1.75D, 0.0D, 0.75D, 2.25D, 1.0D, 1.25D),
                new Vec3d(2.0D, 0.0D, 1.0D),
                List.of(new Vec3d(1.0D, 0.0D, 1.0D)),
                5.0D);

        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorsConflict(actorCorridors, peerCorridors));
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
        var peerIntent = List.of(new MareaWildCalmReciprocalYieldRuntime.DirectedCorridor(
                -1.0D,
                0.0D,
                new Box(1.5D, 0.0D, 0.5D, 3.5D, 1.0D, 2.0D)));

        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorOccupiedOrClaimed(
                yieldCorridor, peerBounds, peerIntent));
        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorOccupiedOrClaimed(
                yieldCorridor, peerBounds, List.of()));
    }

    @Test
    void yieldLeaseUsesSegmentedPathElevationInsteadOfCurrentHeightStraightCorridor() {
        var yieldingRoute = MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D),
                new Vec3d(0.0D, 0.0D, 0.0D),
                List.of(
                        new Vec3d(1.0D, 0.0D, 0.0D),
                        new Vec3d(1.0D, 4.0D, 0.0D),
                        new Vec3d(3.0D, 4.0D, 0.0D)),
                5.0D);

        Box underElevatedSegment = new Box(1.75D, 0.0D, -0.2D, 2.25D, 1.0D, 0.2D);
        Box onElevatedSegment = new Box(1.75D, 4.0D, -0.2D, 2.25D, 5.0D, 0.2D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorsOccupiedOrClaimed(
                yieldingRoute, underElevatedSegment, List.of()));
        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorsOccupiedOrClaimed(
                yieldingRoute, onElevatedSegment, List.of()));
    }

    @Test
    void yieldLeaseRetainsAgainstPeerClaimOnlyWhereSegmentedRoutesActuallyOverlap() {
        var yieldingRoute = List.of(
                new MareaWildCalmReciprocalYieldRuntime.DirectedCorridor(
                        1.0D, 0.0D, new Box(0.0D, 4.0D, 0.0D, 3.0D, 5.0D, 0.5D)));
        Box peerBounds = new Box(3.5D, 0.0D, 1.0D, 4.0D, 1.0D, 1.5D);
        var lowPeerClaim = List.of(
                new MareaWildCalmReciprocalYieldRuntime.DirectedCorridor(
                        0.0D, -1.0D, new Box(1.5D, 0.0D, -0.5D, 2.0D, 1.0D, 1.5D)));
        var highPeerClaim = List.of(
                new MareaWildCalmReciprocalYieldRuntime.DirectedCorridor(
                        0.0D, -1.0D, new Box(1.5D, 4.0D, -0.5D, 2.0D, 5.0D, 1.5D)));

        assertFalse(MareaWildCalmReciprocalYieldRuntime.corridorsOccupiedOrClaimed(
                yieldingRoute, peerBounds, lowPeerClaim));
        assertTrue(MareaWildCalmReciprocalYieldRuntime.corridorsOccupiedOrClaimed(
                yieldingRoute, peerBounds, highPeerClaim));
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
                        null, new Box(0, 0, 0, 1, 1, 1), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.corridorsOccupiedOrClaimed(
                        null, new Box(0, 0, 0, 1, 1, 1), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.pathIntentCorridors(
                        new Box(0, 0, 0, 1, 1, 1),
                        new Vec3d(0.0D, 0.0D, 0.0D),
                        List.of(new Vec3d(Double.NaN, 0.0D, 0.0D)),
                        5.0D));
    }
}
