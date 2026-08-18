package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreBootstrapProjectionTest {
    @Test
    void projectsAuthoritativeRosterStatusesStatsAndSeed() {
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
                                new CanonicalCombatStats(74, 63, 92, 71, 85),
                                null,
                                12),
                        new BattlePokemonSnapshot(
                                "pokemon-clean",
                                "player-1",
                                "wartortle",
                                24,
                                Set.of("Swim 5"),
                                Set.of(),
                                new CanonicalCombatStats(58, 76, 65, 72, 50),
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
        assertEquals(Set.of("pokemon-burned", "pokemon-clean"), projection.combatStatsByCombatant().keySet());
        assertEquals(92, projection.combatStatsByCombatant().get("pokemon-burned").spatk());
        assertEquals(76, projection.combatStatsByCombatant().get("pokemon-clean").def());
    }

    @Test
    void canonicalBootstrapRejectsAnyRosterMemberWithoutFrozenStats() {
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-missing-stats",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1),
                List.of(
                        new BattlePokemonSnapshot(
                                "pokemon-ready",
                                "player-1",
                                "eevee",
                                10,
                                Set.of(),
                                Set.of(),
                                new CanonicalCombatStats(10, 10, 10, 10, 10),
                                null,
                                1),
                        new BattlePokemonSnapshot(
                                "pokemon-legacy",
                                "player-1",
                                "pikachu",
                                10,
                                Set.of(),
                                null,
                                2)
                ),
                List.of(),
                9L
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleCoreBootstrapProjection.from(snapshot)
        );
        assertEquals(
                "canonical combat stats are required for combatant: pokemon-legacy",
                error.getMessage()
        );
    }

    @Test
    void projectionDefensivelyCopiesRosterStatusAndStatCollections() {
        LinkedHashSet<String> mutableCombatants = new LinkedHashSet<>(Set.of("pokemon-1"));
        LinkedHashSet<String> mutableStatuses = new LinkedHashSet<>(Set.of("burned"));
        BattleCombatantStatProjection stats = new BattleCombatantStatProjection("pokemon-1", 20, 21, 22, 23, 24);
        BattleCoreBootstrapProjection projection = new BattleCoreBootstrapProjection(
                "battle-43",
                22L,
                mutableCombatants,
                Map.of("pokemon-1", mutableStatuses),
                Map.of("pokemon-1", stats)
        );

        mutableCombatants.add("pokemon-2");
        mutableStatuses.add("poisoned");

        assertEquals(Set.of("pokemon-1"), projection.combatantIds());
        assertEquals(Set.of("burned"), projection.statusesByCombatant().get("pokemon-1"));
        assertEquals(stats, projection.combatStatsByCombatant().get("pokemon-1"));
        assertThrows(UnsupportedOperationException.class, () -> projection.combatantIds().add("pokemon-2"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.statusesByCombatant().get("pokemon-1").add("frozen")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.statusesByCombatant().put("pokemon-2", Set.of("asleep"))
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.combatStatsByCombatant().put("pokemon-2", stats)
        );
    }

    @Test
    void rejectsStateForCombatantOutsideAuthoritativeRoster() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-44",
                        1L,
                        Set.of("pokemon-1"),
                        Map.of("client-injected", Set.of("burned")),
                        Map.of()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-44",
                        1L,
                        Set.of("pokemon-1"),
                        Map.of(),
                        Map.of(
                                "client-injected",
                                new BattleCombatantStatProjection("client-injected", 1, 1, 1, 1, 1)
                        )
                )
        );
    }

    @Test
    void rejectsCombatStatProjectionWhoseEmbeddedIdDoesNotMatchMapKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-46",
                        3L,
                        Set.of("pokemon-1"),
                        Map.of(),
                        Map.of(
                                "pokemon-1",
                                new BattleCombatantStatProjection("pokemon-other", 1, 1, 1, 1, 1)
                        )
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
        assertEquals(Map.of(), projection.combatStatsByCombatant());
    }
}
