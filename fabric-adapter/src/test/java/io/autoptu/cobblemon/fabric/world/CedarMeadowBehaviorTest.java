package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CedarMeadowBehaviorTest {
    @Test
    void progressesFromCalmToWatchingToAlarmedAndBackToCalm() {
        CedarMeadowBehavior behavior = new CedarMeadowBehavior();

        assertEquals(CedarMeadowBehavior.State.CALM, behavior.state());
        assertEquals(CedarMeadowBehavior.State.WATCHING, behavior.update(10.0D, true));
        assertEquals(CedarMeadowBehavior.State.ALARMED, behavior.update(5.0D, true));
        assertEquals(CedarMeadowBehavior.State.RECOVERING, behavior.update(20.0D, true));

        for (int i = 0; i < 79; i++) {
            behavior.update(Double.POSITIVE_INFINITY, false);
        }
        assertEquals(CedarMeadowBehavior.State.CALM, behavior.update(Double.POSITIVE_INFINITY, false));
    }

    @Test
    void noPlayerEventuallyClearsAnAlarm() {
        CedarMeadowBehavior behavior = new CedarMeadowBehavior();
        behavior.update(3.0D, true);

        for (int i = 0; i < 99; i++) {
            behavior.update(Double.POSITIVE_INFINITY, false);
        }
        assertEquals(CedarMeadowBehavior.State.ALARMED, behavior.state());
        assertEquals(CedarMeadowBehavior.State.CALM, behavior.update(Double.POSITIVE_INFINITY, false));
    }
}
