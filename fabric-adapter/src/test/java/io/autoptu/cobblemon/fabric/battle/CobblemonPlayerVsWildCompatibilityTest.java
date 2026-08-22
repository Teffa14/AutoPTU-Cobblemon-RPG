package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.battlecore.IntegrationFeatureCompatibility;
import io.autoptu.cobblemon.battlecore.UpstreamCompatibilityMatrix;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CobblemonPlayerVsWildCompatibilityTest {
    @Test
    void prestartClaimUsesOnlyTheExistingPlayerVsWildAuthorityCapabilityBoundary() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_VS_WILD_AUTHORITY_COMPOSITION
        );

        assertEquals(Set.of(
                UpstreamCompatibilityMatrix.Capability.ITEMS,
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
        ), requirement.capabilities());
        assertFalse(requirement.hasBlockingDependency());
    }
}
