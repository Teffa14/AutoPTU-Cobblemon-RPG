package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        UUID first = UUID.fromString("8e3d8854-0f85-4d7d-844d-a58c91079345");
        UUID second = UUID.fromString("0ddcb831-f81c-44e8-a906-d677be11d464");

        assertEquals(
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(first),
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(first));
        assertNotEquals(
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(first),
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(second));
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
