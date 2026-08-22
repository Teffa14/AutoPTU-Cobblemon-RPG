package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("fa307e722c4912b50a4d1e59b7b6a98fc29a55cc",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7",
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
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
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
        assertTrue(perks.contains("RoundTrainerFeatureLifecyclePolicy"));
        assertTrue(perks.contains("TrainerFeaturePrerequisiteResolution"));
        assertTrue(perks.contains("class, subclass, level"));
        assertTrue(perksLimit.contains("must not select eligible Trainer Features"));
        assertTrue(perksLimit.contains("decide prerequisites"));
        assertTrue(perksLimit.contains("dispatch round_start Features"));

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
        assertTrue(adapter.contracts().contains("dedicated-server ModInitializer"));
        assertTrue(adapter.contracts().contains("Cobblemon 1.7.3"));
        assertTrue(adapter.contracts().contains("presentation UUID lookup"));
        assertTrue(adapter.limitation().contains("entity-bound relocation"));
        assertTrue(adapter.limitation().contains("HP projection"));
    }
}
