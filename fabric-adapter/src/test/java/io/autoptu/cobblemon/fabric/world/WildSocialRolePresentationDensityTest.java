package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WildSocialRolePresentationDensityTest {
    @Test
    void markerDensityReflectsGatheredMembersWithoutUnboundedParticles() {
        assertEquals(2, WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(0));
        assertEquals(3, WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(1));
        assertEquals(6, WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(4));
        assertEquals(8, WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(6));
        assertEquals(8, WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(Integer.MAX_VALUE));
    }

    @Test
    void invalidGatheredMemberCountFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(-1));
    }
}
