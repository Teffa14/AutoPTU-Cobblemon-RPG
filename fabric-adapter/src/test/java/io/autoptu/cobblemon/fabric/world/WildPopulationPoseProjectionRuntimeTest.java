package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationPoseProjectionRuntimeTest {
    @Test
    void dormantActorWithHurtPresentationIsCleared() {
        assertTrue(WildPopulationPoseProjectionRuntime.shouldClearHurtPresentation(false, false, 5));
        assertTrue(WildPopulationPoseProjectionRuntime.shouldClearHurtPresentation(true, true, 1));
    }

    @Test
    void activeVisibleActorKeepsVanillaHurtPresentation() {
        assertFalse(WildPopulationPoseProjectionRuntime.shouldClearHurtPresentation(true, false, 5));
    }

    @Test
    void dormantActorWithoutHurtPresentationNeedsNoWrite() {
        assertFalse(WildPopulationPoseProjectionRuntime.shouldClearHurtPresentation(false, true, 0));
    }
}
