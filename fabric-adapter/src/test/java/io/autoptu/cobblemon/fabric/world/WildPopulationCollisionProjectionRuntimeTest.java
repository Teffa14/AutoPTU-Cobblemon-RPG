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

    @Test
    void dormantActorIsReanchoredOnlyAfterMinecraftPhysicsMovesIt() {
        assertFalse(WildPopulationCollisionProjectionRuntime.shouldReanchor(
                10.5D, 64.0D, -4.5D,
                10.5D, 64.0D, -4.5D
        ));
        assertTrue(WildPopulationCollisionProjectionRuntime.shouldReanchor(
                10.5D, 63.9D, -4.5D,
                10.5D, 64.0D, -4.5D
        ));
        assertTrue(WildPopulationCollisionProjectionRuntime.shouldReanchor(
                10.7D, 64.0D, -4.5D,
                10.5D, 64.0D, -4.5D
        ));
    }
}
