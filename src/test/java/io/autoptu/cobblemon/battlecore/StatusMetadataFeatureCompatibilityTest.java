package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusMetadataFeatureCompatibilityTest {
    @Test
    void statusMetadataTransportConsumesPartialStatusAndLifecycleContractsOnly() {
        IntegrationFeatureCompatibility.Requirement snapshot = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_STATUS_METADATA_SNAPSHOT);
        IntegrationFeatureCompatibility.Requirement bootstrap = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_STATUS_METADATA_BOOTSTRAP);

        assertFalse(snapshot.hasBlockingDependency());
        assertFalse(bootstrap.hasBlockingDependency());
        assertTrue(snapshot.capabilities().contains(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE));
        assertTrue(snapshot.capabilities().contains(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertTrue(bootstrap.boundedScope().contains("StatusEntry/StatusStateStore"));
        assertTrue(bootstrap.boundedScope().contains("without executing"));
    }
}
