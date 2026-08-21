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
    void delayedHitCombatantExecutionReDerivesCombatInputsButLifecycleRemainsPartialAndCoreOwned() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(lifecycle.contracts().contains("RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState"));
        assertTrue(lifecycle.contracts().contains("COMBATANT-target"));
        assertTrue(lifecycle.contracts().contains("evasion"));
        assertTrue(lifecycle.contracts().contains("accuracy stage"));
        assertTrue(lifecycle.contracts().contains("STAB"));
        assertTrue(lifecycle.contracts().contains("type effectiveness"));
        assertTrue(lifecycle.contracts().contains("damage modifiers"));
        assertTrue(lifecycle.contracts().contains("post-damage hooks"));
        assertTrue(lifecycle.contracts().contains("HP"));
        assertTrue(lifecycle.contracts().contains("damage-history"));
        assertTrue(lifecycle.contracts().contains("MoveResolvedEvent"));
        assertTrue(lifecycle.contracts().contains("without spending action or move frequency again"));
        assertTrue(lifecycle.limitation().contains("not yet connected delayed-hit maturity to ROUND_START"));
        assertTrue(lifecycle.limitation().contains("TILE target expansion"));
        assertTrue(lifecycle.limitation().contains("legacy delayed-hit combat inputs"));
        assertTrue(moves.contracts().contains("Forged legacy AC"));
        assertTrue(moves.contracts().contains("MoveResolutionInput"));
        assertTrue(moves.limitation().contains("Minecraft must not execute delayed hits"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));
        assertTrue(moves.limitation().contains("decide when they mature"));
        assertTrue(moves.limitation().contains("MoveResolutionInput-style combat values"));
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
