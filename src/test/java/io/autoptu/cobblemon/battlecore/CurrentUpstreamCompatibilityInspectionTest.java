package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("b705561395b0ae776740e9207b44c1c53856f326",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void keepsInitiativeVerifiedButLifecycleAbilitiesAndLiveAdapterConservative() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String contracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).contracts();
        assertTrue(contracts.contains("RuntimeInitiativePokemonCandidateFactory"));
        assertTrue(contracts.contains("BattleRuntimeState"));
        assertTrue(contracts.contains("StatusStatResolution"));
        assertTrue(contracts.contains("PokemonInitiativeEntryResolution"));

        String limitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(limitation.contains("must never supply resolved Speed"));
        assertTrue(limitation.contains("RuntimeInitiativePokemonContext"));
        assertTrue(limitation.contains("server-owned"));
        assertFalse(limitation.isBlank());

        String adapterLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).limitation();
        assertTrue(adapterLimitation.contains("No Fabric/Cobblemon/Craftics runtime"));
    }
}
