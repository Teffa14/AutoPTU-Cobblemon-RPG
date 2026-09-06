package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildPresentationProfileTest {
    @Test
    void standardProfileCarriesNoSpecialCobblemonPresentation() {
        assertFalse(WildPresentationProfile.STANDARD.alphaVisual());
        assertEquals(WildPresentationProfile.HerdRole.NONE, WildPresentationProfile.STANDARD.herdRole());
    }

    @Test
    void alphaVisualAndHerdLeadershipAreIndependentServerAuthoredCapabilities() {
        var alphaLeader = WildPresentationProfile.alphaLeader();
        assertTrue(alphaLeader.alphaVisual());
        assertEquals(WildPresentationProfile.HerdRole.LEADER, alphaLeader.herdRole());

        var ordinaryLeader = new WildPresentationProfile(false, WildPresentationProfile.HerdRole.LEADER);
        assertFalse(ordinaryLeader.alphaVisual());
        assertEquals(WildPresentationProfile.HerdRole.LEADER, ordinaryLeader.herdRole());

        var alphaWithoutHerd = new WildPresentationProfile(true, WildPresentationProfile.HerdRole.NONE);
        assertTrue(alphaWithoutHerd.alphaVisual());
        assertEquals(WildPresentationProfile.HerdRole.NONE, alphaWithoutHerd.herdRole());
    }

    @Test
    void nullHerdRoleFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new WildPresentationProfile(false, null));
    }
}
