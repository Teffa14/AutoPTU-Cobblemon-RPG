package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreCombatantRosterProjectionTest {
    @Test
    void derivesCombatantIdsAndStatusesFromOneAuthoritativeSnapshot() {
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-50",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1),
                List.of(
                        new BattlePokemonSnapshot("pokemon-1", "player-1", "pikachu", 20, Set.of(), Set.of("paralyzed"), null, 2),
                        new BattlePokemonSnapshot("pokemon-2", "player-1", "squirtle", 18, Set.of(), Set.of(), null, 3)
                ),
                List.of(),
                55L
        );

        BattleCoreCombatantRosterProjection projection = BattleCoreCombatantRosterProjection.from(snapshot);

        assertEquals(Set.of("pokemon-1", "pokemon-2"), projection.combatantIds());
        assertEquals(Map.of("pokemon-1", Set.of("paralyzed")), projection.statusesByCombatant());
    }

    @Test
    void rejectsStatusStateForUnknownCombatant() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreCombatantRosterProjection(
                        Set.of("pokemon-1"),
                        Map.of("client-injected", Set.of("frozen"))
                )
        );
    }

    @Test
    void exportedCollectionsAreImmutable() {
        BattleCoreCombatantRosterProjection projection = new BattleCoreCombatantRosterProjection(
                Set.of("pokemon-1"),
                Map.of("pokemon-1", Set.of("asleep"))
        );

        assertThrows(UnsupportedOperationException.class, () -> projection.combatantIds().add("pokemon-2"));
        assertThrows(UnsupportedOperationException.class, () -> projection.statusesByCombatant().put("pokemon-2", Set.of()));
        assertThrows(UnsupportedOperationException.class, () -> projection.statusesByCombatant().get("pokemon-1").add("burned"));
    }
}
