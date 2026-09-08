package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.ecology.MigrationPhase;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedPopulationPresenceRuntimeTest {
    @Test
    void announcesInitialFocusPopulationSwitchCountAuthoredAlphaAndMigrationChanges() {
        var two = presence("ouros.marea.lower_shelf", 2, 0);
        var three = presence("ouros.marea.lower_shelf", 3, 0);
        var alphaPresent = presence("ouros.marea.lower_shelf", 3, 1);
        var preparing = presence("ouros.marea.lower_shelf", 3, 1, MigrationPhase.PREPARING);
        var departing = presence("ouros.marea.lower_shelf", 3, 1, MigrationPhase.DEPARTING);
        var otherPopulation = presence("ouros.sendero.seasonal_crossing", 3, 0);

        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(null, two));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, two));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, three));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(three, alphaPresent));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(alphaPresent, three));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(alphaPresent, preparing));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(preparing, departing));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(preparing, preparing));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, otherPopulation));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, null));
    }

    @Test
    void messageReportsInitialServerProjectedPopulationPresence() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible",
                WildFocusedPopulationPresenceRuntime.presenceText(null, presence("ouros.marea.lower_shelf", 3, 0)));
    }

    @Test
    void messageReportsAuthoredAlphaPresenceWithoutInventingCombatSemantics() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible",
                WildFocusedPopulationPresenceRuntime.presenceText(null, presence("ouros.marea.lower_shelf", 3, 1)));
        assertEquals(
                "Wild population — Fletchling · 4 WILD visible · 2 Alphas visible",
                WildFocusedPopulationPresenceRuntime.presenceText(null, presence("ouros.marea.lower_shelf", 4, 2)));
    }

    @Test
    void messageReportsAuthoredMigrationPhaseFromFocusedEcologyContext() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible · Preparing",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        null,
                        presence("ouros.marea.lower_shelf", 3, 1, MigrationPhase.PREPARING)));
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Departing",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        presence("ouros.marea.lower_shelf", 3, 0, MigrationPhase.PREPARING),
                        presence("ouros.marea.lower_shelf", 3, 0, MigrationPhase.DEPARTING)));
    }

    @Test
    void messageReportsExactVisibleCountTransitionForSamePopulation() {
        assertEquals(
                "Wild population — Fletchling · 3 → 2 WILD visible",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        presence("ouros.marea.lower_shelf", 3, 0),
                        presence("ouros.marea.lower_shelf", 2, 0)));
        assertEquals(
                "Wild population — Fletchling · 2 → 4 WILD visible · Alpha visible",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        presence("ouros.marea.lower_shelf", 2, 0),
                        presence("ouros.marea.lower_shelf", 4, 1)));
    }

    @Test
    void populationSwitchDoesNotInventAContinuousCountTransition() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        presence("ouros.marea.lower_shelf", 2, 0),
                        presence("ouros.sendero.seasonal_crossing", 3, 1)));
    }

    @Test
    void invalidPresenceFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("", "Fletchling", 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "", 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "Fletchling", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "Fletchling", 1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence("ouros.marea", "Fletchling", 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedPopulationPresenceRuntime.presenceText(null, null));
    }

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presence(
            String populationKey,
            int visibleActors,
            int visibleAlphas
    ) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                populationKey,
                "Fletchling",
                visibleActors,
                visibleAlphas);
    }

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presence(
            String populationKey,
            int visibleActors,
            int visibleAlphas,
            MigrationPhase phase
    ) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                populationKey,
                "Fletchling",
                visibleActors,
                visibleAlphas,
                Optional.of(phase));
    }
}
