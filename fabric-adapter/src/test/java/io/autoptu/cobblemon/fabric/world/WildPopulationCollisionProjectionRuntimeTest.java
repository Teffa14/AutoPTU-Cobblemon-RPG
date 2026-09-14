package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationCollisionProjectionRuntimeTest {
    @Test
    void collisionIsEnabledOnlyForVisibleInteractionActiveActors() {
        assertFalse(WildPopulationCollisionProjectionRuntime.shouldDisableCollision(true, false));
        assertTrue(WildPopulationCollisionProjectionRuntime.shouldDisableCollision(false, false));
        assertTrue(WildPopulationCollisionProjectionRuntime.shouldDisableCollision(true, true));
        assertTrue(WildPopulationCollisionProjectionRuntime.shouldDisableCollision(false, true));
    }
}
