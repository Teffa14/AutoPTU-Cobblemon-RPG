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
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePresentationEntitySemanticCueTest {
    private final BattlePresentationEntityProjector projector = new BattlePresentationEntityProjector();

    @Test
    void bindsOnlyCombatantSemanticCuesToTheirRegisteredEntity() {
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));
        BattlePresentationBatch batch = new BattlePresentationBatch("battle-entity", List.of(
                new BattlePresentationCommand(1, 0, BattlePresentationCommand.Kind.TURN_START_CUE,
                        "pokemon-1", Map.of("round", "2", "phase", "START", "initiativeIndex", "0")),
                new BattlePresentationCommand(2, 0, BattlePresentationCommand.Kind.STATUS_SKIP_CUE,
                        "pokemon-2", Map.of("status", "Flinch", "phase", "START", "reason", "flinched")),
                new BattlePresentationCommand(3, 0, BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                        "pokemon-1", Map.of("sourceKind", "ability", "sourceName", "Simple", "effect", "stage")),
                new BattlePresentationCommand(4, 0, BattlePresentationCommand.Kind.TRAINER_FEATURE_CUE,
                        "trainer-1", Map.of("feature", "Defense Mastery")),
                new BattlePresentationCommand(5, 0, BattlePresentationCommand.Kind.FIELD_EFFECT_CUE,
                        "room", Map.of("effectName", "wonder room", "effect", "room_ends", "round", "3")),
                new BattlePresentationCommand(6, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION,
                        "pokemon-2", Map.of("moveId", "Tackle"))));

        List<EntityBoundBattlePresentationCommand> bound = projector.bindCombatantSemanticCues(batch, bindings);

        assertEquals(3, bound.size());
        assertEquals(BattlePresentationCommand.Kind.TURN_START_CUE, bound.get(0).command().kind());
        assertEquals("entity-a", bound.get(0).presentationEntityId());
        assertEquals("pokemon-2", bound.get(1).combatantId());
        assertEquals("entity-b", bound.get(1).presentationEntityId());
        assertEquals(BattlePresentationCommand.Kind.RULE_EFFECT_CUE, bound.get(2).command().kind());
        assertEquals("entity-a", bound.get(2).presentationEntityId());
    }

    @Test
    void rejectsCrossReservationOrInjectedCombatantSemanticCue() {
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        BattlePresentationBatch wrongReservation = new BattlePresentationBatch("other-battle", List.of(
                new BattlePresentationCommand(1, 0, BattlePresentationCommand.Kind.TURN_START_CUE,
                        "pokemon-1", Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> projector.bindCombatantSemanticCues(wrongReservation, bindings));

        BattlePresentationBatch injected = new BattlePresentationBatch("battle-entity", List.of(
                new BattlePresentationCommand(1, 0, BattlePresentationCommand.Kind.STATUS_SKIP_CUE,
                        "pokemon-injected", Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> projector.bindCombatantSemanticCues(injected, bindings));
    }

    @Test
    void entityBindingDoesNotBroadenUpstreamRuleSupport() {
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
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
                "battle-entity", "player-1", trainer, List.of(first, second), List.of(), 1234L, arena);
    }
}
