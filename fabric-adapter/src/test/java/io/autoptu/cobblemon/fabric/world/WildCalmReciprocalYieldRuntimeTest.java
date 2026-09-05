package io.autoptu.cobblemon.fabric.world;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmReciprocalYieldRuntimeTest {
    @Test
    void canonicalOrderingIsStableAcrossPopulationNames() {
        assertTrue(WildCalmReciprocalYieldRuntime.shouldYield("wild:region-b:004", "wild:region-a:002"));
        assertFalse(WildCalmReciprocalYieldRuntime.shouldYield("wild:region-a:002", "wild:region-b:004"));
        assertEquals(
                "wild:region-a:002",
                WildCalmReciprocalYieldRuntime.preferredConflictingCanonicalPeer(
                        "wild:region-z:999",
                        List.of("wild:region-c:010", "wild:region-a:002", "wild:region-b:004")));
    }

    @Test
    void convergingActorsConflictBeforeContactButSeparateHeightsDoNot() {
        Box actor = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        Box peer = new Box(1.50D, 0.0D, -0.25D, 2.00D, 1.0D, 0.25D);
        assertTrue(WildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                actor, 0.02D, 0.0D, peer, -0.02D, 0.0D, 0.90D));

        Box elevatedPeer = new Box(1.50D, 4.0D, -0.25D, 2.00D, 5.0D, 0.25D);
        assertFalse(WildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                actor, 0.02D, 0.0D, elevatedPeer, -0.02D, 0.0D, 0.90D));
    }

    @Test
    void pathIntentPreservesThreeDimensionalSegments() {
        Box actor = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        List<WildCalmReciprocalYieldRuntime.DirectedCorridor> corridors =
                WildCalmReciprocalYieldRuntime.pathIntentCorridors(
                        actor,
                        new Vec3d(0.0D, 0.0D, 0.0D),
                        List.of(new Vec3d(1.0D, 1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 1.0D)),
                        5.0D);
        assertEquals(2, corridors.size());
        assertTrue(corridors.get(0).corridor().maxY > actor.maxY);
    }

    @Test
    void yieldLeaseExpiresAndClearsWhenConflictClears() {
        assertTrue(WildCalmReciprocalYieldRuntime.shouldRetainLease(10L, 40L, true, true, true));
        assertFalse(WildCalmReciprocalYieldRuntime.shouldRetainLease(40L, 40L, true, true, true));
        assertFalse(WildCalmReciprocalYieldRuntime.shouldRetainLease(10L, 40L, true, true, false));
    }
}
