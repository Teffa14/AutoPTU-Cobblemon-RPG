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
    void currentUpstreamOwnsDelayedMaturityResourcesAndTargetSelection() {
        assertEquals("f094111f248f3a6bfe78d835e8f4bce115f84ef7",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);

        String lifecycle = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        String limitations = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        String targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).contracts();

        assertTrue(lifecycle.contains("BattleDelayedHitState"));
        assertTrue(lifecycle.contains("FieldRoundLifecycleHook at ROUND_START order 10"));
        assertTrue(lifecycle.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycle.contains("originating action/frequency spend unchanged"));
        assertTrue(moves.contains("current authoritative RuntimeCombatantState.position"));
        assertTrue(moves.contains("falls back to the stored target position"));
        assertTrue(moves.contains("EffectiveMoveTargetResolver"));
        assertTrue(moves.contains("affected tiles"));
        assertTrue(moves.contains("footprint overlap"));
        assertTrue(moves.contains("line of sight"));
        assertTrue(moves.contains("HP eligibility"));
        assertTrue(moves.contains("originating action/frequency spend"));
        assertTrue(targeting.contains("hp <= 0"));
        assertTrue(targeting.contains("inactive positive-HP"));
        assertTrue(limitations.contains("TILE/area delayed execution remains unsupported on main"));
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
