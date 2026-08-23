package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityRuleEffectPlaybackCompatibilityTest {
    @Test
    void lancerEndCritRangeUsesGenericRuleEffectPlayback() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                41,
                "rule_effect",
                "rule_effect|ability|lancer|pokemon-a|||crit_range|3.0|37",
                Map.of()
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
                "rule_effect|ability|inner focus|target|target|fake-out|status_block|0.0|20",
                Map.of()
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

    @Test
    void spatialVeilStatusBlockPreservesAuthoritativeSourceAndTargetWithoutAdapterRuleLogic() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                43,
                "rule_effect",
                "rule_effect|ability|Aroma Veil|veil-source|target|confuse-ray|status_block|0.0|20",
                Map.of("ignoredAdapterHint", "radius-3")
        );

        List<BattlePresentationCommand> commands = new BattlePresentationProjector().project(event);

        assertEquals(1, commands.size());
        BattlePresentationCommand command = commands.getFirst();
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, command.kind());
        assertEquals("veil-source", command.subjectId());
        assertEquals("ability", command.data().get("sourceKind"));
        assertEquals("Aroma Veil", command.data().get("sourceName"));
        assertEquals("target", command.data().get("targetId"));
        assertEquals("confuse-ray", command.data().get("moveId"));
        assertEquals("status_block", command.data().get("effect"));
        assertEquals("0.0", command.data().get("amount"));
        assertEquals("20", command.data().get("actorHp"));
        assertFalse(command.data().containsKey("ignoredAdapterHint"));

        IntegrationFeatureCompatibility.Requirement abilityPlayback = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ABILITY_EFFECT_PLAYBACK);
        assertTrue(abilityPlayback.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ABILITIES));
        assertFalse(abilityPlayback.hasBlockingDependency());
    }

    @Test
    void simpleCombatStageReactionUsesGenericRuleEffectPlaybackWithoutAdapterStageMutation() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                44,
                "rule_effect",
                "rule_effect|ability|simple|target|target|growl|simple|-1.0|20",
                Map.of("requestedStageDelta", "-1", "appliedStageDelta", "-1")
        );

        List<BattlePresentationCommand> commands = new BattlePresentationProjector().project(event);

        assertEquals(1, commands.size());
        BattlePresentationCommand command = commands.getFirst();
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, command.kind());
        assertEquals("target", command.subjectId());
        assertEquals("ability", command.data().get("sourceKind"));
        assertEquals("simple", command.data().get("sourceName"));
        assertEquals("growl", command.data().get("moveId"));
        assertEquals("simple", command.data().get("effect"));
        assertEquals("-1.0", command.data().get("amount"));
        assertEquals("20", command.data().get("actorHp"));
        assertFalse(command.data().containsKey("requestedStageDelta"));
        assertFalse(command.data().containsKey("appliedStageDelta"));

        IntegrationFeatureCompatibility.Requirement abilityPlayback = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ABILITY_EFFECT_PLAYBACK);
        assertTrue(abilityPlayback.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ABILITIES));
        assertFalse(abilityPlayback.hasBlockingDependency());
    }
}
