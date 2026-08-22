package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("63526aa2dce83d0faa22b364705cd36f590d964b",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("436b09ceb0811b74dc21924995aa82e56e581061",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void recordsVerifiedInfrastructureWithoutPromotingBroaderSystems() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).contracts();
        String targetingLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).limitation();
        assertTrue(targeting.contains("EffectiveMoveTargetResolver"));
        assertTrue(targeting.contains("hp <= 0"));
        assertTrue(targeting.contains("inactive positive-HP"));
        assertTrue(targetingLimit.contains("must not supply effective target lists"));
        assertTrue(targetingLimit.contains("generic active-state filter"));

        String lifecycle = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String lifecycleLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycle.contains("FieldRoundLifecycleHook at order 10"));
        assertTrue(lifecycle.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycle.contains("RoundTemporaryEffectExpiryHook at order 30"));
        assertTrue(lifecycle.contains("Trainer action reset at order 40"));
        assertTrue(lifecycle.contains("round-temporary-effect cleanup at order 45"));
        assertTrue(lifecycle.contains("DeclaredActionRoundLifecycleHook at order 50"));
        assertTrue(lifecycle.contains("DeclaredActionState"));
        assertTrue(lifecycle.contains("RoundTrainerFeatureLifecyclePolicy"));
        assertTrue(lifecycleLimit.contains("matches Python's relative cleanup ordering"));
        assertTrue(lifecycleLimit.contains("must not supply currentRound"));
        assertTrue(lifecycleLimit.contains("declared actions"));
        assertTrue(lifecycleLimit.contains("send-out decisions"));

        String actionEconomy = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).contracts();
        String actionLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(actionEconomy.contains("TrainerRuntimeState"));
        assertTrue(actionEconomy.contains("temporary AP grants"));
        assertTrue(actionLimit.contains("temporary AP grants/expiry"));
        assertTrue(actionLimit.contains("Trainer action-reset state"));

        String perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        String perksLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).limitation();
        assertTrue(perks.contains("TrainerFeaturePrerequisiteResolution"));
        assertTrue(perks.contains("TrainerFeatureContextResolution"));
        assertTrue(perks.contains("TrainerFeatureFrequencyResolution"));
        assertTrue(perks.contains("TrainerFeatureResourceResolution"));
        assertTrue(perks.contains("TrainerFeatureUsageResolution"));
        assertTrue(perks.contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(perks.contains("TrainerFeatureTargetResolution"));
        assertTrue(perks.contains("TrainerFeatureEffectRegistry"));
        assertTrue(perks.contains("heal/heal_active"));
        assertTrue(perks.contains("raise_cs"));
        assertTrue(perks.contains("grant_temp_hp"));
        assertTrue(perks.contains("only after the effect reports applied"));
        assertTrue(perksLimit.contains("AP-specific costs"));
        assertTrue(perksLimit.contains("wider Python effect library"));
        assertTrue(perksLimit.contains("Temporary HP damage absorption"));
        assertTrue(perksLimit.contains("must not grant Features"));
        assertTrue(perksLimit.contains("select or rewrite targets"));

        String legal = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).contracts();
        String legalLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).limitation();
        assertTrue(legal.contains("BattleChoice"));
        assertTrue(legal.contains("RuntimeAutobattlerActionSpace.legalChoices"));
        assertTrue(legal.contains("BattleRuntimeState"));
        assertTrue(legal.contains("move-frequency usage"));
        assertTrue(legal.contains("ActionBudget"));
        assertTrue(legal.contains("EffectiveMoveTargetResolver"));
        assertTrue(legalLimit.contains("current core-produced BattleChoice list"));
        assertTrue(legalLimit.contains("Tactical scoring remains separate"));

        String move = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        String moveLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        assertTrue(move.contains("stale target-id anchors"));
        assertTrue(move.contains("position-only delayed requests"));
        assertTrue(move.contains("EffectiveMoveTargetResolver"));
        assertTrue(move.contains("aim anchor"));
        assertTrue(move.contains("live target IDs"));
        assertTrue(moveLimit.contains("must not choose delayed targets"));
        assertTrue(moveLimit.contains("unported move specials"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("Cobblemon 1.7.3"));
        assertTrue(adapter.contracts().contains("PokemonEntity UUID lookup"));
        assertTrue(adapter.contracts().contains("positive HP mirroring"));
        assertTrue(adapter.contracts().contains("BATTLE_STARTED_PRE"));
        assertTrue(adapter.contracts().contains("canonical participant/combatant IDs"));
        assertTrue(adapter.contracts().contains("one server-issued reservation ID and RNG seed"));
        assertTrue(adapter.contracts().contains("FabricAuthenticatedPlayerContextResolver"));
        assertTrue(adapter.contracts().contains("MinecraftServer PlayerManager"));
        assertTrue(adapter.contracts().contains("FileVersionedCanonicalStateRepository"));
        assertTrue(adapter.contracts().contains("one CAS winner"));
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerStoreRuntime"));
        assertTrue(adapter.contracts().contains("Minecraft save root"));
        assertTrue(adapter.contracts().contains("CanonicalPlayerEncounterProfile"));
        assertTrue(adapter.contracts().contains("FileCanonicalPlayerEncounterProfileRepository"));
        assertTrue(adapter.contracts().contains("PersistentCanonicalPlayerEncounterContextSource"));
        assertTrue(adapter.contracts().contains("BattleAuthorityService still re-resolves every Pokemon and item"));
        assertTrue(adapter.limitation().contains("successful authenticated graphical client encounter is still pending"));
        assertTrue(adapter.limitation().contains("not independent Pokemon/item aggregate truth"));
        assertTrue(adapter.limitation().contains("RuntimeCombatantState materialization"));
        assertTrue(adapter.limitation().contains("identity/presentation inputs only"));
    }
}
