package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationFireProjectionRuntimeTest {
    @Test
    void clearsFireOnlyForBurningDormantPresentationActors() {
        assertTrue(WildPopulationFireProjectionRuntime.shouldClearFire(false, false, true));
        assertTrue(WildPopulationFireProjectionRuntime.shouldClearFire(true, true, true));
        assertFalse(WildPopulationFireProjectionRuntime.shouldClearFire(true, false, true));
        assertFalse(WildPopulationFireProjectionRuntime.shouldClearFire(false, true, false));
    }
}
