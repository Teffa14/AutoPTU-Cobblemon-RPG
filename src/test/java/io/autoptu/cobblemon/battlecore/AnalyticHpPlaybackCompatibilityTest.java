package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnalyticHpPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void rendersLiveAnalyticResultOnlyFromAuthoritativeEvents() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-analytic", List.of(
                new BattleEventPlaybackEnvelope(
                        90,
                        "rule_effect",
                        "rule_effect|ability|Analytic|actor|target|psychic-strike|damage_bonus|5.0|1",
                        Map.of("amount", "999", "targetHp", "0")
                ),
                new BattleEventPlaybackEnvelope(
                        91,
                        "move_resolved",
                        "move_resolved|runtime|actor|target|psychic-strike|true|false|28|72",
                        Map.of("damage", "1", "targetHp", "99")
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(3, presentation.commands().size());
        BattlePresentationCommand ability = presentation.commands().get(0);
        BattlePresentationCommand animation = presentation.commands().get(1);
        BattlePresentationCommand hp = presentation.commands().get(2);

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, ability.kind());
        assertEquals("Analytic", ability.data().get("sourceName"));
        assertEquals("5.0", ability.data().get("amount"));
        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, animation.kind());
        assertEquals("psychic-strike", animation.data().get("moveId"));
        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, hp.kind());
        assertEquals("target", hp.subjectId());
        assertEquals("28", hp.data().get("damage"));
        assertEquals("72", hp.data().get("targetHp"));
    }

    @Test
    void liveAnalyticPlaybackKeepsBroadCategoriesPartialAndAdapterBlocked() {
        UpstreamCompatibilityMatrix.Entry actionEconomy = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, actionEconomy.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
    }
}
