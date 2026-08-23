package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitiativeTurnStartPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void projectsAuthoritativeTurnStartCueWithoutTrustingPresentationAttributes() {
        BattlePresentationCommand command = projector.project(new BattleEventPlaybackEnvelope(
                12,
                "turn_start",
                "turn_start|3|actor|start|2",
                Map.of("actorId", "forged", "round", "999", "initiativeIndex", "99")
        )).getFirst();

        assertEquals(BattlePresentationCommand.Kind.TURN_START_CUE, command.kind());
        assertEquals("actor", command.subjectId());
        assertEquals("3", command.data().get("round"));
        assertEquals("start", command.data().get("phase"));
        assertEquals("2", command.data().get("initiativeIndex"));
    }

    @Test
    void preservesTurnStartThenStartEffectsThenStatusSkip() {
        BattlePlaybackBatch playback = new BattlePlaybackBatch(
                "reservation-turn-start",
                List.of(
                        new BattleEventPlaybackEnvelope(
                                1,
                                "turn_start",
                                "turn_start|3|actor|start|0",
                                Map.of("actorId", "forged")
                        ),
                        new BattleEventPlaybackEnvelope(
                                2,
                                "rule_effect",
                                "rule_effect|status|flinch|actor|||start|0.0|0",
                                Map.of("effect", "forged")
                        ),
                        new BattleEventPlaybackEnvelope(
                                3,
                                "status_skip",
                                "status_skip|actor|Flinch|standard|start",
                                Map.of("status", "forged")
                        )
                )
        );

        BattlePresentationBatch presentation = projector.project(playback);
        assertEquals(List.of(
                        BattlePresentationCommand.Kind.TURN_START_CUE,
                        BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                        BattlePresentationCommand.Kind.STATUS_SKIP_CUE),
                presentation.commands().stream().map(BattlePresentationCommand::kind).toList());
        assertEquals(List.of(1L, 2L, 3L), presentation.commands().stream()
                .map(BattlePresentationCommand::sequence).toList());
    }

    @Test
    void rejectsMalformedTurnStartInsteadOfInventingInitiativeState() {
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "turn_start", "turn_start|3|actor|start|-1", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "turn_start", "turn_start|3||start|0", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "turn_start", "turn_start|3|actor|start", Map.of()
        )));
    }

    @Test
    void compatibilityKeepsTurnSelectionAndStartEffectsCoreOwnedWhileLifecycleStaysPartial() {
        UpstreamCompatibilityMatrix.Entry initiative = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE
        );
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE
        );
        UpstreamCompatibilityMatrix.Entry legalActions = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE
        );

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, initiative.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, legalActions.support());
        assertTrue(initiative.contracts().contains("BattleRoundController"));
        assertTrue(initiative.contracts().contains("START processing"));
        assertTrue(initiative.adapterPolicy().contains("choose the next actor"));
        assertTrue(initiative.adapterPolicy().contains("provide client-computed modifiers"));
        assertTrue(initiative.adapterPolicy().contains("execute START hooks"));
        assertTrue(lifecycle.contracts().contains("pending status-skip consumption"));
        assertTrue(lifecycle.adapterPolicy().contains("complete Python start_round parity is still absent"));
        assertTrue(legalActions.contracts().contains("before the next decision window"));
        assertTrue(legalActions.adapterPolicy().contains("must not supply initiative order"));
        assertEquals("5d9e5069fa0c68432825a48be25fff6ba245d305", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
    }
}
