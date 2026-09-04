package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AmbientPokemonBehaviorControllerTest {
    private static final AmbientPokemonBehaviorController.Profile PROFILE =
            new AmbientPokemonBehaviorController.Profile(14.0D, 7.0D, 3, 5);

    @Test
    void reactsOnlyToServerObservedPresenceAndDistance() {
        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);

        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.state());
        assertEquals(AmbientPokemonBehaviorController.State.WATCHING, controller.update(12.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.ALARMED, controller.update(6.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.RECOVERING, controller.update(18.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.RECOVERING, controller.update(18.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.update(18.0D, true));
    }

    @Test
    void abandonedAlarmEventuallyReturnsToCalmWithoutPokemonGameplayState() {
        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);
        controller.update(4.0D, true);

        for (int i = 0; i < 4; i++) {
            assertEquals(AmbientPokemonBehaviorController.State.ALARMED, controller.update(Double.POSITIVE_INFINITY, false));
        }
        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.update(Double.POSITIVE_INFINITY, false));
    }

    @Test
    void recoveryImpulsePointsBackToAuthoredHabitatAnchor() {
        double[] impulse = MareaWildAmbientBehaviorRuntime.recoveryImpulse(10.0D, 10.0D, 13.0D, 14.0D, 0.05D);

        assertEquals(0.03D, impulse[0], 0.0000001D);
        assertEquals(0.04D, impulse[1], 0.0000001D);
    }

    @Test
    void recoveryImpulseIsZeroAtAnchorAndRejectsInvalidInputs() {
        double[] atAnchor = MareaWildAmbientBehaviorRuntime.recoveryImpulse(5.0D, 7.0D, 5.0D, 7.0D, 0.04D);
        assertEquals(0.0D, atAnchor[0], 0.0D);
        assertEquals(0.0D, atAnchor[1], 0.0D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.recoveryImpulse(0.0D, 0.0D, 1.0D, 1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.recoveryImpulse(Double.NaN, 0.0D, 1.0D, 1.0D, 0.04D));
    }

    @Test
    void ambientVelocityIsCappedInsteadOfAccumulatingAcrossTicks() {
        double[] bounded = MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.12D, 0.16D, 0.08D);
        assertEquals(0.048D, bounded[0], 0.0000001D);
        assertEquals(0.064D, bounded[1], 0.0000001D);

        double[] alreadyBounded = MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.03D, 0.04D, 0.08D);
        assertEquals(0.03D, alreadyBounded[0], 0.0000001D);
        assertEquals(0.04D, alreadyBounded[1], 0.0000001D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(Double.NaN, 0.0D, 0.08D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.01D, 0.01D, 0.0D));
    }

    @Test
    void rejectsInvalidAuthoredProfilesAndInvalidObservedDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new AmbientPokemonBehaviorController.Profile(0.0D, 1.0D, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbientPokemonBehaviorController.Profile(5.0D, 6.0D, 1, 1));

        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);
        assertThrows(IllegalArgumentException.class, () -> controller.update(Double.NaN, true));
        assertThrows(IllegalArgumentException.class, () -> controller.update(-1.0D, true));
    }
}