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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleEntityBoundPresentationStreamTest {
    private final BattlePresentationEntityProjector projector = new BattlePresentationEntityProjector();

    @Test
    void mergesCombatantOutputsInAuthoritativeSequenceAndOrdinalOrder() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        BattlePresentationBatch presentation = new BattlePresentationBatch("battle-stream", List.of(
                command(10, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("targetId", "pokemon-2", "moveId", "Tackle")),
                command(10, 1, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2", Map.of()),
                command(11, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, "pokemon-1", Map.of()),
                command(12, 0, BattlePresentationCommand.Kind.TURN_START_CUE, "pokemon-2", Map.of()),
                command(13, 0, BattlePresentationCommand.Kind.TRAINER_FEATURE_CUE, "player-1", Map.of())));
        BattleHealthProjectionBatch health = new BattleHealthProjectionBatch("battle-stream", List.of(
                new BattleHealthProjection(10, 1, "pokemon-2", 20, 40)));
        BattleWorldRelocationBatch relocations = new BattleWorldRelocationBatch(
                "battle-stream", snapshot.arena(), List.of(
                new BattleWorldRelocation(
                        11, 0, "pokemon-1",
                        new WorldBlockCoordinate("minecraft:overworld", 100, 64, 200),
                        new WorldBlockCoordinate("minecraft:overworld", 101, 64, 200))));

        BattleEntityBoundPresentationStream stream = projector.bindCombatantStream(
                presentation, health, relocations, bindings);

        assertEquals("battle-stream", stream.reservationId());
        assertEquals(4, stream.outputs().size());
        assertInstanceOf(EntityBoundMoveAnimation.class, stream.outputs().get(0));
        assertInstanceOf(EntityBoundBattleHealthProjection.class, stream.outputs().get(1));
        assertInstanceOf(EntityBoundBattleWorldRelocation.class, stream.outputs().get(2));
        assertInstanceOf(EntityBoundBattlePresentationCommand.class, stream.outputs().get(3));
        assertEquals(List.of(10L, 10L, 11L, 12L),
                stream.outputs().stream().map(EntityBoundPresentationOutput::sequence).toList());
        assertEquals(List.of(0, 1, 0, 0),
                stream.outputs().stream().map(EntityBoundPresentationOutput::ordinal).toList());

        EntityBoundMoveAnimation move = (EntityBoundMoveAnimation) stream.outputs().get(0);
        assertEquals("entity-a", move.attackerPresentationEntityId());
        assertEquals("entity-b", move.targetPresentationEntityId());
        EntityBoundBattleHealthProjection hp = (EntityBoundBattleHealthProjection) stream.outputs().get(1);
        assertEquals("entity-b", hp.presentationEntityId());
        EntityBoundBattlePresentationCommand turn = (EntityBoundBattlePresentationCommand) stream.outputs().get(3);
        assertEquals("entity-b", turn.presentationEntityId());
    }

    @Test
    void rejectsMissingOrInjectedTypedProjectionCoverage() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));
        BattlePresentationBatch presentation = new BattlePresentationBatch("battle-stream", List.of(
                command(10, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2", Map.of())));
        BattleWorldRelocationBatch noRelocations = new BattleWorldRelocationBatch(
                "battle-stream", snapshot.arena(), List.of());

        assertThrows(IllegalArgumentException.class, () -> projector.bindCombatantStream(
                presentation,
                new BattleHealthProjectionBatch("battle-stream", List.of()),
                noRelocations,
                bindings));

        assertThrows(IllegalArgumentException.class, () -> projector.bindCombatantStream(
                new BattlePresentationBatch("battle-stream", List.of()),
                new BattleHealthProjectionBatch("battle-stream", List.of(
                        new BattleHealthProjection(10, 0, "pokemon-2", 1, 59))),
                noRelocations,
                bindings));
    }

    @Test
    void rejectsCrossReservationTypedBatches() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        assertThrows(IllegalArgumentException.class, () -> projector.bindCombatantStream(
                new BattlePresentationBatch("battle-stream", List.of()),
                new BattleHealthProjectionBatch("other-battle", List.of()),
                new BattleWorldRelocationBatch("battle-stream", snapshot.arena(), List.of()),
                bindings));
    }

    private static BattlePresentationCommand command(
            long sequence,
            int ordinal,
            BattlePresentationCommand.Kind kind,
            String subjectId,
            Map<String, String> data) {
        return new BattlePresentationCommand(sequence, ordinal, kind, subjectId, data);
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
                "battle-stream", "player-1", trainer, List.of(first, second), List.of(), 1234L, arena);
    }
}
