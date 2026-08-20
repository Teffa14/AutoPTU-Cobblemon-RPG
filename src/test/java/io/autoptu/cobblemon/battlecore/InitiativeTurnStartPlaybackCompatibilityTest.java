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
    void rendersAuthoritativeTurnStartWithoutTrustingPresentationAttributes() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                41,
                "turn_start",
                "turn_start|3|pokemon-alpha|start|4",
                Map.of(
                        "round", "999",
                        "actorId", "forged",
                        "phase", "end",
                        "initiativeIndex", "0"
                )
        );

        BattlePresentationCommand command = projector.project(event).getFirst();

        assertEquals(BattlePresentationCommand.Kind.TURN_START_CUE, command.kind());
        assertEquals(41, command.sequence());
        assertEquals("pokemon-alpha", command.subjectId());
        assertEquals("3", command.data().get("round"));
        assertEquals("start", command.data().get("phase"));
        assertEquals("4", command.data().get("initiativeIndex"));
        assertTrue(BattleEventPlaybackEnvelope.supportedKinds().contains("turn_start"));
    }

    @Test
    void preservesTurnStartThenStartEffectThenPendingSkipBeforeDecisionWindow() {
        BattlePlaybackBatch playback = new BattlePlaybackBatch(
                "reservation-start-effects",
                List.of(
                        new BattleEventPlaybackEnvelope(
                                50,
                                "turn_start",
                                "turn_start|3|pokemon-alpha|start|4",
                                Map.of()
                        ),
                        new BattleEventPlaybackEnvelope(
                                51,
                                "rule_effect",
                                "rule_effect|status|flinch|pokemon-alpha|||flinch|0.0|30",
                                Map.of("effect", "forged")
                        ),
                        new BattleEventPlaybackEnvelope(
                                52,
                                "status_skip",
                                "status_skip|pokemon-alpha|Flinch|start|flinched",
                                Map.of("status", "forged", "phase", "end")
                        )
                )
        );

        BattlePresentationBatch presentation = projector.project(playback);

        assertEquals(List.of(50L, 51L, 52L), presentation.commands().stream()
                .map(BattlePresentationCommand::sequence)
                .toList());
        assertEquals(List.of(
                        BattlePresentationCommand.Kind.TURN_START_CUE,
                        BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                        BattlePresentationCommand.Kind.STATUS_SKIP_CUE),
                presentation.commands().stream().map(BattlePresentationCommand::kind).toList());
        assertEquals("status", presentation.commands().get(1).data().get("sourceKind"));
        assertEquals("flinch", presentation.commands().get(1).data().get("effect"));
        assertEquals("Flinch", presentation.commands().get(2).data().get("status"));
        assertEquals("start", presentation.commands().get(2).data().get("phase"));
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
        assertTrue(initiative.contracts().contains("advanceInitiativeTurnWithRollover"));
        assertTrue(initiative.contracts().contains("InitiativeRoundRebuilder"));
        assertTrue(initiative.contracts().contains("TurnStartedEvent"));
        assertTrue(initiative.contracts().contains("START status/ability/perk hooks"));
        assertTrue(initiative.contracts().contains("pending status skip"));
        assertTrue(initiative.adapterPolicy().contains("must not choose the next actor"));
        assertTrue(initiative.adapterPolicy().contains("provide client-computed initiative"));
        assertTrue(initiative.adapterPolicy().contains("execute START hooks"));
        assertTrue(lifecycle.contracts().contains("ROUND_START lifecycle events before core-owned InitiativeRoundRebuilder output"));
        assertTrue(lifecycle.adapterPolicy().contains("round-start lifecycle events before the next turn_start"));
        assertTrue(lifecycle.adapterPolicy().contains("full Python initiative rebuild formula"));
        assertTrue(legalActions.contracts().contains("before opening the next decision window"));
        assertTrue(legalActions.adapterPolicy().contains("must not supply the next-round initiative order"));
        assertEquals("6678d4563116a4ec8c70d9daafc00d28bb9ab25b", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
    }
}