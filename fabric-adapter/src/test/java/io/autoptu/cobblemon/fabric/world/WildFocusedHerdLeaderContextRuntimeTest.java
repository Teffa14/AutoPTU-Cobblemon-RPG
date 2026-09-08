package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WildFocusedHerdLeaderContextRuntimeTest {
    @Test
    void announcesInitialLeaderIdentityPopulationAndCohesionChanges() {
        UUID alphaOne = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID alphaTwo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var nearby = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", true);
        var distant = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaOne, "Fletchling", false);
        var replacement = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alphaTwo, "Fletchling", false);
        var otherPopulation = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.sendero.crossing", alphaTwo, "Fletchling", false);

        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(null, nearby));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, nearby));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, distant));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(distant, replacement));
        assertTrue(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(replacement, otherPopulation));
        assertFalse(WildFocusedHerdLeaderContextRuntime.shouldAnnounce(nearby, null));
    }

    @Test
    void textDistinguishesNearbyFromRegroupingDistance() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var nearby = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", true);
        var distant = new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, "Fletchling", false);

        assertEquals("Herd leader — Alpha Fletchling · nearby", WildFocusedHerdLeaderContextRuntime.contextText(nearby));
        assertEquals("Herd leader — Alpha Fletchling · regrouping distance", WildFocusedHerdLeaderContextRuntime.contextText(distant));
    }

    @Test
    void leaderContextRejectsUnusableIdentity() {
        UUID alpha = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext(" ", alpha, "Fletchling", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", null, "Fletchling", true));
        assertThrows(IllegalArgumentException.class,
                () -> new WildFocusedHerdLeaderContextRuntime.LeaderContext("ouros.marea.lower_shelf", alpha, " ", true));
    }
}
