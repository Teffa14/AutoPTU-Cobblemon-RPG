package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreBootstrapProjectionTest {
    @Test
    void projectsOnlyServerReservedPokemonStatusesAndSeed() {
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-42",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of("Ace Trainer"), Map.of("Command", 4), 8),
                List.of(
                        new BattlePokemonSnapshot(
                                "pokemon-burned",
                                "player-1",
                                "charizard",
                                27,
                                Set.of("Overland 5"),
                                Set.of("burned"),
                                null,
                                12),
                        new BattlePokemonSnapshot(
                                "pokemon-clean",
                                "player-1",
                                "wartortle",
                                24,
                                Set.of("Swim 5"),
                                Set.of(),
                                null,
                                7)
                ),
                List.of(),
                987654321L
        );

        BattleCoreBootstrapProjection projection = BattleCoreBootstrapProjection.from(snapshot);

        assertEquals("battle-42", projection.reservationId());
        assertEquals(987654321L, projection.rngSeed());
        assertEquals(Map.of("pokemon-burned", Set.of("burned")), projection.statusesByCombatant());
    }

    @Test
    void projectionDefensivelyCopiesStatusCollections() {
        LinkedHashSet<String> mutableStatuses = new LinkedHashSet<>(Set.of("burned"));
        BattleCoreBootstrapProjection projection = new BattleCoreBootstrapProjection(
                "battle-43",
                22L,
                Map.of("pokemon-1", mutableStatuses)
        );

        mutableStatuses.add("poisoned");

        assertEquals(Set.of("burned"), projection.statusesByCombatant().get("pokemon-1"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.statusesByCombatant().get("pokemon-1").add("frozen")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.statusesByCombatant().put("pokemon-2", Set.of("asleep"))
        );
    }

    @Test
    void rejectsUnidentifiedCombatants() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection("battle-44", 1L, Map.of(" ", Set.of("burned")))
        );
    }
}
