package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildAmbientBehaviorRuntimeTest {
    @Test
    void acceptsOnlyVisibleNonSpectatorPlayersForAmbientPerception() {
        assertTrue(MareaWildAmbientBehaviorRuntime.acceptsAmbientPlayer(false, true));
        assertFalse(MareaWildAmbientBehaviorRuntime.acceptsAmbientPlayer(false, false));
        assertFalse(MareaWildAmbientBehaviorRuntime.acceptsAmbientPlayer(true, true));
        assertFalse(MareaWildAmbientBehaviorRuntime.acceptsAmbientPlayer(true, false));
    }
}
