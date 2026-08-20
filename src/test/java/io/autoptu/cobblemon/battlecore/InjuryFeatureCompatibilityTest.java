package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjuryFeatureCompatibilityTest {
    @Test
    void injuryTransportUsesOnlyPartialDamageAbilityAndLifecycleContracts() {
        IntegrationFeatureCompatibility.Requirement snapshot = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_SNAPSHOT);
        assertEquals(EnumSet.of(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                UpstreamCompatibilityMatrix.Capability.ABILITIES), EnumSet.copyOf(snapshot.capabilities()));
        assertFalse(snapshot.hasBlockingDependency());

        IntegrationFeatureCompatibility.Requirement bootstrap = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP);
        assertEquals(EnumSet.of(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                UpstreamCompatibilityMatrix.Capability.ABILITIES,
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE), EnumSet.copyOf(bootstrap.capabilities()));
        assertFalse(bootstrap.hasBlockingDependency());

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
    }

    @Test
    void injuryScopeKeepsAuraHistoryAndRestRulesOutOfMinecraft() {
        String snapshot = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_SNAPSHOT).boundedScope();
        String bootstrap = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP).boundedScope();
        assertTrue(snapshot.contains("Injury generation"));
        assertTrue(snapshot.contains("healing/rest"));
        assertTrue(snapshot.contains("Aura Storm scaling"));
        assertTrue(snapshot.contains("Aura Break"));
        assertTrue(bootstrap.contains("current counts only"));
        assertTrue(bootstrap.contains("Previous/last-round history"));
        assertTrue(bootstrap.contains("lifecycle rotation"));
        assertTrue(bootstrap.contains("Minecraft/client payloads may not supply"));
    }
}
