package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldEffectPlaybackCompatibilityTest {
    @Test
    void preservesFieldExpiryBeforeLaterAuthoritativeCombatPlayback() {
        BattlePlaybackBatch events = new BattlePlaybackBatch("battle-field", List.of(
                new BattleEventPlaybackEnvelope(
                        20,
                        "field_effect",
                        "field_effect|terrain|electric terrain|terrain_ends|3",
                        Map.of("remaining", "99", "effect", "forged")
                ),
                new BattleEventPlaybackEnvelope(
                        21,
                        "move_resolved",
                        "move_resolved|delayed|pokemon-1|pokemon-2|future sight|true|false|14|26",
                        Map.of("damage", "999", "targetHp", "0")
                )
        ));

        BattlePresentationBatch presentation = new BattlePresentationProjector().project(events);

        assertEquals(3, presentation.commands().size());
        BattlePresentationCommand field = presentation.commands().get(0);
        assertEquals(BattlePresentationCommand.Kind.FIELD_EFFECT_CUE, field.kind());
        assertEquals(20, field.sequence());
        assertEquals("terrain", field.subjectId());
        assertEquals("electric terrain", field.data().get("effectName"));
        assertEquals("terrain_ends", field.data().get("effect"));
        assertEquals("3", field.data().get("round"));
        assertEquals(BattlePresentationCommand.Kind.MOVE_ANIMATION, presentation.commands().get(1).kind());
        assertEquals(BattlePresentationCommand.Kind.HP_PROJECTION, presentation.commands().get(2).kind());
        assertEquals("14", presentation.commands().get(2).data().get("damage"));
        assertEquals("26", presentation.commands().get(2).data().get("targetHp"));
    }

    @Test
    void fieldExpiryRemainsGlobalAndNeverRequiresCombatantEntityBinding() {
        BattlePresentationBatch presentation = new BattlePresentationBatch("battle-field", List.of(
                new BattlePresentationCommand(
                        4,
                        0,
                        BattlePresentationCommand.Kind.FIELD_EFFECT_CUE,
                        "room",
                        Map.of("fieldKind", "room", "effectName", "wonder room", "effect", "room_ends", "round", "2")
                )
        ));
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        List<EntityBoundBattlePresentationCommand> bound =
                new BattlePresentationEntityProjector().bindCombatantSemanticCues(presentation, bindings);

        assertTrue(bound.isEmpty());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).support());
    }

    private static BattleAuthoritySnapshot snapshot() {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot first = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:charizard", 40, Set.of("Sky"), null, 2);
        BattlePokemonSnapshot second = new BattlePokemonSnapshot(
                "pokemon-2", "player-1", "cobblemon:blastoise", 40, Set.of("Swim"), null, 2);
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                "minecraft:overworld", 100, 64, 200, 1, 0, 0, 1);
        return new BattleAuthoritySnapshot(
                "battle-field", "player-1", trainer, List.of(first, second), List.of(), 1234L, arena);
    }
}
