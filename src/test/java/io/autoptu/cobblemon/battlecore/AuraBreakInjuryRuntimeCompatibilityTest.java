package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraBreakInjuryRuntimeCompatibilityTest {
    @Test
    void currentAuraBreakContractDoesNotPromoteBroadCategories() {
        assertEquals("554b97e44fca9736f98704f8db3b1a661c63e93f", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("9df36aeae4bcbef49fd5edb658b51d68bd45fa71", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);

        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(damage.contracts().contains("AuraBreakErrataAdjustment"));
        assertTrue(abilities.contracts().contains("Aura Storm"));
        assertTrue(damage.contracts().contains("AuraBreakBlockerQuery"));
        assertTrue(damage.contracts().contains("Aura Storm [Errata] is live-wired"));
        assertTrue(damage.contracts().contains("signed post-damage adjustments"));
        assertTrue(damage.contracts().contains("currentRound"));
        assertTrue(damage.adapterPolicy().contains("aura_break_errata"));
        assertTrue(abilities.adapterPolicy().contains("PR #158"));
        assertTrue(abilities.adapterPolicy().contains("still draft"));
    }

    @Test
    void injuryRuntimePreparationComposesOnlyNonblockingPreparationFeatures() {
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
