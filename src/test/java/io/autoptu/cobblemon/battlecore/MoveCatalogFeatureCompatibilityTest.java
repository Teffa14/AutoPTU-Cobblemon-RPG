package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveCatalogFeatureCompatibilityTest {
    @Test
    void trustedMoveCatalogHasNoBlockingUpstreamDependency() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTHORITATIVE_MOVE_CATALOG_PROJECTION);
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.capabilities().contains(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(requirement.capabilities().contains(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
        assertTrue(requirement.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertTrue(requirement.boundedScope().contains("Minecraft/client payloads may not supply"));
    }
}
