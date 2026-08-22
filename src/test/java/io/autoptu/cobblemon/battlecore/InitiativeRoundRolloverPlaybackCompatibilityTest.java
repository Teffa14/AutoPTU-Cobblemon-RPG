package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitiativeRoundRolloverPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void preservesRoundStartLifecycleEffectsBeforeNextTurnStart() {
        BattlePlaybackBatch playback = new BattlePlaybackBatch(
                "reservation-rollover",
                List.of(
                        new BattleEventPlaybackEnvelope(
                                70,
                                "rule_effect",
                                "rule_effect|lifecycle|round_cleanup|system|||round_start|0.0|0",
                                Map.of("effect", "forged", "round", "999")
                        ),
                        new BattleEventPlaybackEnvelope(
                                71,
                                "turn_start",
                                "turn_start|4|pokemon-beta|start|0",
                                Map.of("actorId", "forged", "initiativeIndex", "99")
                        )
                )
        );

        BattlePresentationBatch presentation = projector.project(playback);

        assertEquals(List.of(70L, 71L), presentation.commands().stream()
                .map(BattlePresentationCommand::sequence)
                .toList());
        assertEquals(List.of(
                        BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                        BattlePresentationCommand.Kind.TURN_START_CUE),
                presentation.commands().stream().map(BattlePresentationCommand::kind).toList());
        assertEquals("lifecycle", presentation.commands().get(0).data().get("sourceKind"));
        assertEquals("round_start", presentation.commands().get(0).data().get("effect"));
        assertEquals("pokemon-beta", presentation.commands().get(1).subjectId());
        assertEquals("4", presentation.commands().get(1).data().get("round"));
        assertEquals("0", presentation.commands().get(1).data().get("initiativeIndex"));
    }

    @Test
    void rolloverCompatibilityKeepsInitiativeRebuildInsideCore() {
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
        assertTrue(initiative.contracts().contains("authoritative initiative assembly/installation"));
        assertTrue(initiative.contracts().contains("BattleRoundController"));
        assertTrue(initiative.adapterPolicy().contains("choose the next actor"));
        assertTrue(initiative.adapterPolicy().contains("provide client-computed modifiers"));
        assertTrue(lifecycle.contracts().contains("InitiativeOrderAssembly/InitiativeAssemblyInstaller"));
        assertTrue(lifecycle.adapterPolicy().contains("Java owns much of initiative rebuilding"));
        assertTrue(legalActions.contracts().contains("initiative exhaustion"));
        assertTrue(legalActions.adapterPolicy().contains("must not supply initiative order"));
    }
}
