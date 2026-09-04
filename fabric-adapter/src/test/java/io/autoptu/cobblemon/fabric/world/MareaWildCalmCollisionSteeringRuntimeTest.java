package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmCollisionSteeringRuntimeTest {
    @Test
    void rotatedVelocityPreservesSpeedAcrossDetourAngles() {
        double speed = 0.025D;
        for (double angle : new double[] {-135.0D, -90.0D, -45.0D, 45.0D, 90.0D, 135.0D}) {
            double[] rotated = MareaWildCalmCollisionSteeringRuntime.rotate(speed, 0.0D, angle);
            assertEquals(speed, Math.sqrt(rotated[0] * rotated[0] + rotated[1] * rotated[1]), 0.0000001D);
        }
    }

    @Test
    void actorIdentityDeterministicallyChoosesDetourHandedness() {
        UUID clockwise = new UUID(0L, 0L);
        UUID counterclockwise = new UUID(0L, 1L);

        assertTrue(MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(counterclockwise));
        assertEquals(
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise),
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise));
    }

    @Test
    void invalidSteeringInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(null));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.rotate(Double.NaN, 0.0D, 45.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.rotate(0.01D, 0.0D, Double.POSITIVE_INFINITY));
    }
}
