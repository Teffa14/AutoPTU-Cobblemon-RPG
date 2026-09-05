package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmIdleLookRuntimeTest {
    @Test
    void idleFacingIsDeterministicWithinOneRestWindow() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000123");

        float first = MareaWildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D);
        float second = MareaWildCalmIdleLookRuntime.idleFacingYaw(actor, 79L, 12.0D, 10.0D, 10.0D, 10.0D);

        assertEquals(first, second);
        assertTrue(first >= -180.0F && first < 180.0F);
    }

    @Test
    void laterRestWindowCanSelectAnotherScanDirection() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000123");

        float first = MareaWildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D);
        boolean changed = false;
        for (long window = 1L; window <= 12L; window++) {
            float candidate = MareaWildCalmIdleLookRuntime.idleFacingYaw(
                    actor,
                    window * 80L + 60L,
                    12.0D,
                    10.0D,
                    10.0D,
                    10.0D);
            if (candidate != first) {
                changed = true;
                break;
            }
        }

        assertTrue(changed);
    }

    @Test
    void actorIdentityDistributesIdleScanAcrossAvailableDirections() {
        Set<Float> observed = new HashSet<>();
        for (long lowBits = 1L; lowBits <= 64L; lowBits++) {
            UUID actor = new UUID(0L, lowBits);
            observed.add(MareaWildCalmIdleLookRuntime.idleFacingYaw(
                    actor,
                    60L,
                    12.0D,
                    10.0D,
                    10.0D,
                    10.0D));
        }

        assertTrue(observed.size() > 1);
        assertTrue(observed.size() <= 3);
    }

    @Test
    void invalidCoordinatesFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmIdleLookRuntime.idleFacingYaw(null, 60L, 0.0D, 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmIdleLookRuntime.idleFacingYaw(UUID.randomUUID(), 60L, Double.NaN, 0.0D, 0.0D, 0.0D));
    }
}
