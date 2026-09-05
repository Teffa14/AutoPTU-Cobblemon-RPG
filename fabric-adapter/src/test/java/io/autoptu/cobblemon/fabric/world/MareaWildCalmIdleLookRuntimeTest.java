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
    void genericIdleFacingIsDeterministicWithinOneRestWindow() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000123");

        float first = WildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D, 80L, 35.0F);
        float second = WildCalmIdleLookRuntime.idleFacingYaw(actor, 79L, 12.0D, 10.0D, 10.0D, 10.0D, 80L, 35.0F);

        assertEquals(first, second);
        assertTrue(first >= -180.0F && first < 180.0F);
    }

    @Test
    void genericPolicySupportsDifferentPopulationCadencesAndScanWidths() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000123");
        float narrow = WildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D, 80L, 10.0F);
        float wide = WildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D, 80L, 70.0F);

        assertTrue(narrow != wide || narrow == -90.0F);
        WildBehaviorProfile firstPopulation = new WildBehaviorProfile(14.0D, 7.0D, 3, 5, 80L, 60L, 0.001D, 14.0D, 35.0F);
        WildBehaviorProfile secondPopulation = new WildBehaviorProfile(20.0D, 9.0D, 4, 6, 120L, 90L, 0.002D, 18.0D, 20.0F);
        assertTrue(firstPopulation.calmMovementActive(59L));
        assertTrue(!firstPopulation.calmMovementActive(60L));
        assertTrue(secondPopulation.calmMovementActive(89L));
        assertTrue(!secondPopulation.calmMovementActive(90L));
    }

    @Test
    void laterRestWindowCanSelectAnotherScanDirection() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000123");

        float first = WildCalmIdleLookRuntime.idleFacingYaw(actor, 60L, 12.0D, 10.0D, 10.0D, 10.0D, 80L, 35.0F);
        boolean changed = false;
        for (long window = 1L; window <= 12L; window++) {
            float candidate = WildCalmIdleLookRuntime.idleFacingYaw(
                    actor,
                    window * 80L + 60L,
                    12.0D,
                    10.0D,
                    10.0D,
                    10.0D,
                    80L,
                    35.0F);
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
            observed.add(WildCalmIdleLookRuntime.idleFacingYaw(
                    actor,
                    60L,
                    12.0D,
                    10.0D,
                    10.0D,
                    10.0D,
                    80L,
                    35.0F));
        }

        assertTrue(observed.size() > 1);
        assertTrue(observed.size() <= 3);
    }

    @Test
    void invalidCoordinatesAndProfilesFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmIdleLookRuntime.idleFacingYaw(null, 60L, 0.0D, 0.0D, 0.0D, 0.0D, 80L, 35.0F));
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmIdleLookRuntime.idleFacingYaw(UUID.randomUUID(), 60L, Double.NaN, 0.0D, 0.0D, 0.0D, 80L, 35.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new WildBehaviorProfile(14.0D, 15.0D, 3, 5, 80L, 60L, 0.001D, 14.0D, 35.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new WildBehaviorProfile(14.0D, 7.0D, 3, 5, 80L, 80L, 0.001D, 14.0D, 35.0F));
    }
}
