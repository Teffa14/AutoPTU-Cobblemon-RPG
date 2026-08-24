package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PreDamageReactionCompatibilityTest {
    @Test
    void inspectedUpstreamsMatchCurrentReadOnlyHeads() {
        assertEquals("ab520743d8d99f06fa28fd4d6fa06a0c4ecd3fee",
                PreDamageReactionCompatibility.INSPECTED_AUTOPTU_JAVA_SHA);
        assertEquals("03321a2eba42437180fddf5c4b2570c50ba429a6",
                PreDamageReactionCompatibility.INSPECTED_AUTOPTU_PYTHON_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                PreDamageReactionCompatibility.JAVA_PRE_DAMAGE_ORACLE_PIN_SHA);
    }

    @Test
    void mergedFollowUpRuntimePromotesSwayWhileMinecraftRemainsProjectionOnly() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageReactionRegistryIsAvailable());
        assertTrue(PreDamageReactionCompatibility.genericPreDamageFollowUpMoveSeamIsAvailable());
        assertTrue(PreDamageReactionCompatibility.preDamageFollowUpExecutionPolicyIsFrozen());
        assertTrue(PreDamageReactionCompatibility.authoritativeAdjacentReactionPushPrimitiveIsAvailable());
        assertTrue(PreDamageReactionCompatibility.swayOracleContractIsFrozen());
        assertTrue(PreDamageReactionCompatibility.swayHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.swayLiveRuntimeFollowUpWiringIsAvailable());
        assertTrue(PreDamageReactionCompatibility.swayAuthoritativeExecutionIsAvailable());
        assertTrue(PreDamageReactionCompatibility.swayUsageGuardAndStandardSpendAreCoreOwned());
        assertFalse(PreDamageReactionCompatibility.minecraftMayExecutePreDamageReactionRules());
    }

    @Test
    void newlyMergedShellShieldRemainsGenericCoreOwnedReactionBehavior() {
        assertTrue(PreDamageReactionCompatibility.shellShieldHookIsParityBacked());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("parity-backed Shell Shield"));
        assertTrue(policy.contains("same generic PRE-damage registry"));
        assertTrue(policy.contains("must not invoke the reaction registry or follow-up executor"));
        assertTrue(policy.contains("evaluate Sway or Shell Shield eligibility"));
        assertTrue(policy.contains("mutate combat stages/status/HP"));
    }

    @Test
    void reactionStateTargetingAndNestedExecutionRemainAuthoritativeCoreResponsibilities() {
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("effective target kind"));
        assertTrue(policy.contains("readiness and usage bookkeeping"));
        assertTrue(policy.contains("optional out-of-turn decisions"));
        assertTrue(policy.contains("nested follow-up move execution"));
        assertTrue(policy.contains("RuntimeMoveResolutionWithFollowUps"));
        assertTrue(policy.contains("STANDARD spend"));
        assertTrue(policy.contains("sway_used and sway_redirect"));
        assertTrue(policy.contains("authoritative adjacent push selection"));
        assertTrue(policy.contains("recursion protection"));
        assertTrue(policy.contains("original-hit cancellation"));
    }

    @Test
    void adapterMayOnlyRenderEventsThatAlreadyComeFromAuthoritativeCore() {
        assertTrue(PreDamageReactionCompatibility.minecraftMayRenderAuthoritativeReactionEvents());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("already produced by AutoPTU-Java"));
        assertTrue(policy.contains("promotes Sway end-to-end authoritative execution availability only"));
        assertTrue(policy.contains("does not imply complete abilities"));
    }
}
