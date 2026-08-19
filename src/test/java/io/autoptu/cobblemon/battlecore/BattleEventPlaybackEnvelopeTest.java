package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEventPlaybackEnvelopeTest {
    @Test
    void acceptsCurrentStableUpstreamSemanticKindsWithoutRuleRecalculation() {
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                3,
                "rule_effect",
                "rule_effect|item|Pink Pearl|attacker|target|psybeam|damage_modifier|5.0|27",
                Map.of(
                        "sourceKind", "item",
                        "sourceName", "Pink Pearl",
                        "effect", "damage_modifier",
                        "amount", "5.0"
                )
        );

        assertEquals("rule_effect", event.kind());
        assertEquals("Pink Pearl", event.attributes().get("sourceName"));
        assertTrue(BattleEventPlaybackEnvelope.supportedKinds().containsAll(List.of(
                "move_resolved",
                "shift_resolved",
                "status_skip",
                "trainer_feature",
                "rule_effect",
                "phase",
                "turn_end"
        )));
    }

    @Test
    void rejectsUnknownKindsAndMismatchedStableKeys() {
        assertThrows(IllegalArgumentException.class, () -> new BattleEventPlaybackEnvelope(
                0, "ability_damage", "ability_damage|actor", Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleEventPlaybackEnvelope(
                0, "move_resolved", "rule_effect|actor", Map.of()
        ));
    }

    @Test
    void defensivelyCopiesPresentationAttributes() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("actorId", "alpha");
        BattleEventPlaybackEnvelope event = new BattleEventPlaybackEnvelope(
                0,
                "status_skip",
                "status_skip|alpha|sleep|standard|1|2",
                attributes
        );

        attributes.put("actorId", "forged");
        assertEquals("alpha", event.attributes().get("actorId"));
        assertThrows(UnsupportedOperationException.class,
                () -> event.attributes().put("damage", "9999"));
    }

    @Test
    void batchRequiresAuthoritativeOrderAndIsImmutable() {
        BattleEventPlaybackEnvelope first = new BattleEventPlaybackEnvelope(
                4, "shift_resolved", "shift_resolved|alpha|0,0|1,0", Map.of()
        );
        BattleEventPlaybackEnvelope second = new BattleEventPlaybackEnvelope(
                5, "move_resolved", "move_resolved|runtime|alpha|beta|tackle|true|false|7|13", Map.of()
        );
        BattlePlaybackBatch batch = new BattlePlaybackBatch("reservation-1", List.of(first, second));

        assertEquals(List.of(first, second), batch.events());
        assertThrows(UnsupportedOperationException.class, () -> batch.events().add(first));
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePlaybackBatch("reservation-1", List.of(second, first)));
    }
}
