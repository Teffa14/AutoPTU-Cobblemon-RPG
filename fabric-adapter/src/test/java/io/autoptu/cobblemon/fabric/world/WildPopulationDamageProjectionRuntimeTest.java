package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationDamageProjectionRuntimeTest {
    @Test
    void vanillaDamageShieldCoversEveryCanonicalWildProjection() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldShieldCanonicalProjection(true));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldShieldCanonicalProjection(false));
    }

    @Test
    void leavingProjectionPreservesTheActorsOriginalInvulnerabilityPolicy() {
        assertFalse(WildPopulationDamageProjectionRuntime.restoredInvulnerability(false));
        assertTrue(WildPopulationDamageProjectionRuntime.restoredInvulnerability(true));
    }
}
