package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PostDamageAuraHpPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void rendersAuraEventBeforeAuthoritativeAdjustedDamageAndHp() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-aura", List.of(
                new BattleEventPlaybackEnvelope(
                        40,
                        "rule_effect",
                        "rule_effect|ability|Aqua Boost|aura-holder|attacker|water pulse|damage_bonus|5.0|100",
                        Map.of("amount", "999", "actorHp", "0")
                ),
                new BattleEventPlaybackEnvelope(
                        41,
                        "move_resolved",
                        "move_resolved|runtime|attacker|defender|water pulse|true|false|24|36",
                        Map.of("damage", "19", "targetHp", "41")
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(3, presentation.commands().size());
        BattlePresentationCommand aura = presentation.commands().get(0);
        BattlePresentationCommand animation = presentation.commands().get(1);
        BattlePresentationCommand hp = presentation.commands().get(2);

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, aura.kind());
        assertEquals(40, aura.sequence());
        assertEquals("Aqua Boost", aura.data().get("sourceName"));
        assertEquals("5.0", aura.data().get("amount"));

        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, animation.kind());
        assertEquals(41, animation.sequence());
        assertEquals("attacker", animation.subjectId());
        assertEquals("water pulse", animation.data().get("moveId"));

        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, hp.kind());
        assertEquals(41, hp.sequence());
        assertEquals("defender", hp.subjectId());
        assertEquals("24", hp.data().get("damage"));
        assertEquals("36", hp.data().get("targetHp"));
    }

    @Test
    void damagePlaybackConsumesPartialCoreResultWithoutGrantingAdapterAuthority() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.DAMAGE_RESULT_PLAYBACK);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);

        assertFalse(requirement.hasBlockingDependency());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(
                java.util.Set.of(
                        UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE
                ),
                requirement.capabilities()
        );
    }
}
