package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationFallDistanceProjectionRuntimeTest {
    @Test
    void dormantActorWithFallDistanceIsReset() {
        assertTrue(WildPopulationFallDistanceProjectionRuntime.shouldResetFallDistance(false, false, 6.5F));
        assertTrue(WildPopulationFallDistanceProjectionRuntime.shouldResetFallDistance(true, true, 2.0F));
    }

    @Test
    void activeVisibleActorKeepsVanillaFallDistance() {
        assertFalse(WildPopulationFallDistanceProjectionRuntime.shouldResetFallDistance(true, false, 6.5F));
    }

    @Test
    void zeroFallDistanceNeedsNoWrite() {
        assertFalse(WildPopulationFallDistanceProjectionRuntime.shouldResetFallDistance(false, true, 0.0F));
    }
}
