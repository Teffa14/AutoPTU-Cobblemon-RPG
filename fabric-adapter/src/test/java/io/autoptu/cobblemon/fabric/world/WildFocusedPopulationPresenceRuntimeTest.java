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
    void announcesInitialFocusPopulationSwitchCountAuthoredAlphaMigrationHabitatSpreadAndFocusedRoleChanges() {
        var two = presence("ouros.marea.lower_shelf", 2, 0);
        var three = presence("ouros.marea.lower_shelf", 3, 0);
        var alphaPresent = presence("ouros.marea.lower_shelf", 3, 1);
        var preparing = presence("ouros.marea.lower_shelf", 3, 1, MigrationPhase.PREPARING);
        var departing = presence("ouros.marea.lower_shelf", 3, 1, MigrationPhase.DEPARTING);
        var lowerShelf = presenceAt("ouros.marea.lower_shelf", 3, 1, MigrationPhase.DEPARTING, "Marea Lower Shelf");
        var upperShelf = presenceAt("ouros.marea.lower_shelf", 3, 1, MigrationPhase.DEPARTING, "Marea Upper Shelf");
        var cohesive = presenceWithSpread("ouros.marea.lower_shelf", 3, 1,
                WildFocusedPopulationPresenceRuntime.GroupSpread.COHESIVE);
        var dispersed = presenceWithSpread("ouros.marea.lower_shelf", 3, 1,
                WildFocusedPopulationPresenceRuntime.GroupSpread.DISPERSED);
        var focusedMember = presenceWithRole("ouros.marea.lower_shelf", 3, 1, WildSocialRole.MEMBER);
        var focusedAlpha = presenceWithRole("ouros.marea.lower_shelf", 3, 1, WildSocialRole.ALPHA);
        var otherPopulation = presence("ouros.sendero.seasonal_crossing", 3, 0);

        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(null, two));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, two));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, three));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(three, alphaPresent));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(alphaPresent, three));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(alphaPresent, preparing));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(preparing, departing));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(preparing, preparing));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(lowerShelf, upperShelf));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(lowerShelf, lowerShelf));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(cohesive, dispersed));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(cohesive, cohesive));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(focusedMember, focusedAlpha));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(focusedAlpha, focusedAlpha));
        assertTrue(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, otherPopulation));
        assertFalse(WildFocusedPopulationPresenceRuntime.shouldAnnounce(two, null));
    }

    @Test
    void classifiesGroupSpreadFromAuthoredPresentationThresholds() {
        WildBehaviorProfile profile = new WildBehaviorProfile(12.0D, 5.0D, 2, 3, 60L, 20L, 0.01D, 6.0D, 30.0F);

        assertEquals(WildFocusedPopulationPresenceRuntime.GroupSpread.ALONE,
                WildFocusedPopulationPresenceRuntime.classifyGroupSpread(1, 0.0D, profile));
        assertEquals(WildFocusedPopulationPresenceRuntime.GroupSpread.CLUSTERED,
                WildFocusedPopulationPresenceRuntime.classifyGroupSpread(2, profile.separationDistance(), profile));
        assertEquals(WildFocusedPopulationPresenceRuntime.GroupSpread.COHESIVE,
                WildFocusedPopulationPresenceRuntime.classifyGroupSpread(2, profile.separationDistance() + 0.1D, profile));
        assertEquals(WildFocusedPopulationPresenceRuntime.GroupSpread.COHESIVE,
                WildFocusedPopulationPresenceRuntime.classifyGroupSpread(3, profile.cohesionDistance(), profile));
        assertEquals(WildFocusedPopulationPresenceRuntime.GroupSpread.DISPERSED,
                WildFocusedPopulationPresenceRuntime.classifyGroupSpread(3, profile.cohesionDistance() + 0.1D, profile));
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
    void messageIdentifiesWhenTheFocusedCanonicalActorIsTheAuthoredAlpha() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible · focused Alpha",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        null,
                        presenceWithRole("ouros.marea.lower_shelf", 3, 1, WildSocialRole.ALPHA)));
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        null,
                        presenceWithRole("ouros.marea.lower_shelf", 3, 1, WildSocialRole.MEMBER)));
    }

    @Test
    void messageReportsObservedGroupSpreadAsMinecraftPresentationContext() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible · cohesive",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        null,
                        presenceWithSpread("ouros.marea.lower_shelf", 3, 1,
                                WildFocusedPopulationPresenceRuntime.GroupSpread.COHESIVE)));
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · spread out",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        presenceWithSpread("ouros.marea.lower_shelf", 3, 0,
                                WildFocusedPopulationPresenceRuntime.GroupSpread.COHESIVE),
                        presenceWithSpread("ouros.marea.lower_shelf", 3, 0,
                                WildFocusedPopulationPresenceRuntime.GroupSpread.DISPERSED)));
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
    void messageReportsProjectedHabitatAlongsideAuthoredMigrationPhase() {
        assertEquals(
                "Wild population — Fletchling · 3 WILD visible · Alpha visible · habitat Marea Upper Shelf · Departing",
                WildFocusedPopulationPresenceRuntime.presenceText(
                        null,
                        presenceAt(
                                "ouros.marea.lower_shelf",
                                3,
                                1,
                                MigrationPhase.DEPARTING,
                                "Marea Upper Shelf")));
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
        WildBehaviorProfile profile = new WildBehaviorProfile(12.0D, 5.0D, 2, 3, 60L, 20L, 0.01D, 6.0D, 30.0F);
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
                () -> new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                        "ouros.marea", "Fletchling", 1, 0, Optional.empty(), Optional.of("   ")));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedPopulationPresenceRuntime.classifyGroupSpread(0, 0.0D, profile));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedPopulationPresenceRuntime.classifyGroupSpread(1, -1.0D, profile));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedPopulationPresenceRuntime.classifyGroupSpread(1, 0.0D, null));
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

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presenceAt(
            String populationKey,
            int visibleActors,
            int visibleAlphas,
            MigrationPhase phase,
            String habitatDisplayName
    ) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                populationKey,
                "Fletchling",
                visibleActors,
                visibleAlphas,
                Optional.of(phase),
                Optional.of(habitatDisplayName));
    }

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presenceWithSpread(
            String populationKey,
            int visibleActors,
            int visibleAlphas,
            WildFocusedPopulationPresenceRuntime.GroupSpread spread
    ) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                populationKey,
                "Fletchling",
                visibleActors,
                visibleAlphas,
                Optional.empty(),
                Optional.empty(),
                Optional.of(spread));
    }

    private static WildFocusedPopulationPresenceRuntime.PopulationPresence presenceWithRole(
            String populationKey,
            int visibleActors,
            int visibleAlphas,
            WildSocialRole role
    ) {
        return new WildFocusedPopulationPresenceRuntime.PopulationPresence(
                populationKey,
                "Fletchling",
                visibleActors,
                visibleAlphas,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(role));
    }
}
