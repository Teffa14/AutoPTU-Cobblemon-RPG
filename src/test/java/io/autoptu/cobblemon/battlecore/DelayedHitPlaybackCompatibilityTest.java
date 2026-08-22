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
    void currentUpstreamOwnsDelayedMaturityResourcesTargetSelectionAndRoundExpiry() {
        assertEquals("66d82a5beb767ec8dd32803b5d08afaad3d454aa",
                CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);

        String lifecycle = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts();
        String lifecycleLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).limitation();
        String moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts();
        String limitations = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).limitation();
        String targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).contracts();

        assertTrue(lifecycle.contains("FieldRoundLifecycleHook at order 10"));
        assertTrue(lifecycle.contains("DelayedHitRoundLifecycleHook at order 20"));
        assertTrue(lifecycle.contains("RoundTemporaryEffectExpiryHook at order 30"));
        assertTrue(lifecycle.contains("Follow Me then Foresight"));
        assertTrue(lifecycleLimit.contains("expiry decisions"));
        assertTrue(moves.contains("stale target-id anchors"));
        assertTrue(moves.contains("position-only delayed requests"));
        assertTrue(moves.contains("EffectiveMoveTargetResolver"));
        assertTrue(moves.contains("footprints"));
        assertTrue(moves.contains("line of sight"));
        assertTrue(moves.contains("HP eligibility"));
        assertTrue(moves.contains("aim anchor"));
        assertTrue(targeting.contains("hp <= 0"));
        assertTrue(targeting.contains("inactive positive-HP"));
        assertTrue(limitations.contains("must not choose delayed targets"));
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
