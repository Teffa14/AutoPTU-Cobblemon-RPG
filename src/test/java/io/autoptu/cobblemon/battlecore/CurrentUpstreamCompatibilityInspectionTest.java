package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("ce990c84ad133f9b0b56f774e2a59c8cb0c4d90b",
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
        assertTrue(initiativeContracts.contains("during ROUND_START"));

        String initiativeLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).limitation();
        assertTrue(initiativeLimitation.contains("precomputed rollover order"));
        assertTrue(initiativeLimitation.contains("delayed-hit RNG"));
        assertTrue(initiativeLimitation.contains("delayed-hit queue mutation"));
        assertTrue(initiativeLimitation.contains("Trainer ID"));
        assertFalse(initiativeLimitation.isBlank());

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        assertTrue(lifecycleContracts.contains("BattleDelayedHitState"));
        assertTrue(lifecycleContracts.contains("Python-compatible battle RNG stream"));
        assertTrue(lifecycleContracts.contains("FieldRoundLifecycleHook at ROUND_START order 10"));
        assertTrue(lifecycleContracts.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycleContracts.contains("terrain -> zones -> rooms"));
        assertTrue(lifecycleContracts.contains("DelayedHitLifecycleExecutor"));
        assertTrue(lifecycleContracts.contains("MoveResolvedEvent"));
        assertTrue(lifecycleContracts.contains("originating action/frequency spend unchanged"));
        assertTrue(lifecycleContracts.contains("damage-history rotation"));

        String lifecycleLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        assertTrue(lifecycleLimitation.contains("Complete lifecycle remains broader"));
        assertTrue(lifecycleLimitation.contains("TILE/area delayed hits"));
        assertTrue(lifecycleLimitation.contains("Trainer AP/temporary-AP"));
        assertTrue(lifecycleLimitation.contains("Air Lock"));
        assertTrue(lifecycleLimitation.contains("must not advance field durations"));
        assertTrue(lifecycleLimitation.contains("inject lifecycle hooks"));
        assertTrue(lifecycleLimitation.contains("mature delayed hits"));
        assertTrue(lifecycleLimitation.contains("duplicate action/frequency bookkeeping"));

        String moveContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        assertTrue(moveContracts.contains("DelayedHitExecutionPolicy"));
        assertTrue(moveContracts.contains("remains COMBATANT targeting at maturity"));
        assertTrue(moveContracts.contains("current authoritative RuntimeCombatantState.position"));
        assertTrue(moveContracts.contains("position-only delayed entry remains TILE targeting"));
        assertTrue(moveContracts.contains("recomputes affected_tiles"));
        assertTrue(moveContracts.contains("footprint overlap"));
        assertTrue(moveContracts.contains("line of sight"));
        assertTrue(moveContracts.contains("DelayedHitResourcePolicy"));
        assertTrue(moveContracts.contains("BattleRuntimeState owns BattleDelayedHitState"));
        assertTrue(moveContracts.contains("DelayedHitRoundLifecycleHook automatically during ROUND_START"));
        assertTrue(moveContracts.contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(moveContracts.contains("PythonRandom"));
        assertTrue(moveContracts.contains("damage-history"));
        assertTrue(moveContracts.contains("without a second action/frequency spend"));

        String moveLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        assertTrue(moveLimitation.contains("bounded delayed-hit execution"));
        assertTrue(moveLimitation.contains("TILE/area delayed execution remains unsupported on main"));
        assertTrue(moveLimitation.contains("known review defect"));
        assertTrue(moveLimitation.contains("freeze a live combatant target to the stored scheduling position"));
        assertTrue(moveLimitation.contains("precompute affected tiles"));
        assertTrue(moveLimitation.contains("supply RNG/combat inputs"));
        assertTrue(moveLimitation.contains("consume or refund move frequency/actions"));

        String fieldContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).contracts();
        assertTrue(fieldContracts.contains("FieldEffectEntry"));
        assertTrue(fieldContracts.contains("FieldRoundLifecycleHook"));
        assertTrue(fieldContracts.contains("FieldRoundProgression"));
        assertTrue(fieldContracts.contains("FieldEffectEndedEvent"));
        assertTrue(fieldContracts.contains("FieldStatusCleanupRequest"));
        assertTrue(fieldContracts.contains("authoritative environment"));
        assertTrue(fieldContracts.contains("before delayed-hit maturity"));

        String environmentLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).limitation();
        assertTrue(environmentLimitation.contains("partial field-system support"));
        assertTrue(environmentLimitation.contains("full terrain effects"));
        assertTrue(environmentLimitation.contains("forced movement"));
        assertTrue(environmentLimitation.contains("must not create PTU field entries"));
        assertTrue(environmentLimitation.contains("Wonder Room cleanup"));

        String abilityContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        assertTrue(abilityContracts.contains("delayed-hit combat preparation"));
        assertTrue(abilityContracts.contains("authoritative move/damage/post-damage hooks"));
        assertTrue(abilityContracts.contains("during ROUND_START"));

        String trainerContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        assertTrue(trainerContracts.contains("TrainerRuntimeState"));
        assertTrue(trainerContracts.contains("explicit initiative Speed"));
        assertTrue(trainerContracts.contains("team identity"));
        assertTrue(trainerContracts.contains("Rider Agility Training"));
        assertTrue(trainerContracts.contains("Hardened Initiative"));

        String adapterContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).contracts();
        assertTrue(adapterContracts.contains("field_effect"));
        assertTrue(adapterContracts.contains("delayed move_resolved playback"));
        assertTrue(adapterContracts.contains("stable event contract"));
        assertTrue(adapterContracts.contains("runtime environment seed"));

        String adapterLimitation = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).limitation();
        assertTrue(adapterLimitation.contains("No Fabric/Cobblemon/Craftics runtime"));
    }
}
