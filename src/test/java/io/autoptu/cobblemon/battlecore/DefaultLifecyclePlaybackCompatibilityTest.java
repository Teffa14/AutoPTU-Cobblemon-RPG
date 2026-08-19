package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultLifecyclePlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void preservesDefaultLifecyclePhaseThenRuleEffectOrdering() {
        BattlePlaybackBatch input = new BattlePlaybackBatch("reservation-lifecycle", List.of(
                new BattleEventPlaybackEnvelope(
                        100,
                        "phase",
                        "phase|2|actor|end",
                        Map.of("phase", "forged")
                ),
                new BattleEventPlaybackEnvelope(
                        101,
                        "rule_effect",
                        "rule_effect|ability|Lancer|actor|||damage_reduction|5.0|20",
                        Map.of("amount", "999")
                ),
                new BattleEventPlaybackEnvelope(
                        102,
                        "turn_end",
                        "turn_end|2|actor|end",
                        Map.of()
                )
        ));

        BattlePresentationBatch output = projector.project(input);

        assertEquals(3, output.commands().size());
        assertEquals(BattlePresentationCommand.Kind.PHASE_CUE, output.commands().get(0).kind());
        assertEquals(100, output.commands().get(0).sequence());
        assertEquals("2", output.commands().get(0).data().get("round"));
        assertEquals("end", output.commands().get(0).data().get("phase"));

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, output.commands().get(1).kind());
        assertEquals(101, output.commands().get(1).sequence());
        assertEquals("ability", output.commands().get(1).data().get("sourceKind"));
        assertEquals("Lancer", output.commands().get(1).data().get("sourceName"));
        assertEquals("5.0", output.commands().get(1).data().get("amount"));

        assertEquals(BattlePresentationCommand.Kind.TURN_END_CUE, output.commands().get(2).kind());
        assertEquals(102, output.commands().get(2).sequence());
        assertEquals("actor", output.commands().get(2).subjectId());
        assertEquals("end", output.commands().get(2).data().get("phase"));
    }

    @Test
    void rejectsMalformedLifecycleStableKeysInsteadOfInventingState() {
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "phase", "phase|-1|actor|end", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "turn_end", "turn_end|2||end", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                1, "phase", "phase|2|actor", Map.of()
        )));
    }
}
