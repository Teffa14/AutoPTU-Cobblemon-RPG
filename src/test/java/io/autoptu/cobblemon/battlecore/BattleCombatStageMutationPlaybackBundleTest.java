package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCombatStageMutationPlaybackBundleTest {
    @Test
    void preservesAuthoritativeRecursiveReactionEventOrder() {
        BattleCombatStageMutationProjection mutation = new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.DEF,
                0,
                -1,
                -1,
                -1,
                -2
        );
        BattlePlaybackBatch playback = new BattlePlaybackBatch("battle-1", List.of(
                new BattleEventPlaybackEnvelope(
                        40,
                        "rule_effect",
                        "rule_effect|ability|simple|target|target||double_stage|-1.0|50",
                        Map.of("source", "Simple")
                ),
                new BattleEventPlaybackEnvelope(
                        41,
                        "rule_effect",
                        "rule_effect|ability|minus [swsh]|holder|target||extra_drop|-1.0|50",
                        Map.of("source", "Minus [SwSh]")
                )
        ));

        BattleCombatStageMutationPlaybackBundle bundle = new BattleCombatStageMutationPlaybackBundle(
                "battle-1", mutation, playback
        );

        assertTrue(bundle.hasReactionPlayback());
        assertEquals(-2, bundle.mutation().finalStage());
        assertEquals(2, bundle.events().size());
        assertTrue(bundle.events().get(0).stableKey().contains("simple"));
        assertTrue(bundle.events().get(1).stableKey().contains("minus [swsh]"));
        assertThrows(UnsupportedOperationException.class, () -> bundle.events().add(
                new BattleEventPlaybackEnvelope(42, "rule_effect", "rule_effect|ability|x|a|b||effect|1.0|1", Map.of())
        ));
    }

    @Test
    void acceptsMutationWithoutReactionEvents() {
        BattleCombatStageMutationPlaybackBundle bundle = new BattleCombatStageMutationPlaybackBundle(
                "battle-1",
                new BattleCombatStageMutationProjection(
                        "target",
                        BattleCombatStageMutationProjection.Stat.ATK,
                        0,
                        1,
                        1,
                        1,
                        1
                ),
                new BattlePlaybackBatch("battle-1", List.of())
        );

        assertFalse(bundle.hasReactionPlayback());
        assertTrue(bundle.events().isEmpty());
    }

    @Test
    void rejectsCrossReservationPlaybackInjection() {
        assertThrows(IllegalArgumentException.class, () -> new BattleCombatStageMutationPlaybackBundle(
                "battle-1",
                new BattleCombatStageMutationProjection(
                        "target",
                        BattleCombatStageMutationProjection.Stat.SPD,
                        0,
                        1,
                        1,
                        1,
                        1
                ),
                new BattlePlaybackBatch("battle-2", List.of())
        ));
    }
}
