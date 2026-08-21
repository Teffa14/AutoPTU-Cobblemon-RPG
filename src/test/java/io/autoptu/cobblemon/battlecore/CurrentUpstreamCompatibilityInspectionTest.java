package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("3c82018e8f9f123500688d59cc94eba565593231",
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

        String contracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).contracts();
        assertTrue(contracts.contains("BattleRuntimeState"));
        assertTrue(contracts.contains("RuntimeInitiativeOrderAssembly.fromState"));
        assertTrue(contracts.contains("InitiativeRoundRebuilder.authoritative"));
        assertTrue(contracts.contains("BattleRoundController.advanceInitiativeTurnWithRollover()"));
        assertTrue(contracts.contains("DelayedHitResourcePolicy"));
        assertTrue(contracts.contains("BattleRuntime.applyDelayedAuthoritativeMove"));
        assertTrue(contracts.contains("action and frequency spend"));

        String limitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(limitation.contains("precomputed rollover order"));
        assertTrue(limitation.contains("InitiativeRoundRebuilder"));
        assertTrue(limitation.contains("delayed-hit action/frequency bookkeeping"));
        assertTrue(limitation.contains("Trainer ID"));
        assertFalse(limitation.isBlank());

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        assertTrue(lifecycleContracts.contains("default production rollover"));
        assertTrue(lifecycleContracts.contains("mixed Trainer/Pokemon initiative order"));
        assertTrue(lifecycleContracts.contains("DelayedHitExecutionPolicy"));
        assertTrue(lifecycleContracts.contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(lifecycleContracts.contains("COMBATANT-target"));
        assertTrue(lifecycleContracts.contains("evasion"));
        assertTrue(lifecycleContracts.contains("STAB"));
        assertTrue(lifecycleContracts.contains("type effectiveness"));
        assertTrue(lifecycleContracts.contains("damage modifiers"));
        assertTrue(lifecycleContracts.contains("post-damage hooks"));
        assertTrue(lifecycleContracts.contains("HP"));
        assertTrue(lifecycleContracts.contains("MoveResolvedEvent"));
        assertTrue(lifecycleContracts.contains("without spending action or move frequency again"));

        String lifecycleLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycleLimitation.contains("not yet connected delayed-hit maturity to ROUND_START"));
        assertTrue(lifecycleLimitation.contains("TILE target expansion"));
        assertTrue(lifecycleLimitation.contains("Trainer-specific action-space"));
        assertTrue(lifecycleLimitation.contains("round-start Trainer AP"));
        assertTrue(lifecycleLimitation.contains("broader Python terrain/weather/round effects"));
        assertTrue(lifecycleLimitation.contains("must not trigger delayed-hit maturity"));
        assertTrue(lifecycleLimitation.contains("legacy delayed-hit combat inputs"));

        String moveContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        assertTrue(moveContracts.contains("DelayedHitExecutionPolicy"));
        assertTrue(moveContracts.contains("DelayedHitResourcePolicy"));
        assertTrue(moveContracts.contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(moveContracts.contains("MoveResolutionInput"));
        assertTrue(moveContracts.contains("Forged legacy AC"));
        assertTrue(moveContracts.contains("type-effectiveness"));
        assertTrue(moveContracts.contains("PythonRandom"));
        assertTrue(moveContracts.contains("damage history"));

        String moveLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        assertTrue(moveLimitation.contains("bounded live delayed-hit execution"));
        assertTrue(moveLimitation.contains("ROUND_START scheduling/maturity dispatch"));
        assertTrue(moveLimitation.contains("TILE target expansion"));
        assertTrue(moveLimitation.contains("must not execute delayed hits"));
        assertTrue(moveLimitation.contains("consume or refund move frequency/actions"));
        assertTrue(moveLimitation.contains("MoveResolutionInput-style combat values"));

        String abilityContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        assertTrue(abilityContracts.contains("delayed-hit combat preparation"));
        assertTrue(abilityContracts.contains("authoritative move/damage/post-damage hooks"));

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
