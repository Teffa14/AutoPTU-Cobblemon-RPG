package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedHerdLeaderContextRuntimeTest {
    @Test
    void announcesInitialLeaderIdentityPopulationCohesionDistanceAndDirectionChanges() {
        UUID alphaOne = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID alphaTwo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var nearby = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", true, 5, "N");
        var sameBandFarther = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", true, 6, "N");
        var turned = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", true, 6, "NE");
        var distant = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", false, 14, "NE");
        var replacement = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaTwo, "Fletchling", false, 14, "NE");
        var otherPopulation = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.sendero.crossing", alphaTwo, "Fletchling", false, 14, "NE");

        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(null, nearby));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, nearby));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, sameBandFarther));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(sameBandFarther, turned));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(turned, distant));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(distant, replacement));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(replacement, otherPopulation));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, null));
    }

    @Test
    void textDistinguishesNearbyFromRegroupingAndReportsObservedBlocksAndDirection() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var nearby = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, "NE");
        var distant = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", false, 14, "W");

        assertEquals("Herd leader — Alpha Fletchling · nearby · 5 blocks · NE", WildFocusedHerdLeaderContextRuntime.contextText(nearby));
        assertEquals("Herd leader — Alpha Fletchling · regrouping distance · 14 blocks · W", WildFocusedHerdLeaderContextRuntime.contextText(distant));
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
    void leaderContextRejectsUnusableIdentityDistanceOrDirection() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext(" ", alpha, "Fletchling", true, 5, "N"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", null, "Fletchling", true, 5, "N"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, " ", true, 5, "N"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, -1, "N"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true, 5, " "));
    }
}
