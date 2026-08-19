package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleInitialPlacementSnapshotTest {
    @Test
    void freezesCompleteAuthoritativeRosterAndProjectsWorldAnchors() {
        BattleAuthoritySnapshot battle = battleWithArena();
        LinkedHashMap<String, BattleGridCoordinate> anchors = new LinkedHashMap<>();
        anchors.put("pokemon-a", new BattleGridCoordinate(2, 3));
        anchors.put("pokemon-b", new BattleGridCoordinate(-1, 4));

        BattleInitialPlacementSnapshot snapshot = BattleInitialPlacementSnapshot.from(battle, anchors);
        anchors.put("pokemon-a", new BattleGridCoordinate(99, 99));

        assertEquals(new BattleGridCoordinate(2, 3), snapshot.placementsByCombatant().get("pokemon-a").anchor());
        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 103, 70, 198), snapshot.worldAnchor(battle, "pokemon-a"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.placementsByCombatant().put(
                "pokemon-c", new BattleCombatantInitialPlacement("pokemon-c", new BattleGridCoordinate(0, 0))));
    }

    @Test
    void rejectsMissingOrInjectedCombatants() {
        BattleAuthoritySnapshot battle = battleWithArena();

        assertThrows(IllegalArgumentException.class, () -> BattleInitialPlacementSnapshot.from(
                battle,
                Map.of("pokemon-a", new BattleGridCoordinate(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> BattleInitialPlacementSnapshot.from(
                battle,
                Map.of(
                        "pokemon-a", new BattleGridCoordinate(0, 0),
                        "pokemon-b", new BattleGridCoordinate(1, 0),
                        "forged", new BattleGridCoordinate(2, 0))));
    }

    @Test
    void rejectsHeadlessReservationBecauseWorldPlacementNeedsFrozenArena() {
        BattleAuthoritySnapshot battle = new BattleAuthoritySnapshot(
                "reservation-1",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 0),
                List.of(pokemon("pokemon-a")),
                List.of(),
                42L);

        assertThrows(IllegalArgumentException.class, () -> BattleInitialPlacementSnapshot.from(
                battle, Map.of("pokemon-a", new BattleGridCoordinate(0, 0))));
    }

    @Test
    void rejectsMismatchedEmbeddedCombatantIdentityAndReservation() {
        assertThrows(IllegalArgumentException.class, () -> new BattleInitialPlacementSnapshot(
                "reservation-1",
                Map.of("pokemon-a", new BattleCombatantInitialPlacement(
                        "pokemon-b", new BattleGridCoordinate(0, 0)))));

        BattleInitialPlacementSnapshot snapshot = BattleInitialPlacementSnapshot.from(
                battleWithArena(),
                Map.of(
                        "pokemon-a", new BattleGridCoordinate(0, 0),
                        "pokemon-b", new BattleGridCoordinate(1, 0)));
        BattleAuthoritySnapshot other = new BattleAuthoritySnapshot(
                "reservation-2",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 0),
                List.of(pokemon("pokemon-a"), pokemon("pokemon-b")),
                List.of(),
                43L,
                new BattleArenaSnapshot("minecraft:overworld", 100, 70, 200, 0, -1, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> snapshot.worldAnchor(other, "pokemon-a"));
    }

    private static BattleAuthoritySnapshot battleWithArena() {
        return new BattleAuthoritySnapshot(
                "reservation-1",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 0),
                List.of(pokemon("pokemon-a"), pokemon("pokemon-b")),
                List.of(),
                42L,
                new BattleArenaSnapshot("minecraft:overworld", 100, 70, 200, 0, -1, 1, 0));
    }

    private static BattlePokemonSnapshot pokemon(String id) {
        return new BattlePokemonSnapshot(id, "player-1", "cobblemon:test", 10, Set.of(), null, 0);
    }
}
