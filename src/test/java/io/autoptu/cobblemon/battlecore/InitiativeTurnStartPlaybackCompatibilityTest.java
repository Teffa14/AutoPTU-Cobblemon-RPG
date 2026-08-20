package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

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
    void compatibilityKeepsTurnSelectionCoreOwnedAndLifecyclePartial() {
        UpstreamCompatibilityMatrix.Entry initiative = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE
        );
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE
        );

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, initiative.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertTrue(initiative.contracts().contains("advanceInitiativeTurn"));
        assertTrue(initiative.contracts().contains("TurnStartedEvent"));
        assertTrue(initiative.adapterPolicy().contains("must not choose the next actor"));
        assertTrue(lifecycle.contracts().contains("turn-start"));
        assertTrue(lifecycle.adapterPolicy().contains("Automatic round rollover"));
        assertTrue(lifecycle.adapterPolicy().contains("actor selection"));
        assertEquals("201e52e68184b52b14a5040f8a440058e6d8daa9", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
    }
}
