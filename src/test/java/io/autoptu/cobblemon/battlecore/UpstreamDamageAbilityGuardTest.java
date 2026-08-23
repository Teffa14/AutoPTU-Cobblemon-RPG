package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamDamageAbilityGuardTest {
    @Test
    void postDamageAuraRuntimeBindingKeepsDamageAndAbilitiesPartial() {
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(abilities.contracts().contains("AbilityPhaseEffectRegistry"));
        assertTrue(abilities.contracts().contains("CombatStageHookRegistry"));
        assertTrue(abilities.contracts().contains("PostDamageHookRegistry"));
        assertTrue(abilities.contracts().contains("Aqua Boost"));
        assertTrue(abilities.contracts().contains("Power Spot"));
        assertTrue(abilities.contracts().contains("Type Aura"));
        assertTrue(abilities.contracts().contains("Aura Storm [Errata]"));
        assertTrue(damage.contracts().contains("PostDamageHookRegistry/PostDamageHookResult"));
        assertTrue(damage.contracts().contains("RuntimeMoveResolution"));
        assertTrue(damage.adapterPolicy().contains("independent HP mutation"));
        assertTrue(abilities.adapterPolicy().contains("independently alter damage/HP"));
    }

    @Test
    void rngAndAnalyticPostDamageAbilitiesRemainCoreOwned() {
        UpstreamCompatibilityMatrix.Entry actionEconomy = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, actionEconomy.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(actionEconomy.contracts().contains("InitiativeProgressState"));
        assertTrue(actionEconomy.contracts().contains("Analytic"));
        assertTrue(damage.contracts().contains("PythonRandom"));
        assertTrue(damage.contracts().contains("Adaptability [Errata] and Damp [Errata] are live-wired"));
        assertTrue(damage.contracts().contains("Analytic is live-wired"));
        assertTrue(abilities.contracts().contains("Adaptability [Errata] and Damp [Errata] are live post-damage abilities"));
        assertTrue(abilities.contracts().contains("Analytic is live-wired"));
        assertTrue(damage.adapterPolicy().contains("supply or advance the battle RNG"));
        assertTrue(damage.adapterPolicy().contains("decide Analytic eligibility"));
        assertTrue(damage.adapterPolicy().contains("add its +5 damage"));
        assertTrue(abilities.adapterPolicy().contains("independently alter damage/HP"));
    }

    @Test
    void initiativeRolloverAndTrainerStateRemainCoreOwned() {
        UpstreamCompatibilityMatrix.Entry initiative = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry legalActions = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE);

        assertTrue(initiative.contracts().contains("authoritative initiative assembly/installation"));
        assertTrue(initiative.contracts().contains("TrainerRuntimeState owns"));
        assertTrue(initiative.adapterPolicy().contains("choose the next actor"));
        assertTrue(initiative.adapterPolicy().contains("provide client-computed modifiers"));
        assertTrue(lifecycle.contracts().contains("InitiativeOrderAssembly/InitiativeAssemblyInstaller"));
        assertTrue(lifecycle.contracts().contains("RuntimeInitiativePokemonCandidateFactory"));
        assertTrue(lifecycle.adapterPolicy().contains("complete Python start_round parity is still absent"));
        assertTrue(legalActions.contracts().contains("initiative exhaustion"));
        assertTrue(legalActions.adapterPolicy().contains("must not supply initiative order"));
    }

    @Test
    void existingLiveAdapterPresentationBoundaryStaysGuarded() {
        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.contracts().contains("dedicated-server"));
        assertTrue(adapter.contracts().contains("PokemonEntity"));
        assertTrue(adapter.contracts().contains("authoritative relocation"));
        assertTrue(adapter.contracts().contains("positive HP projection"));
        assertTrue(adapter.contracts().contains("BATTLE_STARTED_PRE"));
        assertTrue(adapter.contracts().contains("CanonicalWildEncounterBlueprintSource"));
        assertTrue(adapter.contracts().contains("WorldScopedCanonicalWildEncounterBlueprintRegistry"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterIdentityBinder"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterProvisioningService"));
        assertTrue(adapter.adapterPolicy().contains("populate the world-scoped WILD blueprint registry"));
        assertTrue(adapter.adapterPolicy().contains("neither the registry, source contract nor provisioner derives species, level, HP, stats, moves, abilities"));
        assertTrue(adapter.adapterPolicy().contains("complete battle playback remain pending"));
    }
}
