package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("dbc1bfb14c0e0036c1cc3301d35355d36611bf4b",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("8108e0d2b876414a5e62c2021801a3692cda05b8",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void recordsCurrentBoundariesWithoutPromotingBroaderSystems() {
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
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).contracts();
        String statusLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).limitation();
        assertTrue(statuses.contains("ordered stacked StatusEntry"));
        assertTrue(statuses.contains("repeated normalized names"));
        assertTrue(statusLimit.contains("representation/storage semantics only"));
        assertTrue(statusLimit.contains("Complete status ticking"));
        assertTrue(statusLimit.contains("must not interpret or execute"));

        String lifecycle = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String lifecycleLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycle.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycle.contains("Trainer action reset at order 40"));
        assertTrue(lifecycleLimit.contains("currentRound"));
        assertTrue(lifecycleLimit.contains("queue/RNG mutation"));

        String perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        String perksLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).limitation();
        assertTrue(perks.contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(perks.contains("heal/heal_active"));
        assertTrue(perks.contains("raise_cs"));
        assertTrue(perks.contains("grant_temp_hp"));
        assertTrue(perks.contains("grant_ap"));
        assertTrue(perks.contains("effect before resource consumption"));
        assertTrue(perksLimit.contains("wider Python effect library"));
        assertTrue(perksLimit.contains("Temporary HP damage absorption"));
        assertTrue(perksLimit.contains("must not grant Features"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("Cobblemon 1.7.3"));
        assertTrue(adapter.contracts().contains("PokemonEntity UUID lookup"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.contracts().contains("FileCanonicalPokemonRepository"));
        assertTrue(adapter.contracts().contains("ordered stacked-status metadata"));
        assertTrue(adapter.limitation().contains("successful authenticated graphical client encounter"));
        assertTrue(adapter.limitation().contains("Wiring durable Pokemon/item stores"));
        assertTrue(adapter.limitation().contains("RuntimeCombatantState materialization"));
        assertTrue(adapter.limitation().contains("never supply PTU stats"));
    }
}
