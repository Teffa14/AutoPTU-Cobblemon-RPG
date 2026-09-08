package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedHerdLeaderContextRuntimeTest {
    @Test
    void announcesInitialLeaderIdentityPopulationCohesionDistanceDirectionAndHabitatChanges() {
        UUID alphaOne = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID alphaTwo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var nearby = context("ouros.marea.lower_shelf", alphaOne, true, 5, "N", "Marea Lower Shelf", true);
        var sameBandFarther = context("ouros.marea.lower_shelf", alphaOne, true, 6, "N", "Marea Lower Shelf", true);
        var turned = context("ouros.marea.lower_shelf", alphaOne, true, 6, "NE", "Marea Lower Shelf", true);
        var distant = context("ouros.marea.lower_shelf", alphaOne, false, 14, "NE", "Marea Lower Shelf", true);
        var migrated = context("ouros.marea.lower_shelf", alphaOne, false, 14, "NE", "Marea Upper Shelf", false);
        var replacement = context("ouros.marea.lower_shelf", alphaTwo, false, 14, "NE", "Marea Upper Shelf", false);
        var otherPopulation = context("ouros.sendero.crossing", alphaTwo, false, 14, "NE", "Marea Upper Shelf", false);

        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(null, nearby));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, nearby));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, sameBandFarther));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(sameBandFarther, turned));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(turned, distant));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(distant, migrated));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(migrated, replacement));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(replacement, otherPopulation));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, null));
    }

    @Test
    void textReportsLeaderHabitatOnlyWhenLeaderProjectionIsInAnotherAuthoredHabitat() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var nearby = context("ouros.marea.lower_shelf", alpha, true, 5, "NE", "Marea Lower Shelf", true);
        var acrossHabitat = context("ouros.marea.lower_shelf", alpha, false, 14, "W", "Marea Upper Shelf", false);

        assertEquals("Herd leader — Alpha Fletchling · nearby · 5 blocks · NE",
                WildFocusedHerdLeaderContextRuntime.contextText(nearby));
        assertEquals("Herd leader — Alpha Fletchling · regrouping distance · 14 blocks · W · leader habitat Marea Upper Shelf",
                WildFocusedHerdLeaderContextRuntime.contextText(acrossHabitat));
    }

    @Test
    void roundsHorizontalMinecraftDistanceWithoutUsingVerticalSeparation() {
        assertEquals(0, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(0.0D));
        assertEquals(5, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(25.0D));
        assertEquals(6, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(30.25D));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(-1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(Double.NaN));
    }

    @Test
    void resolvesEightWayMinecraftCompassDirectionFromObservedHorizontalOffset() {
        assertEquals("N", WildFocusedHerdLeaderContextRuntime.compassDirection(0.0D, -5.0D));
        assertEquals("NE", WildFocusedHerdLeaderContextRuntime.compassDirection(5.0D, -5.0D));
        assertEquals("E", WildFocusedHerdLeaderContextRuntime.compassDirection(5.0D, 0.0D));
        assertEquals("SE", WildFocusedHerdLeaderContextRuntime.compassDirection(5.0D, 5.0D));
        assertEquals("S", WildFocusedHerdLeaderContextRuntime.compassDirection(0.0D, 5.0D));
        assertEquals("SW", WildFocusedHerdLeaderContextRuntime.compassDirection(-5.0D, 5.0D));
        assertEquals("W", WildFocusedHerdLeaderContextRuntime.compassDirection(-5.0D, 0.0D));
        assertEquals("NW", WildFocusedHerdLeaderContextRuntime.compassDirection(-5.0D, -5.0D));
        assertEquals("here", WildFocusedHerdLeaderContextRuntime.compassDirection(0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.compassDirection(Double.NaN, 0.0D));
    }

    @Test
    void leaderContextRejectsUnusableIdentityDistanceDirectionOrHabitat() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext(" ", alpha, "Fletchling", true, 5, "N", "Marea", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", null, "Fletchling", true, 5, "N", "Marea", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, " ", true, 5, "N", "Marea", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, -1, "N", "Marea", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, " ", "Marea", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, "N", " ", true));
    }

    private static WildFocusedHerdLeaderContextRuntime.LeaderContext context(
            String populationKey,
            UUID alpha,
            boolean withinCohesion,
            int distance,
            String direction,
            String leaderHabitat,
            boolean sameHabitat
    ) {
        return new WildFocusedHerdLeaderContextRuntime.LeaderContext(
                populationKey, alpha, "Fletchling", withinCohesion, distance, direction, leaderHabitat, sameHabitat);
    }
}
