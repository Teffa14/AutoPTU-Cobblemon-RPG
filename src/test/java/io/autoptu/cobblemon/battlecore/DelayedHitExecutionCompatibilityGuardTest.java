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
    void delayedHitCallChainAndResourceOwnershipRemainPartialAndCoreOwned() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(lifecycle.contracts().contains("DelayedHitExecutionPolicy"));
        assertTrue(lifecycle.contracts().contains("DelayedHitResourcePolicy"));
        assertTrue(lifecycle.contracts().contains("re-enters ordinary move-action resolution"));
        assertTrue(lifecycle.contracts().contains("spends no action"));
        assertTrue(lifecycle.contracts().contains("consumes no frequency"));
        assertTrue(lifecycle.contracts().contains("records no ordinary move use"));
        assertTrue(lifecycle.limitation().contains("not yet connected to ROUND_START"));
        assertTrue(moves.limitation().contains("Minecraft must not execute delayed hits"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));
        assertTrue(moves.limitation().contains("record ordinary move use at maturity"));
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
