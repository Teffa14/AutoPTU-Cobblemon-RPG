package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("3906892c11129c419e702d87ff71db071c12050f",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void keepsInitiativeVerifiedAndBroaderSystemsConservative() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
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

        String contracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).contracts();
        assertTrue(contracts.contains("BattleRuntimeState"));
        assertTrue(contracts.contains("RuntimeInitiativeOrderAssembly.fromState"));
        assertTrue(contracts.contains("InitiativeRoundRebuilder.authoritative"));
        assertTrue(contracts.contains("InitiativeAssemblyInstaller"));
        assertTrue(contracts.contains("BattleRoundController.advanceInitiativeTurnWithRollover()"));
        assertTrue(contracts.contains("default production path"));
        assertTrue(contracts.contains("injectable rebuilder overload is deprecated"));
        assertTrue(contracts.contains("BattleEnvironmentState"));
        assertTrue(contracts.contains("TrainerRuntimeState"));

        String limitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(limitation.contains("precomputed rollover order"));
        assertTrue(limitation.contains("InitiativeRoundRebuilder"));
        assertTrue(limitation.contains("alternative rollover strategy"));
        assertTrue(limitation.contains("Trainer ID"));
        assertFalse(limitation.isBlank());

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        assertTrue(lifecycleContracts.contains("default production rollover"));
        assertTrue(lifecycleContracts.contains("mixed Trainer/Pokemon initiative order"));
        assertTrue(lifecycleContracts.contains("temporary-effect cleanup"));
        assertTrue(lifecycleContracts.contains("without an adapter-supplied rebuilder"));

        String lifecycleLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycleLimitation.contains("Trainer-specific action-space"));
        assertTrue(lifecycleLimitation.contains("round-start Trainer AP"));
        assertTrue(lifecycleLimitation.contains("broader Python terrain/weather/round effects"));
        assertTrue(lifecycleLimitation.contains("default authoritative rollover path"));
        assertTrue(lifecycleLimitation.contains("injecting lifecycle strategy"));

        String trainerContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        assertTrue(trainerContracts.contains("TrainerRuntimeState"));
        assertTrue(trainerContracts.contains("explicit initiative Speed"));
        assertTrue(trainerContracts.contains("team identity"));
        assertTrue(trainerContracts.contains("Rider Agility Training"));
        assertTrue(trainerContracts.contains("Hardened Initiative"));

        String environmentLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).limitation();
        assertTrue(environmentLimitation.contains("not complete"));
        assertTrue(environmentLimitation.contains("entity passenger state"));
        assertTrue(environmentLimitation.contains("Trick Room"));
        assertTrue(environmentLimitation.contains("League semantics"));

        String adapterLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).limitation();
        assertTrue(adapterLimitation.contains("No Fabric/Cobblemon/Craftics runtime"));
    }
}
