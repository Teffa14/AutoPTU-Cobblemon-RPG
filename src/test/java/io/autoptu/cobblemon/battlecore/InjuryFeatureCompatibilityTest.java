package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjuryFeatureCompatibilityTest {
    @Test
    void injuryTransportUsesOnlyPartialDamageAndAbilityContracts() {
        for (IntegrationFeatureCompatibility.Feature feature : new IntegrationFeatureCompatibility.Feature[]{
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_SNAPSHOT,
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP
        }) {
            IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(feature);
            assertEquals(EnumSet.of(
                    UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                    UpstreamCompatibilityMatrix.Capability.ABILITIES), EnumSet.copyOf(requirement.capabilities()));
            assertFalse(requirement.hasBlockingDependency());
        }
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
    }

    @Test
    void injuryScopeKeepsAuraAndRestRulesOutOfMinecraft() {
        String snapshot = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_SNAPSHOT).boundedScope();
        String bootstrap = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP).boundedScope();
        assertTrue(snapshot.contains("Injury generation"));
        assertTrue(snapshot.contains("healing/rest"));
        assertTrue(snapshot.contains("Aura Storm scaling"));
        assertTrue(snapshot.contains("Aura Break"));
        assertTrue(bootstrap.contains("may not supply injury counts"));
        assertTrue(bootstrap.contains("Aura Storm/Aura Break"));
    }
}
