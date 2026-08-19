package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecyclePlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void projectsCurrentPhaseAndTurnEndContractsWithoutAdapterLifecycleDecisions() {
        BattlePlaybackBatch input = new BattlePlaybackBatch("reservation-lifecycle", List.of(
                new BattleEventPlaybackEnvelope(20, "phase", "phase|3|alpha|end", Map.of("phase", "forged")),
                new BattleEventPlaybackEnvelope(
                        21,
                        "rule_effect",
                        "rule_effect|ability|lancer|alpha|||damage_reduction|5.0|40",
                        Map.of()
                ),
                new BattleEventPlaybackEnvelope(22, "turn_end", "turn_end|3|alpha|end", Map.of("round", "999"))
        ));

        BattlePresentationBatch output = projector.project(input);

        assertEquals(3, output.commands().size());
        assertEquals(BattlePresentationCommand.Kind.PHASE_CUE, output.commands().get(0).kind());
        assertEquals("3", output.commands().get(0).data().get("round"));
        assertEquals("end", output.commands().get(0).data().get("phase"));
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, output.commands().get(1).kind());
        assertEquals("lancer", output.commands().get(1).data().get("sourceName"));
        assertEquals(BattlePresentationCommand.Kind.TURN_END_CUE, output.commands().get(2).kind());
        assertEquals("3", output.commands().get(2).data().get("round"));
        assertEquals(List.of(20L, 21L, 22L), output.commands().stream().map(BattlePresentationCommand::sequence).toList());
    }

    @Test
    void lifecycleStableKeysFailClosedInsteadOfGuessingMissingState() {
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                new BattleEventPlaybackEnvelope(1, "phase", "phase|alpha|end", Map.of())
        ));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                new BattleEventPlaybackEnvelope(2, "turn_end", "turn_end|-1|alpha|end", Map.of())
        ));
    }

    @Test
    void lifecyclePlaybackRemainsPartialAndCoreOwned() {
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE
        );
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertTrue(lifecycle.adapterPolicy().contains("event ordering"));
    }
}
