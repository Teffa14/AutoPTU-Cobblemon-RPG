package io.autoptu.cobblemon.fabric.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WildPopulationVelocityProjectionRuntimeTest {
    @Test
    void stopsResidualMotionWhenInteractionIsInactive() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(false, false, 0.25));
    }

    @Test
    void stopsResidualMotionWhenActorIsHidden() {
        assertTrue(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(true, true, 0.25));
    }

    @Test
    void preservesVisibleActiveActorMotion() {
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(true, false, 0.25));
    }

    @Test
    void doesNotRewriteAlreadyStationaryDormantActor() {
        assertFalse(WildPopulationVelocityProjectionRuntime.shouldStopResidualMotion(false, true, 0.0));
    }
}
