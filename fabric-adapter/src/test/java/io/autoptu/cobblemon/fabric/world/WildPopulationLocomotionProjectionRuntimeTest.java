package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationLocomotionProjectionRuntimeTest {
    @Test
    void dormantSprintingActorIsReset() {
        assertTrue(WildPopulationLocomotionProjectionRuntime.shouldClearSprint(false, false, true));
        assertTrue(WildPopulationLocomotionProjectionRuntime.shouldClearSprint(true, true, true));
    }

    @Test
    void activeVisibleActorKeepsVanillaSprintPresentation() {
        assertFalse(WildPopulationLocomotionProjectionRuntime.shouldClearSprint(true, false, true));
    }

    @Test
    void nonSprintingActorNeedsNoWrite() {
        assertFalse(WildPopulationLocomotionProjectionRuntime.shouldClearSprint(false, true, false));
    }
}
