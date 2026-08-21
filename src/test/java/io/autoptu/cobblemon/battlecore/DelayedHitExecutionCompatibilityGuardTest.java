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
    void delayedHitCombatantExecutionUsesBattleOwnedRoundStartLifecycleResourcesAndTargetBinding() {
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
        assertTrue(lifecycle.contracts().contains("DelayedHitRoundLifecycleHook"));
        assertTrue(lifecycle.contracts().contains("ROUND_START order 10"));
        assertTrue(lifecycle.contracts().contains("order 20"));
        assertTrue(lifecycle.contracts().contains("COMBATANT-target"));
        assertTrue(lifecycle.contracts().contains("originating action/frequency spend unchanged"));
        assertTrue(lifecycle.limitation().contains("TILE/area delayed hits remain unsupported"));
        assertTrue(lifecycle.limitation().contains("own the delayed queue or mutable RNG"));
        assertTrue(lifecycle.limitation().contains("mature delayed hits"));

        assertTrue(moves.contracts().contains("forwards both targetId and targetPosition unchanged"));
        assertTrue(moves.contracts().contains("targetId remains COMBATANT targeting"));
        assertTrue(moves.contracts().contains("aim anchor"));
        assertTrue(moves.contracts().contains("position-only delayed entry resolves as TILE"));
        assertTrue(moves.contracts().contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(moves.contracts().contains("PythonRandom"));
        assertTrue(moves.contracts().contains("HP mutation"));
        assertTrue(moves.contracts().contains("damage-history"));
        assertTrue(moves.contracts().contains("MoveResolvedEvent"));
        assertTrue(moves.contracts().contains("without a second action/frequency spend"));
        assertTrue(moves.limitation().contains("TILE/area delayed execution remains unsupported on main"));
        assertTrue(moves.limitation().contains("rewrite target mode because a target position exists"));
        assertTrue(moves.limitation().contains("replace a canonical targetId with TILE targeting"));
        assertTrue(moves.limitation().contains("supply RNG/combat inputs"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));
    }

    @Test
    void runtimeAssemblyCannotInjectDelayedHitLifecycleResourceCombatPreparationOrTargetInterpretation() {
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
                "delayedHitRoundLifecycleHook",
                "delayedHitQueue",
                "delayedHitState",
                "delayedHitRng",
                "pythonRandom",
                "targetResolver",
                "delayedTargetResolver",
                "delayedTargetMode",
                "targetPositionForcesTile",
                "delayedTargetRewriter",
                "targetBindingResolver",
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
