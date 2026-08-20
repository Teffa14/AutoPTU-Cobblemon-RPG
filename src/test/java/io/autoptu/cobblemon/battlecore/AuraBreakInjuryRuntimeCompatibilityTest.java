package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraBreakInjuryRuntimeCompatibilityTest {
    @Test
    void currentAuraBreakContractDoesNotPromoteBroadCategories() {
        assertEquals("9e1c918f33faa45c4c8832ba457cc36b875267c7", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);

        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(damage.contracts().contains("AuraBreakErrataAdjustment"));
        assertTrue(abilities.contracts().contains("AuraBreakErrataAdjustment"));
        assertTrue(damage.contracts().contains("still requires"));
        assertTrue(abilities.contracts().contains("still awaits"));
        assertTrue(damage.adapterPolicy().contains("aura_break_errata"));
        assertTrue(abilities.adapterPolicy().contains("temporary-effect cleanup"));
    }

    @Test
    void injuryRuntimePreparationComposesOnlyNonBlockingPreparationFeatures() {
        IntegrationFeatureCompatibility.Requirement injury = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP);
        IntegrationFeatureCompatibility.Requirement runtime = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_BATTLE_PREPARATION_ENVELOPE);

        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES),
                EnumSet.copyOf(injury.capabilities()));
        assertFalse(injury.hasBlockingDependency());
        assertFalse(runtime.hasBlockingDependency());
        assertTrue(injury.boundedScope().contains("future core runtime contracts"));
        assertTrue(runtime.boundedScope().contains("must not construct RuntimeCombatantState"));
    }
}
