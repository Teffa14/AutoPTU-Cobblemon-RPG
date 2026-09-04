package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleSemanticCueRendererTest {
    @Test
    void turnStartCopiesOnlyAuthoritativeLifecycleFields() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                20,
                0,
                BattlePresentationCommand.Kind.TURN_START_CUE,
                "actor-1",
                Map.of("round", "3", "phase", "action", "initiativeIndex", "1")
        );

        assertEquals(
                "Turn start · round 3 · action · initiative 1",
                BattleSemanticCueRenderer.cueText(cue)
        );
    }

    @Test
    void turnEndAndPhaseCopyValidatedLifecycleFields() {
        BattlePresentationCommand turnEnd = new BattlePresentationCommand(
                21,
                0,
                BattlePresentationCommand.Kind.TURN_END_CUE,
                "actor-1",
                Map.of("round", "3", "phase", "action")
        );
        BattlePresentationCommand phase = new BattlePresentationCommand(
                22,
                0,
                BattlePresentationCommand.Kind.PHASE_CUE,
                "actor-1",
                Map.of("round", "4", "phase", "round_end")
        );

        assertEquals("Turn end · round 3 · action", BattleSemanticCueRenderer.cueText(turnEnd));
        assertEquals("Phase · round 4 · round_end", BattleSemanticCueRenderer.cueText(phase));
    }

    @Test
    void ruleEffectDoesNotClassifyMeaningLocally() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                23,
                0,
                BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                "actor-1",
                Map.of(
                        "sourceKind", "ability",
                        "sourceName", "Authoritative Ability",
                        "targetId", "actor-1",
                        "moveId", "",
                        "effect", "engine_effect_key",
                        "amount", "2.0",
                        "actorHp", "41"
                )
        );

        assertEquals(
                "Authoritative Ability · engine_effect_key · 2.0",
                BattleSemanticCueRenderer.cueText(cue)
        );
    }

    @Test
    void statusSkipKeepsExistingExactProjection() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                24,
                0,
                BattlePresentationCommand.Kind.STATUS_SKIP_CUE,
                "actor-1",
                Map.of("status", "sleep", "phase", "action", "reason", "cannot_act")
        );

        assertEquals(
                "sleep · action · cannot_act",
                BattleSemanticCueRenderer.cueText(cue)
        );
    }

    @Test
    void missingRequiredLifecycleFieldFailsClosed() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                25,
                0,
                BattlePresentationCommand.Kind.TURN_START_CUE,
                "actor-1",
                Map.of("round", "1", "phase", "action")
        );

        assertThrows(IllegalArgumentException.class, () -> BattleSemanticCueRenderer.cueText(cue));
    }

    @Test
    void nonCombatantCueTextIsNotInvented() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                26,
                0,
                BattlePresentationCommand.Kind.FIELD_EFFECT_CUE,
                "terrain",
                Map.of("fieldKind", "terrain", "effectName", "rain", "effect", "terrain_ends", "round", "2")
        );

        assertThrows(IllegalArgumentException.class, () -> BattleSemanticCueRenderer.cueText(cue));
    }
}
