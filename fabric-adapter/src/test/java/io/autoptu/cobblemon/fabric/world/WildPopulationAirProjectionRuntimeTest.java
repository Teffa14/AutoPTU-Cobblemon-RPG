package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationAirProjectionRuntimeTest {
    @Test
    void dormantActorWithDepletedAirIsRestored() {
        assertTrue(WildPopulationAirProjectionRuntime.shouldRestoreAir(false, true, 40, 300));
        assertTrue(WildPopulationAirProjectionRuntime.shouldRestoreAir(false, false, 40, 300));
        assertTrue(WildPopulationAirProjectionRuntime.shouldRestoreAir(true, true, 40, 300));
    }

    @Test
    void activeVisibleActorKeepsVanillaAirBehavior() {
        assertFalse(WildPopulationAirProjectionRuntime.shouldRestoreAir(true, false, 40, 300));
    }

    @Test
    void fullAirNeedsNoWrite() {
        assertFalse(WildPopulationAirProjectionRuntime.shouldRestoreAir(false, true, 300, 300));
    }
}
