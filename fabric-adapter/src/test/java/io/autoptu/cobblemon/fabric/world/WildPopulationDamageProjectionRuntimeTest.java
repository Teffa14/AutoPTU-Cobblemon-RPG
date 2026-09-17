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
    void nativeFireIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(true, true));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(true, false));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(false, true));
    }

    @Test
    void nativeFreezingIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 140));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(false, 140));
    }

    @Test
    void leavingProjectionPreservesTheActorsOriginalInvulnerabilityPolicy() {
        assertFalse(WildPopulationDamageProjectionRuntime.restoredInvulnerability(false));
        assertTrue(WildPopulationDamageProjectionRuntime.restoredInvulnerability(true));
    }
}
