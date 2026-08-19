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

class BattleWorldRelocationProjectorTest {
    private final BattleWorldRelocationProjector projector = new BattleWorldRelocationProjector();

    @Test
    void projectsAuthoritativeGridRelocationIntoFrozenWorldArena() {
        BattleAuthoritySnapshot snapshot = snapshot(true);
        BattlePresentationBatch batch = new BattlePresentationBatch("battle-1", List.of(
                new BattlePresentationCommand(
                        7, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, "pokemon-1",
                        Map.of("origin", "2,3", "destination", "4,3")),
                new BattlePresentationCommand(
                        8, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-1",
                        Map.of("damage", "5", "targetHp", "20"))));

        BattleWorldRelocationBatch projected = projector.project(snapshot, batch);

        assertEquals("battle-1", projected.reservationId());
        assertEquals(snapshot.arena(), projected.arena());
        assertEquals(1, projected.relocations().size());
        BattleWorldRelocation relocation = projected.relocations().getFirst();
        assertEquals("pokemon-1", relocation.combatantId());
        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 102, 64, 203), relocation.origin());
        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 104, 64, 203), relocation.destination());
    }

    @Test
    void rejectsPlaybackThatIsNotBoundToFrozenReservationAndRoster() {
        BattleAuthoritySnapshot snapshot = snapshot(true);
        BattlePresentationCommand relocation = new BattlePresentationCommand(
                1, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, "pokemon-1",
                Map.of("origin", "0,0", "destination", "1,0"));

        assertThrows(IllegalArgumentException.class, () ->
                projector.project(snapshot, new BattlePresentationBatch("other-battle", List.of(relocation))));

        BattlePresentationCommand injected = new BattlePresentationCommand(
                1, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, "pokemon-injected",
                Map.of("origin", "0,0", "destination", "1,0"));
        assertThrows(IllegalArgumentException.class, () ->
                projector.project(snapshot, new BattlePresentationBatch("battle-1", List.of(injected))));
    }

    @Test
    void refusesWorldProjectionWithoutFrozenArena() {
        BattleAuthoritySnapshot headless = snapshot(false);
        BattlePresentationBatch batch = new BattlePresentationBatch("battle-1", List.of(
                new BattlePresentationCommand(
                        1, 0, BattlePresentationCommand.Kind.ENTITY_RELOCATION, "pokemon-1",
                        Map.of("origin", "0,0", "destination", "1,0"))));

        assertThrows(IllegalArgumentException.class, () -> projector.project(headless, batch));
    }

    private static BattleAuthoritySnapshot snapshot(boolean withArena) {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot pokemon = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:charizard", 40, Set.of("Sky"), null, 2);
        BattleArenaSnapshot arena = withArena
                ? new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1)
                : null;
        return new BattleAuthoritySnapshot(
                "battle-1", "player-1", trainer, List.of(pokemon), List.of(), 1234L, arena);
    }
}
