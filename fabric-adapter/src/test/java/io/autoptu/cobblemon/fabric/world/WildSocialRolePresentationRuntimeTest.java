package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildSocialRolePresentationRuntimeTest {
    @Test
    void activeAuthoredAlphaProjectsNativeVisual() {
        assertTrue(WildSocialRolePresentationRuntime.shouldProjectNativeAlphaVisual(
                true,
                WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER));
    }

    @Test
    void reservationOrBattleHandoffClearsNativeAlphaVisual() {
        assertFalse(WildSocialRolePresentationRuntime.shouldProjectNativeAlphaVisual(
                false,
                WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER));
    }

    @Test
    void activeOrdinaryMemberNeverProjectsNativeAlphaVisual() {
        assertFalse(WildSocialRolePresentationRuntime.shouldProjectNativeAlphaVisual(
                true,
                WildEcologyDescriptorRegistry.PresentationCapabilities.NONE));
    }

    @Test
    void missingPresentationCapabilitiesFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildSocialRolePresentationRuntime.shouldProjectNativeAlphaVisual(true, null));
    }
}
