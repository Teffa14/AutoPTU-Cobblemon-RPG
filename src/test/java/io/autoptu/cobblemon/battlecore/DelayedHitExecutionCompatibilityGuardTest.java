package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedHitExecutionCompatibilityGuardTest {
    @Test
    void delayedHitCombatantExecutionUsesBattleOwnedQueueAndRngButLifecycleRemainsPartial() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(lifecycle.contracts().contains("BattleDelayedHitState"));
        assertTrue(lifecycle.contracts().contains("Python-compatible battle RNG stream"));
        assertTrue(lifecycle.contracts().contains("DelayedHitLifecycleExecutor"));
        assertTrue(lifecycle.contracts().contains("COMBATANT-target"));
        assertTrue(lifecycle.contracts().contains("in insertion order"));
        assertTrue(lifecycle.limitation().contains("Delayed-hit execution is still not registered"));
        assertTrue(lifecycle.limitation().contains("TILE/area delayed hits"));
        assertTrue(lifecycle.limitation().contains("own the delayed queue or mutable RNG"));

        assertTrue(moves.contracts().contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(moves.contracts().contains("PythonRandom"));
        assertTrue(moves.contracts().contains("HP mutation"));
        assertTrue(moves.contracts().contains("damage-history"));
        assertTrue(moves.contracts().contains("MoveResolvedEvent"));
        assertTrue(moves.contracts().contains("without a second action/frequency spend"));
        assertTrue(moves.limitation().contains("automatic ROUND_START dispatch has not landed"));
        assertTrue(moves.limitation().contains("supply RNG/combat inputs"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));
    }

    @Test
    void runtimeAssemblyCannotInjectDelayedHitExecutionResourceOrCombatPreparation() {
        Set<String> components = Arrays.stream(BattleRuntimeAssemblySeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("reservationId", "trainerPreparation", "canonicalState"), components);
        for (String forbidden : Set.of(
                "delayedHitExecutor",
                "delayedHitResolver",
                "delayedHitPolicy",
                "delayedHitResourcePolicy",
                "delayedHitMaturityDispatcher",
                "delayedHitQueue",
                "delayedHitState",
                "delayedHitRng",
                "pythonRandom",
                "targetResolver",
                "moveActionResolver",
                "frequencyConsumer",
                "frequencyRecorder",
                "moveUseRecorder",
                "actionConsumer",
                "actionMarker",
                "moveResolutionInput",
                "moveAc",
                "evasion",
                "accuracyStage",
                "attackValue",
                "defenseValue",
                "effectiveDb",
                "typeMultiplier",
                "damageModifiers",
                "postDamageHooks",
                "stabDamageBase")) {
            assertFalse(components.contains(forbidden), forbidden + " must remain AutoPTU-Java-owned");
        }
    }
}
