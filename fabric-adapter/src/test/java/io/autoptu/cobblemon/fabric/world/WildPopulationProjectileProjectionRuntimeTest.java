package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationProjectileProjectionRuntimeTest {
    @Test
    void dormantActorWithEmbeddedArrowsIsCleared() {
        assertTrue(WildPopulationProjectileProjectionRuntime.shouldClearEmbeddedArrows(false, false, 2));
        assertTrue(WildPopulationProjectileProjectionRuntime.shouldClearEmbeddedArrows(true, true, 1));
    }

    @Test
    void activeVisibleActorKeepsVanillaProjectilePresentation() {
        assertFalse(WildPopulationProjectileProjectionRuntime.shouldClearEmbeddedArrows(true, false, 2));
    }

    @Test
    void actorWithoutEmbeddedArrowsNeedsNoWrite() {
        assertFalse(WildPopulationProjectileProjectionRuntime.shouldClearEmbeddedArrows(false, true, 0));
    }
}
