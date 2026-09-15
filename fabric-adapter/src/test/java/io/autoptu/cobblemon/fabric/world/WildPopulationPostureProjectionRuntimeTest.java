package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationPostureProjectionRuntimeTest {
    @Test
    void dormantSneakingActorIsReset() {
        assertTrue(WildPopulationPostureProjectionRuntime.shouldClearSneak(false, false, true));
        assertTrue(WildPopulationPostureProjectionRuntime.shouldClearSneak(true, true, true));
    }

    @Test
    void activeVisibleActorKeepsVanillaPosture() {
        assertFalse(WildPopulationPostureProjectionRuntime.shouldClearSneak(true, false, true));
    }

    @Test
    void standingActorNeedsNoWrite() {
        assertFalse(WildPopulationPostureProjectionRuntime.shouldClearSneak(false, true, false));
    }
}
