package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedHitPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void delayedCombatantResolutionUsesTheNormalAuthoritativeMovePlaybackContract() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-delayed", List.of(
                new BattleEventPlaybackEnvelope(
                        120,
                        "move_resolved",
                        "move_resolved|Delayed|actor|target|future-sight|true|false|31|69",
                        Map.of("damage", "1", "targetHp", "99", "maturity", "client")
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(2, presentation.commands().size());
        BattlePresentationCommand animation = presentation.commands().get(0);
        BattlePresentationCommand hp = presentation.commands().get(1);

        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, animation.kind());
        assertEquals("future-sight", animation.data().get("moveId"));
        assertEquals("actor", animation.subjectId());
        assertEquals("target", animation.data().get("targetId"));
        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, hp.kind());
        assertEquals("target", hp.subjectId());
        assertEquals("31", hp.data().get("damage"));
        assertEquals("69", hp.data().get("targetHp"));
    }

    @Test
    void currentUpstreamOwnsRoundStartDelayedMaturityQueueRngResourcesAndLiveTargetGeometry() {
        assertEquals("ce990c84ad133f9b0b56f774e2a59c8cb0c4d90b",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String lifecycleLimitations = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        String moveContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        String limitations = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();

        assertTrue(lifecycleContracts.contains("BattleDelayedHitState"));
        assertTrue(lifecycleContracts.contains("Python-compatible battle RNG stream"));
        assertTrue(lifecycleContracts.contains("FieldRoundLifecycleHook at ROUND_START order 10"));
        assertTrue(lifecycleContracts.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycleContracts.contains("terrain -> zones -> rooms"));
        assertTrue(lifecycleContracts.contains("MoveResolvedEvent"));
        assertTrue(lifecycleContracts.contains("originating action/frequency spend unchanged"));
        assertTrue(lifecycleContracts.contains("damage-history rotation"));
        assertTrue(lifecycleLimitations.contains("TILE/area delayed hits remain unsupported"));
        assertTrue(lifecycleLimitations.contains("must not"));
        assertTrue(lifecycleLimitations.contains("mature delayed hits"));

        assertTrue(moveContracts.contains("remains COMBATANT targeting at maturity"));
        assertTrue(moveContracts.contains("current authoritative RuntimeCombatantState.position"));
        assertTrue(moveContracts.contains("position-only delayed entry remains TILE targeting"));
        assertTrue(moveContracts.contains("recomputes affected_tiles"));
        assertTrue(moveContracts.contains("footprint overlap"));
        assertTrue(moveContracts.contains("line of sight"));
        assertTrue(moveContracts.contains("DelayedHitRoundLifecycleHook automatically during ROUND_START"));
        assertTrue(moveContracts.contains("without a second action/frequency spend"));
        assertTrue(limitations.contains("TILE/area delayed execution remains unsupported on main"));
        assertTrue(limitations.contains("known review defect"));
        assertTrue(limitations.contains("freeze a live combatant target to the stored scheduling position"));
        assertTrue(limitations.contains("consume or refund move frequency/actions"));
    }

    @Test
    void delayedPlaybackDoesNotPromoteLifecycleOrMoveLibraryToCompleteSupport() {
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
    }
}
