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
    void currentUpstreamOwnsDelayedQueueRngCombatantMaturityAndTargetBinding() {
        assertEquals("a2931ccc3dd37119a94445f44fb833c755d311c1",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);

        String lifecycleContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String moveContracts = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        String limitations = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();

        assertTrue(lifecycleContracts.contains("BattleDelayedHitState"));
        assertTrue(lifecycleContracts.contains("Python-compatible battle RNG stream"));
        assertTrue(lifecycleContracts.contains("DelayedHitLifecycleExecutor"));
        assertTrue(lifecycleContracts.contains("in insertion order"));
        assertTrue(moveContracts.contains("targetId remains COMBATANT targeting"));
        assertTrue(moveContracts.contains("aim anchor"));
        assertTrue(moveContracts.contains("position-only delayed entry resolves as TILE"));
        assertTrue(moveContracts.contains("without a second action/frequency spend"));
        assertTrue(limitations.contains("TILE/area delayed execution remains unsupported on main"));
        assertTrue(limitations.contains("automatic ROUND_START delayed-hit dispatch has not landed on main"));
        assertTrue(limitations.contains("rewrite target mode because a target position exists"));
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
