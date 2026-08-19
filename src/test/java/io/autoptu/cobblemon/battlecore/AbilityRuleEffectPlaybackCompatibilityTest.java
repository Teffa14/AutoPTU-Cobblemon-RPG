package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityRuleEffectPlaybackCompatibilityTest {
    @Test
    void lancerEndCritRangeUsesGenericRuleEffectPlayback() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                41,
                "rule_effect",
                "rule_effect|ability|lancer|pokemon-a|||crit_range|3.0|37"
        );

        List<BattlePresentationCommand> commands = new BattlePresentationProjector().project(event);

        assertEquals(1, commands.size());
        BattlePresentationCommand command = commands.getFirst();
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, command.kind());
        assertEquals("pokemon-a", command.subjectId());
        assertEquals("ability", command.data().get("sourceKind"));
        assertEquals("lancer", command.data().get("sourceName"));
        assertEquals("crit_range", command.data().get("effect"));
        assertEquals("3.0", command.data().get("amount"));
        assertEquals("37", command.data().get("actorHp"));
    }

    @Test
    void innerFocusStatusBlockUsesSameGenericRuleEffectPlayback() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                42,
                "rule_effect",
                "rule_effect|ability|inner focus|target|target|fake-out|status_block|0.0|20"
        );

        List<BattlePresentationCommand> commands = new BattlePresentationProjector().project(event);

        assertEquals(1, commands.size());
        BattlePresentationCommand command = commands.getFirst();
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, command.kind());
        assertEquals("target", command.subjectId());
        assertEquals("ability", command.data().get("sourceKind"));
        assertEquals("inner focus", command.data().get("sourceName"));
        assertEquals("target", command.data().get("targetId"));
        assertEquals("fake-out", command.data().get("moveId"));
        assertEquals("status_block", command.data().get("effect"));
        assertEquals("0.0", command.data().get("amount"));
    }
}
