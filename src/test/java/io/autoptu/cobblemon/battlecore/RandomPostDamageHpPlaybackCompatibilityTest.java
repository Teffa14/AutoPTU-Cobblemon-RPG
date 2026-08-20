package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RandomPostDamageHpPlaybackCompatibilityTest {
    private final BattlePresentationProjector projector = new BattlePresentationProjector();

    @Test
    void rendersLiveAdaptabilityResultOnlyFromAuthoritativeEvents() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("reservation-rng", List.of(
                new BattleEventPlaybackEnvelope(
                        70,
                        "rule_effect",
                        "rule_effect|ability|Adaptability [Errata]|actor|actor|fire-strike|damage_bonus|7.0|1",
                        Map.of("amount", "999", "targetHp", "0")
                ),
                new BattleEventPlaybackEnvelope(
                        71,
                        "move_resolved",
                        "move_resolved|runtime|actor|target|fire-strike|true|false|31|69",
                        Map.of("damage", "1", "targetHp", "99")
                )
        ));

        BattlePresentationBatch presentation = projector.project(events);

        assertEquals(3, presentation.commands().size());
        BattlePresentationCommand ability = presentation.commands().get(0);
        BattlePresentationCommand animation = presentation.commands().get(1);
        BattlePresentationCommand hp = presentation.commands().get(2);

        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, ability.kind());
        assertEquals("Adaptability [Errata]", ability.data().get("sourceName"));
        assertEquals("7.0", ability.data().get("amount"));

        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, animation.kind());
        assertEquals("fire-strike", animation.data().get("moveId"));

        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, hp.kind());
        assertEquals("target", hp.subjectId());
        assertEquals("31", hp.data().get("damage"));
        assertEquals("69", hp.data().get("targetHp"));
    }

    @Test
    void liveRngAbilityPlaybackStillConsumesOnlyPartialUpstreamCategories() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.DAMAGE_RESULT_PLAYBACK);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertFalse(requirement.hasBlockingDependency());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
    }
}
