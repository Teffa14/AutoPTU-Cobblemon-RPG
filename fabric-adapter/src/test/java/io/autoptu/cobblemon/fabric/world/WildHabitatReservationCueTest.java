package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHabitatReservationCueTest {
    @Test
    void habitatPresenceIncludesOnlyInteractionActiveWildActors() {
        assertTrue(WildHabitatCueRuntime.contributesToHabitatCue(true));
        assertFalse(WildHabitatCueRuntime.contributesToHabitatCue(false));
    }
}
