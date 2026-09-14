package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationAcousticProjectionRuntimeTest {
    @Test
    void audibleOnlyWhenVisibleAndInteractionActive() {
        assertFalse(WildPopulationAcousticProjectionRuntime.shouldMute(true, false));
        assertTrue(WildPopulationAcousticProjectionRuntime.shouldMute(false, false));
        assertTrue(WildPopulationAcousticProjectionRuntime.shouldMute(true, true));
        assertTrue(WildPopulationAcousticProjectionRuntime.shouldMute(false, true));
    }
}
