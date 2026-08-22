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
    void delayedHitExecutionRemainsBattleOwnedAndReevaluatesCurrentTargetGeometry() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        CurrentUpstreamCompatibilityInspection.Evidence targeting =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, targeting.support());
        assertTrue(lifecycle.contracts().contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycle.contracts().contains("RoundTemporaryEffectExpiryHook at order 30"));
        assertTrue(lifecycle.contracts().contains("TemporaryEffectStore.removeFirst"));
        assertTrue(lifecycle.limitation().contains("currentRound"));
        assertTrue(lifecycle.limitation().contains("queue/RNG mutation"));

        assertTrue(moves.contracts().contains("stale target-id anchors"));
        assertTrue(moves.contracts().contains("position-only delayed requests"));
        assertTrue(moves.contracts().contains("EffectiveMoveTargetResolver"));
        assertTrue(moves.contracts().contains("Stored target_position"));
        assertTrue(moves.contracts().contains("footprints"));
        assertTrue(moves.contracts().contains("line of sight"));
        assertTrue(moves.contracts().contains("HP eligibility"));
        assertTrue(moves.contracts().contains("live target IDs"));
        assertTrue(moves.limitation().contains("must not choose delayed targets"));
        assertTrue(moves.limitation().contains("supply RNG/combat inputs"));
        assertTrue(moves.limitation().contains("consume or refund move frequency/actions"));

        assertTrue(targeting.contracts().contains("hp <= 0"));
        assertTrue(targeting.contracts().contains("inactive positive-HP"));
        assertTrue(targeting.limitation().contains("generic active-state filter"));
    }

    @Test
    void runtimeAssemblyCannotInjectDelayedHitLifecycleResourcesCombatPreparationOrTargetGeometry() {
        Set<String> components = Arrays.stream(BattleRuntimeAssemblySeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("reservationId", "trainerPreparation", "canonicalState"), components);
        for (String forbidden : Set.of(
                "delayedHitExecutor", "delayedHitResolver", "delayedHitPolicy", "delayedHitResourcePolicy",
                "delayedHitMaturityDispatcher", "delayedHitRoundLifecycleHook", "delayedHitQueue",
                "delayedHitState", "delayedHitRng", "pythonRandom", "targetResolver", "delayedTargetResolver",
                "delayedTargetMode", "targetPositionForcesTile", "delayedTargetRewriter", "targetBindingResolver",
                "delayedTargetAnchor", "liveTargetPosition", "affectedTiles", "footprintOverlap",
                "lineOfSightResult", "effectiveTargets", "targetHpEligibility", "activeTargetFilter",
                "moveActionResolver", "frequencyConsumer", "frequencyRecorder", "moveUseRecorder",
                "actionConsumer", "actionMarker", "moveResolutionInput", "moveAc", "evasion",
                "accuracyStage", "attackValue", "defenseValue", "effectiveDb", "typeMultiplier",
                "damageModifiers", "postDamageHooks", "stabDamageBase", "staleTargetSelector",
                "replacementTargetSelector", "roundTemporaryEffectExpiry", "temporaryEffectExpiryHook",
                "followMeExpiry", "foresightExpiry", "expiryDecision", "currentRound")) {
            assertFalse(components.contains(forbidden), forbidden + " must remain AutoPTU-Java-owned");
        }
    }
}
