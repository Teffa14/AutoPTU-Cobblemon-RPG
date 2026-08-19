package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerRuntimeFeatureCompatibilityTest {
    @Test
    void trainerRuntimeBootstrapConsumesOnlyNonBlockingUpstreamContracts() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_TRAINER_RUNTIME_BOOTSTRAP);

        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE),
                EnumSet.copyOf(requirement.capabilities()));
        assertFalse(requirement.hasBlockingDependency());

        String scope = requirement.boundedScope();
        assertTrue(scope.contains("Trainer AP"));
        assertTrue(scope.contains("TrainerRuntimeState"));
        assertTrue(scope.contains("bindController"));
        assertTrue(scope.contains("Minecraft"));
        assertTrue(scope.contains("spend/restore AP"));

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
    }
}
