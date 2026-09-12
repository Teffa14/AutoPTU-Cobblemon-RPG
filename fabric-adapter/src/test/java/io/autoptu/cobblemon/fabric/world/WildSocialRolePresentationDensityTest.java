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
    void markerSpreadExpandsWithGatheredMembersAndStaysBounded() {
        assertEquals(0.12D, WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(0), 0.000001D);
        assertEquals(0.15D, WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(1), 0.000001D);
        assertEquals(0.24D, WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(4), 0.000001D);
        assertEquals(0.30D, WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(6), 0.000001D);
        assertEquals(0.30D, WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(Integer.MAX_VALUE), 0.000001D);
    }

    @Test
    void markerVerticalSpreadExpandsWithGatheredMembersAndStaysBounded() {
        assertEquals(0.08D, WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(0), 0.000001D);
        assertEquals(0.10D, WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(1), 0.000001D);
        assertEquals(0.16D, WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(4), 0.000001D);
        assertEquals(0.20D, WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(6), 0.000001D);
        assertEquals(0.20D, WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(Integer.MAX_VALUE), 0.000001D);
    }

    @Test
    void markerHeightRisesWithGatheredMembersAndStaysBounded() {
        assertEquals(0.35D, WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(0), 0.000001D);
        assertEquals(0.40D, WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(1), 0.000001D);
        assertEquals(0.55D, WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(4), 0.000001D);
        assertEquals(0.65D, WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(6), 0.000001D);
        assertEquals(0.65D, WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(Integer.MAX_VALUE), 0.000001D);
    }

    @Test
    void invalidGatheredMemberCountFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.particleCountForHerdMemberCount(-1));
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.markerSpreadForHerdMemberCount(-1));
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.markerVerticalSpreadForHerdMemberCount(-1));
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.markerHeightForHerdMemberCount(-1));
    }
}
