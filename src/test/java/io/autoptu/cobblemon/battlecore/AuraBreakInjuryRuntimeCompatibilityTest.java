package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraBreakInjuryRuntimeCompatibilityTest {
    @Test
    void currentAuraBreakContractDoesNotPromoteBroadCategories() {
        assertEquals("dc8cc6677dcfcf830fb176458b05ad08dba9b526", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);

        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(damage.contracts().contains("AuraBreakErrataAdjustment"));
        assertTrue(abilities.contracts().contains("AuraBreakErrataAdjustment"));
        assertTrue(damage.contracts().contains("AuraBreakBlockerQuery"));
        assertTrue(damage.contracts().contains("Aura Storm [Errata] is now live-wired"));
        assertTrue(abilities.contracts().contains("Aura Storm [Errata] is now live-wired"));
        assertTrue(damage.contracts().contains("signed post-damage adjustments"));
        assertTrue(abilities.contracts().contains("BattleRuntimeState.currentRound"));
        assertTrue(damage.adapterPolicy().contains("aura_break_errata"));
        assertTrue(abilities.adapterPolicy().contains("injury history rotation"));
        assertTrue(abilities.adapterPolicy().contains("remaining ability library"));
    }

    @Test
    void injuryRuntimePreparationComposesOnlyNonBlockingPreparationFeatures() {
        IntegrationFeatureCompatibility.Requirement injury = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_INJURY_BOOTSTRAP);
        IntegrationFeatureCompatibility.Requirement runtime = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_BATTLE_PREPARATION_ENVELOPE);
        IntegrationFeatureCompatibility.Requirement ruleState = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_RULE_STATE_SEED);

        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES,
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE),
                EnumSet.copyOf(injury.capabilities()));
        assertFalse(injury.hasBlockingDependency());
        assertFalse(runtime.hasBlockingDependency());
        assertFalse(ruleState.hasBlockingDependency());
        assertTrue(injury.boundedScope().contains("BattleRuntimeState.injuryHistory()/RoundInjuryHistoryState"));
        assertTrue(injury.boundedScope().contains("Previous/last-round history"));
        assertTrue(injury.boundedScope().contains("Minecraft/client payloads may not supply"));
        assertTrue(runtime.boundedScope().contains("must not construct RuntimeCombatantState"));
        assertTrue(ruleState.boundedScope().contains("battle round"));
        assertTrue(ruleState.boundedScope().contains("Aura Break blocker selection"));
    }
}
