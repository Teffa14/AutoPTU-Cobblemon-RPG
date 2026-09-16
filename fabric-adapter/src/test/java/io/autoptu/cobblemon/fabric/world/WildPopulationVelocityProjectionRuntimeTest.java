package io.autoptu.cobblemon.fabric.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WildPopulationVelocityProjectionRuntimeTest {
    @Test
    void suspendsPresentationWhenInteractionIsInactiveEvenIfStationary() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldSuspendPresentation(false, false));
    }

    @Test
    void suspendsPresentationWhenActorIsHidden() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldSuspendPresentation(true, true));
    }

    @Test
    void preservesVisibleActiveActorPresentation() {
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldSuspendPresentation(true, false));
    }

    @Test
    void detectsResidualMotionSeparatelyFromDormancy() {
        assertTrue(WildPopulationVelocityProjectionRuntime.hasResidualMotion(0.25));
        assertFalse(WildPopulationVelocityProjectionRuntime.hasResidualMotion(0.0));
    }

    @Test
    void keepsCompatibilityPolicyForResidualVelocity() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(false, false, 0.25));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(true, true, 0.25));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(true, false, 0.25));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(false, true, 0.0));
    }

    @Test
    void clearsVanillaFireOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearFire(false, false, true));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearFire(true, true, true));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearFire(true, false, true));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearFire(false, true, false));
    }

    @Test
    void clearsAccumulatedFallDistanceOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearFallDistance(false, false, 3.5F));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearFallDistance(true, true, 1.0F));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearFallDistance(true, false, 3.5F));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearFallDistance(false, true, 0.0F));
    }

    @Test
    void clearsNativeHurtPresentationOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeHurtPresentation(false, false, 7));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeHurtPresentation(true, true, 3));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeHurtPresentation(true, false, 7));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeHurtPresentation(false, true, 0));
    }

    @Test
    void restoresNativeAirOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldRestoreNativeAir(false, false, 120, 300));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldRestoreNativeAir(true, true, 0, 300));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldRestoreNativeAir(true, false, 120, 300));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldRestoreNativeAir(false, true, 300, 300));
    }

    @Test
    void clearsNativeFreezeProgressOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeFreezeProgress(false, false, 40));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeFreezeProgress(true, true, 1));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeFreezeProgress(true, false, 40));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeFreezeProgress(false, true, 0));
    }

    @Test
    void clearsNativeProjectilePresentationOnlyWhileDormant() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeProjectilePresentation(false, false, 2, 0));
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldClearNativeProjectilePresentation(true, true, 0, 1));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeProjectilePresentation(true, false, 2, 1));
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldClearNativeProjectilePresentation(false, true, 0, 0));
    }
}
