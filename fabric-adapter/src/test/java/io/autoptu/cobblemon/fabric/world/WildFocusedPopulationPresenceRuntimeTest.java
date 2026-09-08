package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedPopulationPresenceRuntimeTest {
    @Test
    void announcesInitialFocusPopulationSwitchAndCountChanges() {
        var two = presence("ouros.marea.lower_shelf", 2);
        var three = presence("ouros.marea.lower_shelf", 3);
        var otherPopulation = presence("ouros.sendero.seasonal_crossing", 3);

        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(null, two));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, two));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, three));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, otherPopulation));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, null));
    }

    @Test
    void messageReportsOnlyServerProjectedPopulationPresence() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible",
                WildFocusedPopulationPresenceRuntime.presenceText(presence("ouros.marea.lower_shelf", 3)));
    }

    @Test
    void invalidPresenceFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("", "Fletchling", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "Fletchling", 0));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedPopulationPresenceRuntime.presenceText(null));
    }

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presence(String populationKey, int visibleActors) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(populationKey, "Fletchling", visibleActors);
    }
}
