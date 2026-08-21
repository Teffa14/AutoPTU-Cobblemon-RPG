package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePresentationProjectorTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void projectsMoveResultWithoutRecalculatingDamage() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                8,
                "move_resolved",
                "move_resolved|runtime|alpha|beta|tackle|true|true|17|23",
                Map.of("damage", "9999", "targetHp", "0")
        );

        List<BattlePresentationCommand> commands = projector.project(event);

        assertEquals(2, commands.size());
        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, commands.get(0).kind());
        assertEquals("alpha", commands.get(0).subjectId());
        assertEquals("tackle", commands.get(0).data().get("moveId"));
        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, commands.get(1).kind());
        assertEquals("beta", commands.get(1).subjectId());
        assertEquals("17", commands.get(1).data().get("damage"));
        assertEquals("23", commands.get(1).data().get("targetHp"));
    }

    @Test
    void projectsShiftUsingOnlyAuthoritativeAnchors() {
        BattlePresentationCommand command = projector.project(new BattleEventPlaybackEnvelope(
                2,
                "shift_resolved",
                "shift_resolved|alpha|-1,3|4,8",
                Map.of("destination", "999,999")
        )).getFirst();

        assertEquals(BattlePresentationCommand.Kind.ENTITY_RELOCATION, command.kind());
        assertEquals("alpha", command.subjectId());
        assertEquals("-1,3", command.data().get("origin"));
        assertEquals("4,8", command.data().get("destination"));
    }

    @Test
    void projectsStatusTrainerAndRuleEffectAsPresentationOnlyCues() {
        BattlePresentationCommand status = projector.project(new BattleEventPlaybackEnvelope(
                3,
                "status_skip",
                "status_skip|alpha|sleep|standard|asleep",
                Map.of()
        )).getFirst();
        BattlePresentationCommand trainer = projector.project(new BattleEventPlaybackEnvelope(
                4,
                "trainer_feature",
                "trainer_feature|alpha|focused training|status_skip_bypass||sleep|40",
                Map.of()
        )).getFirst();
        BattlePresentationCommand rule = projector.project(new BattleEventPlaybackEnvelope(
                5,
                "rule_effect",
                "rule_effect|item|pink pearl|alpha|beta|psybeam|damage_modifier|5.0|40",
                Map.of("amount", "500")
        )).getFirst();

        assertEquals(BattlePresentationCommand.Kind.STATUS_SKIP_CUE, status.kind());
        assertEquals("sleep", status.data().get("status"));
        assertEquals(BattlePresentationCommand.Kind.TRAINER_FEATURE_CUE, trainer.kind());
        assertEquals("focused training", trainer.data().get("feature"));
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, rule.kind());
        assertEquals("5.0", rule.data().get("amount"));
        assertEquals("pink pearl", rule.data().get("sourceName"));
    }

    @Test
    void projectsFieldExpiryFromStableContractOnly() {
        BattlePresentationCommand command = projector.project(new BattleEventPlaybackEnvelope(
                6,
                "field_effect",
                "field_effect|room|wonder room|room_ends|4",
                Map.of("fieldKind", "terrain", "effect", "forged", "round", "999")
        )).getFirst();

        assertEquals(BattlePresentationCommand.Kind.FIELD_EFFECT_CUE, command.kind());
        assertEquals("room", command.subjectId());
        assertEquals("room", command.data().get("fieldKind"));
        assertEquals("wonder room", command.data().get("effectName"));
        assertEquals("room_ends", command.data().get("effect"));
        assertEquals("4", command.data().get("round"));
    }

    @Test
    void batchPreservesAuthoritativeEventOrderAndIntraEventCommandOrder() {
        BattlePlaybackBatch input = new BattlePlaybackBatch("reservation-19", List.of(
                new BattleEventPlaybackEnvelope(
                        10, "move_resolved",
                        "move_resolved|runtime|alpha|beta|tackle|true|false|7|33", Map.of()
                ),
                new BattleEventPlaybackEnvelope(
                        11, "shift_resolved",
                        "shift_resolved|beta|2,2|2,4", Map.of()
                )
        ));

        BattlePresentationBatch output = projector.project(input);

        assertEquals("reservation-19", output.reservationId());
        assertEquals(3, output.commands().size());
        assertEquals(10, output.commands().get(0).sequence());
        assertEquals(0, output.commands().get(0).ordinal());
        assertEquals(10, output.commands().get(1).sequence());
        assertEquals(1, output.commands().get(1).ordinal());
        assertEquals(11, output.commands().get(2).sequence());
        assertThrows(UnsupportedOperationException.class, () -> output.commands().clear());
    }

    @Test
    void rejectsMalformedStableContractsRatherThanGuessingAdapterBehavior() {
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                0, "shift_resolved", "shift_resolved|alpha|origin|destination", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                0, "move_resolved", "move_resolved|runtime|alpha|beta|tackle|false|true|4|30", Map.of()
        )));
        assertThrows(IllegalArgumentException.class, () -> projector.project(new BattleEventPlaybackEnvelope(
                0, "field_effect", "field_effect|room|wonder room|zone_ends|4", Map.of()
        )));
    }
}
