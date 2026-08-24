package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PreDamageReactionCompatibilityTest {
    @Test
    void authoritativeRuntimePromotionStillKeepsMinecraftProjectionOnly() {
        assertTrue(PreDamageReactionCompatibility.genericPreDamageReactionRegistryIsAvailable());
        assertTrue(PreDamageReactionCompatibility.authoritativeReactionEscapeMovementIsAvailable());
        assertTrue(PreDamageReactionCompatibility.authoritativeThreatenedAreaContextIsAvailable());
        assertTrue(PreDamageReactionCompatibility.telepathyHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.perceptionHookIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.perceptionReadyAndRoundScopedUsageAreCoreOwned());
        assertTrue(PreDamageReactionCompatibility.preDamagePipelineOrderingIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.ordinaryMoveResolutionInvokesPreDamageReactions());
        assertFalse(PreDamageReactionCompatibility.minecraftMayExecutePreDamageReactionRules());
    }

    @Test
    void perceptionStateAndDecisionsRemainAuthoritativeCoreResponsibilities() {
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("Perception readiness and round-scoped usage"));
        assertTrue(policy.contains("optional out-of-turn decisions"));
        assertTrue(policy.contains("must not invoke the registry"));
        assertTrue(policy.contains("consume perception_ready"));
        assertTrue(policy.contains("create perception_used"));
        assertTrue(policy.contains("choose escape destinations"));
        assertTrue(policy.contains("cancel hit, damage or type effectiveness itself"));
    }

    @Test
    void adapterMayOnlyRenderEventsThatAlreadyComeFromAuthoritativeCore() {
        assertTrue(PreDamageReactionCompatibility.minecraftMayRenderAuthoritativeReactionEvents());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("already emitted by AutoPTU-Java"));
        assertTrue(policy.contains("does not imply complete abilities"));
    }
}
