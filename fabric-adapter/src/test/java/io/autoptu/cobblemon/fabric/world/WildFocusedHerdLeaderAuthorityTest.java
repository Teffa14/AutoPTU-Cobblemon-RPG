package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
