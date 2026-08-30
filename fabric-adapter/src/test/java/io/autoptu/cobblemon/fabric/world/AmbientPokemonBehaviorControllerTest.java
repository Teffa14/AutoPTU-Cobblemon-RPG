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