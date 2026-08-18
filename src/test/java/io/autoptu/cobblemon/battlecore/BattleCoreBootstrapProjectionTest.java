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
    void projectsAuthoritativeRosterStatusesAndSeed() {
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
        assertEquals(Set.of("pokemon-burned", "pokemon-clean"), projection.combatantIds());
        assertEquals(Map.of("pokemon-burned", Set.of("burned")), projection.statusesByCombatant());
    }

    @Test
    void projectionDefensivelyCopiesRosterAndStatusCollections() {
        LinkedHashSet<String> mutableCombatants = new LinkedHashSet<>(Set.of("pokemon-1"));
        LinkedHashSet<String> mutableStatuses = new LinkedHashSet<>(Set.of("burned"));
        BattleCoreBootstrapProjection projection = new BattleCoreBootstrapProjection(
                "battle-43",
                22L,
                mutableCombatants,
                Map.of("pokemon-1", mutableStatuses)
        );

        mutableCombatants.add("pokemon-2");
        mutableStatuses.add("poisoned");

        assertEquals(Set.of("pokemon-1"), projection.combatantIds());
        assertEquals(Set.of("burned"), projection.statusesByCombatant().get("pokemon-1"));
        assertThrows(UnsupportedOperationException.class, () -> projection.combatantIds().add("pokemon-2"));
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
    void rejectsStatusStateForCombatantOutsideAuthoritativeRoster() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-44",
                        1L,
                        Set.of("pokemon-1"),
                        Map.of("client-injected", Set.of("burned"))
                )
        );
    }

    @Test
    void compatibilityConstructorTreatsStatusKeysAsItsLegacyRoster() {
        BattleCoreBootstrapProjection projection = new BattleCoreBootstrapProjection(
                "battle-45",
                2L,
                Map.of("pokemon-1", Set.of("paralyzed"))
        );

        assertEquals(Set.of("pokemon-1"), projection.combatantIds());
        assertEquals(Map.of("pokemon-1", Set.of("paralyzed")), projection.statusesByCombatant());
    }
}
