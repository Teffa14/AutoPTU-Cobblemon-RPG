package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationFeatureCompatibilityTest {
    @Test
    void coversEveryIntegrationFeature() {
        assertEquals(
                EnumSet.allOf(IntegrationFeatureCompatibility.Feature.class),
                EnumSet.copyOf(IntegrationFeatureCompatibility.requirements().keySet())
        );
    }

    @Test
    void boundedCoreOwnedFeaturesHaveNoBlockingDependency() {
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.GRID_TARGET_PREVIEW).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.DAMAGE_RESULT_PLAYBACK).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ABILITY_EFFECT_PLAYBACK).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ITEM_BATTLE_EFFECT_PLAYBACK).hasBlockingDependency());
    }

    @Test
    void unsupportedRuleFamiliesRemainExplicitlyBlocked() {
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK).hasBlockingDependency());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTOBATTLER_TACTICAL_POLICY).hasBlockingDependency());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.LIVE_MINECRAFT_BATTLE_ADAPTER).hasBlockingDependency());
    }

    @Test
    void partialFamiliesRemainNarrowEvenWhenPlaybackIsAllowed() {
        assertEquals(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support()
        );
        assertEquals(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support()
        );
        assertEquals(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ITEMS).support()
        );
    }
}
