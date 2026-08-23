package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("554b97e44fca9736f98704f8db3b1a661c63e93f", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("cd2d31ab9438713629ad3fc65939e8cc622b5a1f", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void recordsCurrentBoundariesWithoutPromotingBroaderSystems() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).contracts();
        String statusLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).limitation();
        assertTrue(statuses.contains("StatusAbilityPreventionResolver"));
        assertTrue(statuses.contains("Aroma Veil"));
        assertTrue(statuses.contains("status_block"));
        assertTrue(statusLimit.contains("complete status ticking"));
        assertTrue(statusLimit.contains("charge consumption/removal"));
        assertTrue(statusLimit.contains("must not evaluate radius"));

        String abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        String abilityLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).limitation();
        assertTrue(abilities.contains("Flower Veil"));
        assertTrue(abilities.contains("CombatStagePreventionHookRegistry"));
        assertTrue(abilities.contains("combat_stage_block"));
        assertTrue(abilityLimit.contains("Big Pecks"));
        assertTrue(abilityLimit.contains("Hyper Cutter"));
        assertTrue(abilityLimit.contains("Minecraft must not decide"));

        String perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        String perksLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).limitation();
        assertTrue(perks.contains("only after the effect reports applied"));
        assertTrue(perks.contains("grant_temp_hp"));
        assertTrue(perks.contains("grant_ap"));
        assertTrue(perks.contains("apply_status"));
        assertTrue(perks.contains("remove_status"));
        assertTrue(perks.contains("cd2d31ab"));
        assertTrue(perksLimit.contains("wider Python effect library"));
        assertTrue(perksLimit.contains("must not grant Features"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("world-scoped create-only registry"));
        assertTrue(adapter.contracts().contains("combat_stage_block"));
        assertTrue(adapter.limitation().contains("authenticated graphical client encounter"));
        assertTrue(adapter.limitation().contains("Concrete campaign/RPG generation policy"));
        assertTrue(adapter.limitation().contains("WILD registry remains lifecycle-scoped"));
        assertTrue(adapter.limitation().contains("RuntimeCombatantState materialization"));
        assertTrue(adapter.limitation().contains("Minecraft may present combat_stage_block only"));
    }
}
