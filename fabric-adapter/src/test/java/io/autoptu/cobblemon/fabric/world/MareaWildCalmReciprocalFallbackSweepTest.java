package io.autoptu.cobblemon.fabric.world;

import net.minecraft.util.math.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmReciprocalFallbackSweepTest {
    @Test
    void convergingFallbackCorridorsConflictBeforeCurrentPeerBoundsEnterActorCorridor() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        Box peerBounds = new Box(1.50D, 0.0D, -0.25D, 2.00D, 1.0D, 0.25D);
        Box actorOnlyCorridor = actorBounds.stretch(0.90D, 0.0D, 0.0D);

        assertFalse(actorOnlyCorridor.intersects(peerBounds));
        assertTrue(MareaWildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                actorBounds, 0.02D, 0.0D,
                peerBounds, -0.02D, 0.0D,
                0.90D));
    }

    @Test
    void verticallySeparatedFallbackCorridorsDoNotConflict() {
        Box bridgeActor = new Box(-0.25D, 4.0D, -0.25D, 0.25D, 5.0D, 0.25D);
        Box underpassPeer = new Box(1.50D, 0.0D, -0.25D, 2.00D, 1.0D, 0.25D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                bridgeActor, 0.02D, 0.0D,
                underpassPeer, -0.02D, 0.0D,
                0.90D));
    }

    @Test
    void stationaryPeerCannotClaimReciprocalFallbackCorridor() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        Box peerBounds = new Box(0.75D, 0.0D, -0.25D, 1.25D, 1.0D, 0.25D);

        assertFalse(MareaWildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                actorBounds, 0.02D, 0.0D,
                peerBounds, 0.0D, 0.0D,
                0.90D));
    }

    @Test
    void reciprocalFallbackSweepRejectsInvalidProbeDistance() {
        Box actorBounds = new Box(-0.25D, 0.0D, -0.25D, 0.25D, 1.0D, 0.25D);
        Box peerBounds = new Box(0.75D, 0.0D, -0.25D, 1.25D, 1.0D, 0.25D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmReciprocalYieldRuntime.sweptReciprocalCorridorsConflict(
                        actorBounds, 0.02D, 0.0D,
                        peerBounds, -0.02D, 0.0D,
                        0.0D));
    }
}
