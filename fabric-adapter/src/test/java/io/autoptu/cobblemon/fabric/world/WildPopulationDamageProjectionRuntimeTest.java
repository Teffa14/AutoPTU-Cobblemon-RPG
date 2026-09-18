package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildPopulationDamageProjectionRuntimeTest {
    @Test void vanillaDamageShieldCoversEveryCanonicalWildProjection() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldShieldCanonicalProjection(true));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldShieldCanonicalProjection(false));
    }

    @Test void nativeHealthIsRestoredOnlyForCanonicalWildProjectionsBelowMaximum() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(true, 1.0F, 20.0F));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(true, 19.5F, 20.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(true, 20.0F, 20.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(false, 1.0F, 20.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(true, Float.NaN, 20.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeHealth(true, 1.0F, 0.0F));
    }

    @Test void nativeAbsorptionIsClearedOnlyForCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeAbsorption(true, 0.5F));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeAbsorption(true, 4.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeAbsorption(true, 0.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeAbsorption(false, 4.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeAbsorption(true, Float.NaN));
    }

    @Test void nativeStuckArrowsAreClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckArrows(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckArrows(true, 7));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckArrows(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckArrows(false, 7));
    }

    @Test void nativeStuckStingersAreClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckStingers(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckStingers(true, 4));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckStingers(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeStuckStingers(false, 4));
    }

    @Test void nativeFireIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(true, true));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(true, false));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldExtinguishCanonicalProjection(false, true));
    }

    @Test void nativeFreezingIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 140));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFreezing(false, 140));
    }

    @Test void nativeAirIsRestoredOnlyForCanonicalWildProjectionsBelowMaximum() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldRestoreNativeAir(true, 0, 300));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldRestoreNativeAir(true, 299, 300));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeAir(true, 300, 300));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldRestoreNativeAir(false, 0, 300));
    }

    @Test void nativeFallDistanceIsClearedOnlyForCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFallDistance(true, 0.01F));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeFallDistance(true, 18.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFallDistance(true, 0.0F));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeFallDistance(false, 18.0F));
    }

    @Test void nativeHurtAnimationIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeHurtAnimation(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeHurtAnimation(true, 10));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeHurtAnimation(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeHurtAnimation(false, 10));
    }

    @Test void nativeDeathAnimationIsClearedOnlyFromCanonicalWildProjections() {
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeDeathAnimation(true, 1));
        assertTrue(WildPopulationDamageProjectionRuntime.shouldClearNativeDeathAnimation(true, 20));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeDeathAnimation(true, 0));
        assertFalse(WildPopulationDamageProjectionRuntime.shouldClearNativeDeathAnimation(false, 20));
    }

    @Test void leavingProjectionPreservesTheActorsOriginalInvulnerabilityPolicy() {
        assertFalse(WildPopulationDamageProjectionRuntime.restoredInvulnerability(false));
        assertTrue(WildPopulationDamageProjectionRuntime.restoredInvulnerability(true));
    }
}
