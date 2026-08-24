package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PreDamageReactionCompatibilityTest {
    @Test
    void inspectedUpstreamsMatchCurrentReadOnlyHeads() {
        assertEquals("ab29df99b0ac884805cb90d115818ad92c62a35d",
                PreDamageReactionCompatibility.INSPECTED_AUTOPTU_JAVA_SHA);
        assertEquals("65702f3816162c804a926c228d54d405f3236a97",
                PreDamageReactionCompatibility.INSPECTED_AUTOPTU_PYTHON_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                PreDamageReactionCompatibility.JAVA_PRE_DAMAGE_ORACLE_PIN_SHA);
    }

    @Test
    void authoritativeRuntimePromotionStillKeepsMinecraftProjectionOnly() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageReactionRegistryIsAvailable());
        assertTrue(PreDamageReactionCompatibility.genericPreDamageFollowUpMoveSeamIsAvailable());
        assertTrue(PreDamageReactionCompatibility.preDamageFollowUpExecutionPolicyIsFrozen());
        assertTrue(PreDamageReactionCompatibility.authoritativeReactionEscapeMovementIsAvailable());
        assertTrue(PreDamageReactionCompatibility.authoritativeAdjacentReactionPushPrimitiveIsAvailable());
        assertTrue(PreDamageReactionCompatibility.authoritativeThreatenedAreaContextIsAvailable());
        assertTrue(PreDamageReactionCompatibility.authoritativeEffectiveTargetKindIsAvailable());
        assertTrue(PreDamageReactionCompatibility.telepathyHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.perceptionHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.perceptionErrataHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.parryHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.swayOracleContractIsFrozen());
        assertTrue(PreDamageReactionCompatibility.swayHookIsParityBacked());
        assertFalse(PreDamageReactionCompatibility.swayLiveRuntimeFollowUpWiringIsAvailable());
        assertFalse(PreDamageReactionCompatibility.swayAuthoritativeExecutionIsAvailable());
        assertTrue(PreDamageReactionCompatibility.perceptionReadyAndRoundScopedUsageAreCoreOwned());
        assertTrue(PreDamageReactionCompatibility.parryReadyAndRoundScopedUsageAreCoreOwned());
        assertTrue(PreDamageReactionCompatibility.swayUsageGuardAndStandardSpendAreCoreOwned());
        assertTrue(PreDamageReactionCompatibility.preDamagePipelineOrderingIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.ordinaryMoveResolutionInvokesPreDamageReactions());
        assertFalse(PreDamageReactionCompatibility.minecraftMayExecutePreDamageReactionRules());
    }

    @Test
    void frozenFollowUpPolicyDoesNotPromoteUnmergedRuntimeWiring() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageFollowUpMoveSeamIsAvailable());
        assertTrue(PreDamageReactionCompatibility.preDamageFollowUpExecutionPolicyIsFrozen());
        assertTrue(PreDamageReactionCompatibility.swayHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.authoritativeAdjacentReactionPushPrimitiveIsAvailable());
        assertFalse(PreDamageReactionCompatibility.swayLiveRuntimeFollowUpWiringIsAvailable());
        assertFalse(PreDamageReactionCompatibility.swayAuthoritativeExecutionIsAvailable());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("frozen PRE-damage follow-up execution policy"));
        assertTrue(policy.contains("RuntimePreDamageReactionContextFactory"));
        assertTrue(policy.contains("without a live follow-up executor"));
        assertTrue(policy.contains("unmerged draft PR #179"));
        assertTrue(policy.contains("remains blocked end-to-end at the adapter boundary"));
        assertTrue(policy.contains("must not invoke the registry or follow-up executor"));
    }

    @Test
    void reactionStateTargetingAndDecisionsRemainAuthoritativeCoreResponsibilities() {
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("effective target kind"));
        assertTrue(policy.contains("Perception and Parry readiness/round-scoped usage"));
        assertTrue(policy.contains("optional out-of-turn decisions"));
        assertTrue(policy.contains("classify a move as melee/ranged/area"));
        assertTrue(policy.contains("sway_used or sway_redirect"));
        assertTrue(policy.contains("choose escape or push destinations"));
        assertTrue(policy.contains("recursively re-resolve a move"));
        assertTrue(policy.contains("spend STANDARD"));
        assertTrue(policy.contains("cancel hit, damage or type effectiveness itself"));
    }

    @Test
    void adapterMayOnlyRenderEventsThatAlreadyComeFromAuthoritativeCore() {
        assertTrue(PreDamageReactionCompatibility.minecraftMayRenderAuthoritativeReactionEvents());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("already emitted by AutoPTU-Java"));
        assertTrue(policy.contains("Perception, Perception [Errata], Parry and Telepathy"));
        assertTrue(policy.contains("does not promote Sway"));
        assertTrue(policy.contains("does not imply complete abilities"));
    }
}
