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

class BattlePresentationEntityMoveAnimationTest {
    private final BattlePresentationProjector presentationProjector = new BattlePresentationProjector();
    private final BattlePresentationEntityProjector entityProjector = new BattlePresentationEntityProjector();

    @Test
    void bindsBothAuthoritativeMoveEndpointsToTheirRegisteredEntities() {
        BattlePresentationEntityBindings bindings = bindings();
        BattlePresentationBatch presentation = presentationProjector.project(new BattlePlaybackBatch(
                "battle-move-entity",
                List.of(new BattleEventPlaybackEnvelope(
                        25,
                        "move_resolved",
                        "move_resolved|runtime|pokemon-1|pokemon-2|ember|true|false|14|46",
                        Map.of("targetId", "pokemon-injected", "moveId", "forged-move"))))) ;

        List<EntityBoundMoveAnimation> moves = entityProjector.bindMoveAnimations(presentation, bindings);

        assertEquals(1, moves.size());
        EntityBoundMoveAnimation move = moves.getFirst();
        assertEquals("pokemon-1", move.attackerCombatantId());
        assertEquals("entity-a", move.attackerPresentationEntityId());
        assertEquals("pokemon-2", move.targetCombatantId());
        assertEquals("entity-b", move.targetPresentationEntityId());
        assertEquals("ember", move.moveId());
        assertEquals("runtime", move.command().data().get("source"));
    }

    @Test
    void rejectsInjectedOrCrossReservationMoveEndpoints() {
        BattlePresentationEntityBindings bindings = bindings();
        BattlePresentationBatch injectedTarget = new BattlePresentationBatch(
                "battle-move-entity",
                List.of(new BattlePresentationCommand(
                        1, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("targetId", "pokemon-injected", "moveId", "ember"))));
        assertThrows(IllegalArgumentException.class,
                () -> entityProjector.bindMoveAnimations(injectedTarget, bindings));

        BattlePresentationBatch otherReservation = new BattlePresentationBatch(
                "other-battle",
                List.of(new BattlePresentationCommand(
                        1, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("targetId", "pokemon-2", "moveId", "ember"))));
        assertThrows(IllegalArgumentException.class,
                () -> entityProjector.bindMoveAnimations(otherReservation, bindings));
    }

    @Test
    void requiresAuthoritativeTargetAndMoveIdentityOnMoveCommands() {
        BattlePresentationEntityBindings bindings = bindings();
        BattlePresentationBatch missingTarget = new BattlePresentationBatch(
                "battle-move-entity",
                List.of(new BattlePresentationCommand(
                        1, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("moveId", "ember"))));
        assertThrows(IllegalArgumentException.class,
                () -> entityProjector.bindMoveAnimations(missingTarget, bindings));

        assertThrows(IllegalArgumentException.class, () -> new EntityBoundMoveAnimation(
                new BattlePresentationCommand(
                        1, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("targetId", "pokemon-2")),
                "entity-a",
                "entity-b"));
    }

    private static BattlePresentationEntityBindings bindings() {
        return BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));
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
                "battle-move-entity", "player-1", trainer, List.of(first, second), List.of(), 1234L, arena);
    }
}
