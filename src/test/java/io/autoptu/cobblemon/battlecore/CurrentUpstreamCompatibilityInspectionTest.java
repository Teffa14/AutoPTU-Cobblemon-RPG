package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("4f16e07862008b8fb00ee405a9cbc160ae8fbcec", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("928c31a7b72243434536fdf05731ced421403f08", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void recordsMergedMultiTargetRuntimeWithoutPromotingTheWholeDamageCategory() {
        CurrentUpstreamCompatibilityInspection.Evidence damage = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(damage.contracts().contains("applyAreaUsingAuthoritativeCombatState"));
        assertTrue(damage.contracts().contains("sequentially"));
        assertTrue(damage.contracts().contains("exactly once"));
        assertTrue(damage.limitation().contains("does not establish complete stateful damage parity"));
        assertTrue(damage.limitation().contains("must not re-run"));
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
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).contracts();
        assertTrue(targeting.contains("RuntimeAreaMoveTargeting"));
        assertTrue(targeting.contains("TILE choices"));

        String abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        assertTrue(abilities.contains("PRE-damage reaction pipeline"));

        String perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        assertTrue(perks.contains("TrainerFeatureResourceResolution"));
        assertTrue(perks.contains("928c31a7"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("authoritative multi-target event projection"));
        assertTrue(adapter.limitation().contains("Minecraft must not execute PTU area rules"));
    }
}
