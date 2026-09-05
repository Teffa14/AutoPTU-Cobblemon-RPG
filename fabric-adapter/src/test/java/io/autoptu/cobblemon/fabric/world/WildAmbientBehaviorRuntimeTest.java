package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildAmbientBehaviorRuntimeTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID SIBLING = UUID.fromString("00000000-0000-0000-0000-000000000222");

    @Test
    void roamingTargetIsDeterministicInsideAnyAuthoredHabitatLeash() {
        double[] first = WildAmbientBehaviorRuntime.calmRoamingTarget(ACTOR, 40L, 100.0D, -20.0D, 12, 80L);
        double[] repeated = WildAmbientBehaviorRuntime.calmRoamingTarget(ACTOR, 40L, 100.0D, -20.0D, 12, 80L);
        assertArrayEquals(first, repeated, 0.0000001D);

        double dx = first[0] - 100.0D;
        double dz = first[1] + 20.0D;
        assertTrue(dx * dx + dz * dz < 12.0D * 12.0D);

        double[] nextSegment = WildAmbientBehaviorRuntime.calmRoamingTarget(ACTOR, 120L, 100.0D, -20.0D, 12, 80L);
        assertNotEquals(first[0], nextSegment[0]);
    }

    @Test
    void separationPushesApartAndCohesionPullsTogetherWithoutSpeciesKnowledge() {
        double[] separation = WildAmbientBehaviorRuntime.pairImpulse(
                ACTOR, 0.0D, 0.0D,
                SIBLING, 1.0D, 0.0D,
                2.5D, 0.018D, false);
        assertTrue(separation[0] < 0.0D);
        assertEquals(0.0D, separation[1], 0.0000001D);

        double[] cohesion = WildAmbientBehaviorRuntime.pairImpulse(
                ACTOR, 0.0D, 0.0D,
                SIBLING, 10.0D, 0.0D,
                6.0D, 0.012D, true);
        assertTrue(cohesion[0] > 0.0D);
        assertEquals(0.0D, cohesion[1], 0.0000001D);
    }

    @Test
    void pairBehaviorIsQuietInsideItsConfiguredMiddleBand() {
        assertArrayEquals(
                new double[] {0.0D, 0.0D},
                WildAmbientBehaviorRuntime.pairImpulse(
                        ACTOR, 0.0D, 0.0D,
                        SIBLING, 4.0D, 0.0D,
                        2.5D, 0.018D, false),
                0.0000001D);
        assertArrayEquals(
                new double[] {0.0D, 0.0D},
                WildAmbientBehaviorRuntime.pairImpulse(
                        ACTOR, 0.0D, 0.0D,
                        SIBLING, 4.0D, 0.0D,
                        6.0D, 0.012D, true),
                0.0000001D);
    }

    @Test
    void invalidGenericEcologyInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildAmbientBehaviorRuntime.calmRoamingTarget(ACTOR, 0L, 0.0D, 0.0D, 0, 80L));
        assertThrows(IllegalArgumentException.class,
                () -> WildAmbientBehaviorRuntime.pairImpulse(
                        ACTOR, 0.0D, 0.0D,
                        SIBLING, 1.0D, 0.0D,
                        0.0D, 0.01D, false));
    }
}
