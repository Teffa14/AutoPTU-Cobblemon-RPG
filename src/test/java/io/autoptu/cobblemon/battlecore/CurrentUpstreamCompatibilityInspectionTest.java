package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("fb91a65dc3bd92f49c7020ec856406df78bfc70a",
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

        String initiativeContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).contracts();
        assertTrue(initiativeContracts.contains("BattleRuntimeState"));
        assertTrue(initiativeContracts.contains("RuntimeInitiativeOrderAssembly.fromState"));
        assertTrue(initiativeContracts.contains("InitiativeRoundRebuilder.authoritative"));
        assertTrue(initiativeContracts.contains("DelayedHitResourcePolicy"));
        assertTrue(initiativeContracts.contains("action and frequency spend"));

        String initiativeLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(initiativeLimitation.contains("precomputed rollover order"));
        assertTrue(initiativeLimitation.contains("delayed-hit RNG"));
        assertTrue(initiativeLimitation.contains("delayed-hit queue mutation"));
        assertTrue(initiativeLimitation.contains("Trainer ID"));
        assertFalse(initiativeLimitation.isBlank());

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        assertTrue(lifecycleContracts.contains("BattleRuntimeState now owns BattleDelayedHitState"));
        assertTrue(lifecycleContracts.contains("Python-compatible battle RNG stream"));
        assertTrue(lifecycleContracts.contains("DelayedHitLifecycleExecutor"));
        assertTrue(lifecycleContracts.contains("COMBATANT-target"));
        assertTrue(lifecycleContracts.contains("in insertion order"));
        assertTrue(lifecycleContracts.contains("read-only delayed-hit snapshot"));

        String lifecycleLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycleLimitation.contains("does not register delayed-hit execution into ROUND_START"));
        assertTrue(lifecycleLimitation.contains("TILE/area delayed hits"));
        assertTrue(lifecycleLimitation.contains("terrain, zone, room"));
        assertTrue(lifecycleLimitation.contains("Trainer AP/temporary-AP"));
        assertTrue(lifecycleLimitation.contains("must not own the delayed queue"));
        assertTrue(lifecycleLimitation.contains("mutable RNG"));

        String moveContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        assertTrue(moveContracts.contains("DelayedHitExecutionPolicy"));
        assertTrue(moveContracts.contains("DelayedHitResourcePolicy"));
        assertTrue(moveContracts.contains("BattleRuntimeState owns BattleDelayedHitState"));
        assertTrue(moveContracts.contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(moveContracts.contains("PythonRandom"));
        assertTrue(moveContracts.contains("damage-history"));
        assertTrue(moveContracts.contains("without a second action/frequency spend"));

        String moveLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        assertTrue(moveLimitation.contains("bounded delayed-hit execution"));
        assertTrue(moveLimitation.contains("TILE/area delayed targets remain unsupported"));
        assertTrue(moveLimitation.contains("automatic ROUND_START dispatch has not landed"));
        assertTrue(moveLimitation.contains("supply RNG/combat inputs"));
        assertTrue(moveLimitation.contains("consume or refund move frequency/actions"));

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
        assertTrue(environmentLimitation.contains("League"));

        String adapterLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).limitation();
        assertTrue(adapterLimitation.contains("No Fabric/Cobblemon/Craftics runtime"));
    }
}
