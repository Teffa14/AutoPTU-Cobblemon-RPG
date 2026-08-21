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
    void delayedHitCombatantExecutionIsLiveButLifecycleRemainsPartialAndCoreOwned() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(lifecycle.contracts().contains("BattleRuntime.applyDelayedAuthoritativeMove"));
        assertTrue(lifecycle.contracts().contains("COMBATANT-target"));
        assertTrue(lifecycle.contracts().contains("HP mutation"));
        assertTrue(lifecycle.contracts().contains("damage-history"));
        assertTrue(lifecycle.contracts().contains("MoveResolvedEvent"));
        assertTrue(lifecycle.contracts().contains("without spending action or move frequency again"));
        assertTrue(lifecycle.limitation().contains("not yet connected delayed-hit maturity to ROUND_START"));
        assertTrue(lifecycle.limitation().contains("TILE target expansion"));
        assertTrue(moves.limitation().contains("Minecraft must not execute delayed hits"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));
        assertTrue(moves.limitation().contains("decide when they mature"));
    }

    @Test
    void runtimeAssemblyCannotInjectDelayedHitExecutionOrResourceStrategy() {
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
                "actionMarker")) {
            assertFalse(components.contains(forbidden), forbidden + " must remain AutoPTU-Java-owned");
        }
    }
}
