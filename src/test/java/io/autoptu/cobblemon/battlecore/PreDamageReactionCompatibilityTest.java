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
        assertTrue(PreDamageReactionCompatibility.preDamagePipelineOrderingIsParityBacked());
        assertTrue(PreDamageReactionCompatibility.ordinaryMoveResolutionInvokesPreDamageReactions());
        assertFalse(PreDamageReactionCompatibility.minecraftMayExecutePreDamageReactionRules());
    }

    @Test
    void adapterMayOnlyRenderEventsThatAlreadyComeFromAuthoritativeCore() {
        assertTrue(PreDamageReactionCompatibility.minecraftMayRenderAuthoritativeReactionEvents());
        String policy = PreDamageReactionCompatibility.adapterPolicy();
        assertTrue(policy.contains("already emitted by AutoPTU-Java"));
        assertTrue(policy.contains("owns PRE-damage registry invocation"));
        assertTrue(policy.contains("derives threatened tiles from canonical BattleRuntimeState"));
        assertTrue(policy.contains("must not invoke the reaction registry"));
        assertTrue(policy.contains("construct or override threatened tiles"));
        assertTrue(policy.contains("cancel hit, damage or type effectiveness itself"));
        assertTrue(policy.contains("does not imply complete abilities"));
    }
}
