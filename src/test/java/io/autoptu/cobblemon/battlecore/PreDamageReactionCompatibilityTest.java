package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PreDamageReactionCompatibilityTest {
    @Test
    void authoritativeRuntimePromotionStillKeepsMinecraftProjectionOnly() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageReactionRegistryIsAvailable());
        assertTrue(PreDamageReactionCompatibility.genericPreDamageFollowUpMoveSeamIsAvailable());
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
    void mergedSwayPrimitivesDoNotUnlockOrdinaryRuntimeExecution() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageFollowUpMoveSeamIsAvailable());
        assertTrue(PreDamageReactionCompatibility.swayHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.authoritativeAdjacentReactionPushPrimitiveIsAvailable());
        assertFalse(PreDamageReactionCompatibility.swayLiveRuntimeFollowUpWiringIsAvailable());
        assertFalse(PreDamageReactionCompatibility.swayAuthoritativeExecutionIsAvailable());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("parity-backed Sway hook"));
        assertTrue(policy.contains("authoritative adjacent reaction push primitive"));
        assertTrue(policy.contains("RuntimePreDamageReactionContextFactory"));
        assertTrue(policy.contains("without a follow-up executor"));
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
        assertTrue(policy.contains("does not promote Sway to live executable adapter support"));
        assertTrue(policy.contains("does not imply complete abilities"));
    }
}
