package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedHerdLeaderContextRuntimeTest {
    @Test
    void announcesInitialLeaderIdentityPopulationCohesionDistanceElevationDirectionHabitatAndVisibilityChanges() {
        UUID alphaOne = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID alphaTwo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var nearby = context("ouros.marea.lower_shelf", alphaOne, true, 5, 0, "N", "Marea Lower Shelf", true, true);
        var sameBandFarther = context("ouros.marea.lower_shelf", alphaOne, true, 6, 0, "N", "Marea Lower Shelf", true, true);
        var elevated = context("ouros.marea.lower_shelf", alphaOne, true, 6, 4, "N", "Marea Lower Shelf", true, true);
        var turned = context("ouros.marea.lower_shelf", alphaOne, true, 6, 4, "NE", "Marea Lower Shelf", true, true);
        var obscured = context("ouros.marea.lower_shelf", alphaOne, true, 6, 4, "NE", "Marea Lower Shelf", true, false);
        var distant = context("ouros.marea.lower_shelf", alphaOne, false, 14, 4, "NE", "Marea Lower Shelf", true, false);
        var migrated = context("ouros.marea.lower_shelf", alphaOne, false, 14, 4, "NE", "Marea Upper Shelf", false, false);
        var replacement = context("ouros.marea.lower_shelf", alphaTwo, false, 14, 4, "NE", "Marea Upper Shelf", false, false);
        var otherPopulation = context("ouros.sendero.crossing", alphaTwo, false, 14, 4, "NE", "Marea Upper Shelf", false, false);

        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(null, nearby));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, nearby));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, sameBandFarther));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(sameBandFarther, elevated));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(elevated, turned));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(turned, obscured));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(obscured, distant));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(distant, migrated));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(migrated, replacement));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(replacement, otherPopulation));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, null));
    }

    @Test
    void textReportsMinecraftElevationVisibilityAndLeaderHabitatWithoutImplyingBattleLineOfSight() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var nearbyVisible = context("ouros.marea.lower_shelf", alpha, true, 5, 4, "NE", "Marea Lower Shelf", true, true);
        var nearbyObscured = context("ouros.marea.lower_shelf", alpha, true, 5, -2, "NE", "Marea Lower Shelf", true, false);
        var acrossHabitat = context("ouros.marea.lower_shelf", alpha, false, 14, 0, "W", "Marea Upper Shelf", false, false);

        assertEquals("Herd leader — Alpha Fletchling · nearby · 5 blocks · NE · 4 blocks above · visible",
                WildFocusedHerdLeaderContextRuntime.contextText(nearbyVisible));
        assertEquals("Herd leader — Alpha Fletchling · nearby · 5 blocks · NE · 2 blocks below · obscured",
                WildFocusedHerdLeaderContextRuntime.contextText(nearbyObscured));
        assertEquals("Herd leader — Alpha Fletchling · regrouping distance · 14 blocks · W · same level · obscured · leader habitat Marea Upper Shelf",
                WildFocusedHerdLeaderContextRuntime.contextText(acrossHabitat));
    }

    @Test
    void roundsHorizontalMinecraftDistanceWithoutMixingVerticalSeparation() {
        assertEquals(0, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(0.0D));
        assertEquals(5, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(25.0D));
        assertEquals(6, WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(30.25D));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(-1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.roundedHorizontalDistanceBlocks(Double.NaN));
    }

    @Test
    void roundsAndDescribesMinecraftVerticalOffset() {
        assertEquals(0, WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(0.49D));
        assertEquals(1, WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(0.5D));
        assertEquals(-3, WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(-2.6D));
        assertEquals(Integer.MAX_VALUE, WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(Double.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(-Double.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> WildFocusedHerdLeaderContextRuntime.roundedVerticalOffsetBlocks(Double.NaN));

        assertEquals("same level", WildFocusedHerdLeaderContextRuntime.verticalRelationText(0));
        assertEquals("1 block above", WildFocusedHerdLeaderContextRuntime.verticalRelationText(1));
        assertEquals("3 blocks above", WildFocusedHerdLeaderContextRuntime.verticalRelationText(3));
        assertEquals("1 block below", WildFocusedHerdLeaderContextRuntime.verticalRelationText(-1));
        assertEquals("3 blocks below", WildFocusedHerdLeaderContextRuntime.verticalRelationText(-3));
        assertEquals("2147483648 blocks below",
                WildFocusedHerdLeaderContextRuntime.verticalRelationText(Integer.MIN_VALUE));
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
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext(" ", alpha, "Fletchling", true, 5, 0, "N", "Marea", true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", null, "Fletchling", true, 5, 0, "N", "Marea", true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, " ", true, 5, 0, "N", "Marea", true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, -1, 0, "N", "Marea", true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, 0, " ", "Marea", true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, 0, "N", " ", true, true));
    }

    private static WildFocusedHerdLeaderContextRuntime.LeaderContext context(
            String populationKey,
            UUID alpha,
            boolean withinCohesion,
            int distance,
            int verticalOffset,
            String direction,
            String leaderHabitat,
            boolean sameHabitat,
            boolean visible
    ) {
        return new WildFocusedHerdLeaderContextRuntime.LeaderContext(
                populationKey,
                alpha,
                "Fletchling",
                withinCohesion,
                distance,
                verticalOffset,
                direction,
                leaderHabitat,
                sameHabitat,
                visible);
    }
}
