package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationFreezeProjectionRuntimeTest {
    @Test
    void dormantActorWithFrozenTicksIsReset() {
        assertTrue(WildPopulationFreezeProjectionRuntime.shouldResetFrozenTicks(false, false, 40));
        assertTrue(WildPopulationFreezeProjectionRuntime.shouldResetFrozenTicks(true, true, 8));
    }

    @Test
    void activeVisibleActorKeepsVanillaFrozenTicks() {
        assertFalse(WildPopulationFreezeProjectionRuntime.shouldResetFrozenTicks(true, false, 40));
    }

    @Test
    void zeroFrozenTicksNeedsNoWrite() {
        assertFalse(WildPopulationFreezeProjectionRuntime.shouldResetFrozenTicks(false, true, 0));
    }
}
