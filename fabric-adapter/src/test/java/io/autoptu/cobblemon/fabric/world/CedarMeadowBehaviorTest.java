package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CedarMeadowBehaviorTest {
    private static final AmbientPokemonBehaviorController.Profile CEDAR_PROFILE =
            new AmbientPokemonBehaviorController.Profile(14.0D, 7.0D, 80, 100);

    @Test
    void cedarProfileProgressesFromCalmToWatchingToAlarmedAndBackToCalm() {
        AmbientPokemonBehaviorController behavior = new AmbientPokemonBehaviorController(CEDAR_PROFILE);

        assertEquals(AmbientPokemonBehaviorController.State.CALM, behavior.state());
        assertEquals(AmbientPokemonBehaviorController.State.WATCHING, behavior.update(10.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.ALARMED, behavior.update(5.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.RECOVERING, behavior.update(20.0D, true));

        for (int i = 0; i < 79; i++) {
            behavior.update(Double.POSITIVE_INFINITY, false);
        }
        assertEquals(AmbientPokemonBehaviorController.State.CALM,
                behavior.update(Double.POSITIVE_INFINITY, false));
    }

    @Test
    void cedarProfileNoPlayerEventuallyClearsAnAlarm() {
        AmbientPokemonBehaviorController behavior = new AmbientPokemonBehaviorController(CEDAR_PROFILE);
        behavior.update(3.0D, true);

        for (int i = 0; i < 99; i++) {
            behavior.update(Double.POSITIVE_INFINITY, false);
        }
        assertEquals(AmbientPokemonBehaviorController.State.ALARMED, behavior.state());
        assertEquals(AmbientPokemonBehaviorController.State.CALM,
                behavior.update(Double.POSITIVE_INFINITY, false));
    }
}
