package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuraStormErrataPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void preservesAuraBreakThenAuraStormThenAuthoritativeMoveAndHpOrder() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-aura-errata", List.of(
                new BattleEventPlaybackEnvelope(
                        70,
                        "rule_effect",
                        "rule_effect|ability|Aura Break [Errata]|breaker|actor|aura-strike|damage_penalty|-6|100",
                        Map.of("amount", "999", "actorHp", "0")
                ),
                new BattleEventPlaybackEnvelope(
                        71,
                        "rule_effect",
                        "rule_effect|ability|Aura Storm [Errata]|actor|target|aura-strike|damage_penalty|-6|200",
                        Map.of("amount", "999", "actorHp", "0")
                ),
                new BattleEventPlaybackEnvelope(
                        72,
                        "move_resolved",
                        "move_resolved|runtime|actor|target|aura-strike|true|false|18|182",
                        Map.of("damage", "999", "targetHp", "0")
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(4, presentation.commands().size());
        BattlePresentationCommand auraBreak = presentation.commands().get(0);
        BattlePresentationCommand auraStorm = presentation.commands().get(1);
        BattlePresentationCommand animation = presentation.commands().get(2);
        BattlePresentationCommand hp = presentation.commands().get(3);

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, auraBreak.kind());
        assertEquals(70, auraBreak.sequence());
        assertEquals("breaker", auraBreak.subjectId());
        assertEquals("Aura Break [Errata]", auraBreak.data().get("sourceName"));
        assertEquals("actor", auraBreak.data().get("targetId"));
        assertEquals("damage_penalty", auraBreak.data().get("effect"));
        assertEquals("-6.0", auraBreak.data().get("amount"));

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, auraStorm.kind());
        assertEquals(71, auraStorm.sequence());
        assertEquals("actor", auraStorm.subjectId());
        assertEquals("Aura Storm [Errata]", auraStorm.data().get("sourceName"));
        assertEquals("target", auraStorm.data().get("targetId"));
        assertEquals("damage_penalty", auraStorm.data().get("effect"));
        assertEquals("-6.0", auraStorm.data().get("amount"));

        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, animation.kind());
        assertEquals(72, animation.sequence());
        assertEquals("actor", animation.subjectId());
        assertEquals("aura-strike", animation.data().get("moveId"));

        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, hp.kind());
        assertEquals(72, hp.sequence());
        assertEquals("target", hp.subjectId());
        assertEquals("18", hp.data().get("damage"));
        assertEquals("182", hp.data().get("targetHp"));
    }

    @Test
    void expiredAuraBreakPathCanRenderPositiveAuraStormWithoutAdapterExpiryLogic() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-aura-expired", List.of(
                new BattleEventPlaybackEnvelope(
                        80,
                        "rule_effect",
                        "rule_effect|ability|Aura Storm [Errata]|actor|target|aura-strike|damage_bonus|6|200",
                        Map.of("expires_round", "3", "current_round", "4")
                ),
                new BattleEventPlaybackEnvelope(
                        81,
                        "move_resolved",
                        "move_resolved|runtime|actor|target|aura-strike|true|false|30|170",
                        Map.of()
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(3, presentation.commands().size());
        assertEquals("Aura Storm [Errata]", presentation.commands().get(0).data().get("sourceName"));
        assertEquals("damage_bonus", presentation.commands().get(0).data().get("effect"));
        assertEquals("6.0", presentation.commands().get(0).data().get("amount"));
        assertEquals("30", presentation.commands().get(2).data().get("damage"));
        assertEquals("170", presentation.commands().get(2).data().get("targetHp"));
    }
}
