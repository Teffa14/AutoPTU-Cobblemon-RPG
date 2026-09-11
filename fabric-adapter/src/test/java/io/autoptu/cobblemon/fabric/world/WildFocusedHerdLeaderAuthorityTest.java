package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildFocusedHerdLeaderAuthorityTest {
    @Test
    void focusedActorCountsAsLeaderOnlyFromAuthoredAlphaRoleAndHerdCapability() {
        assertTrue(WildFocusedHerdLeaderContextRuntime.isAuthoredHerdLeader(
                WildSocialRole.ALPHA,
                WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER));
        assertFalse(WildFocusedHerdLeaderContextRuntime.isAuthoredHerdLeader(
                WildSocialRole.MEMBER,
                WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER));
        assertFalse(WildFocusedHerdLeaderContextRuntime.isAuthoredHerdLeader(
                WildSocialRole.ALPHA,
                WildEcologyDescriptorRegistry.PresentationCapabilities.NONE));
        assertFalse(WildFocusedHerdLeaderContextRuntime.isAuthoredHerdLeader(
                null,
                WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER));
        assertFalse(WildFocusedHerdLeaderContextRuntime.isAuthoredHerdLeader(
                WildSocialRole.ALPHA,
                null));
    }

    @Test
    void distanceAnnouncementBandsSuppressPerBlockActionbarChurn() {
        assertEquals(0, WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(0));
        assertEquals(0, WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(3));
        assertEquals(1, WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(4));
        assertEquals(1, WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(7));
        assertEquals(2, WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(8));
        assertThrows(IllegalArgumentException.class, () -> WildFocusedHerdLeaderContextRuntime.distanceAnnouncementBand(-1));
    }
}
