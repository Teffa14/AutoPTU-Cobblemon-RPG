package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationDamageProjectionRuntimeTest {
    @Test
    void vanillaDamageShieldIsEnabledOnlyWhilePresentationIsInactive() {
        assertFalse(WildPopulationDamageProjectionRuntime.shouldShield(true, false));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldShield(false, false));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldShield(true, true));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldShield(false, true));
    }

    @Test
    void reactivationPreservesTheActorsOriginalInvulnerabilityPolicy() {
        assertFalse(WildPopulationDamageProjectionRuntime.restoredInvulnerability(false));
        assertTrue(WildPopulationDamageProjectionRuntime.restoredInvulnerability(true));
    }
}
