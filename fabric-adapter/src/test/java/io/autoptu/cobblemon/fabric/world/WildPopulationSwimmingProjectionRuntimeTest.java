package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationSwimmingProjectionRuntimeTest {
    @Test
    void dormantSwimmingActorIsReset() {
        assertTrue(WildPopulationSwimmingProjectionRuntime.shouldClearSwimming(false, false, true));
        assertTrue(WildPopulationSwimmingProjectionRuntime.shouldClearSwimming(true, true, true));
    }

    @Test
    void activeVisibleActorKeepsVanillaSwimmingPresentation() {
        assertFalse(WildPopulationSwimmingProjectionRuntime.shouldClearSwimming(true, false, true));
    }

    @Test
    void nonSwimmingActorNeedsNoWrite() {
        assertFalse(WildPopulationSwimmingProjectionRuntime.shouldClearSwimming(false, true, false));
    }
}
