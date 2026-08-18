package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreBootstrapMoveLoadoutTest {

    @Test
    void projectsFrozenMoveLoadoutsForEveryReservedCombatant() {
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-1",
                "trainer-1",
                new BattleTrainerSnapshot("trainer-1", Set.of(), Map.of(), 3),
                List.of(
                        pokemon("pkm-1", new CanonicalMoveLoadout(List.of("Tackle", "Growl"))),
                        pokemon("pkm-2", new CanonicalMoveLoadout(List.of("Ember")))
                ),
                List.of(),
                12345L
        );

        BattleCoreBootstrapProjection projection = BattleCoreBootstrapProjection.from(snapshot);

        assertEquals(Set.of("pkm-1", "pkm-2"), projection.moveLoadoutsByCombatant().keySet());
        assertEquals(List.of("Tackle", "Growl"), projection.moveLoadoutsByCombatant().get("pkm-1").moveIds());
        assertEquals(List.of("Ember"), projection.moveLoadoutsByCombatant().get("pkm-2").moveIds());
        assertThrows(UnsupportedOperationException.class,
                () -> projection.moveLoadoutsByCombatant().put(
                        "pkm-3", new BattleCombatantMoveLoadoutProjection("pkm-3", List.of("Scratch"))));
        assertThrows(UnsupportedOperationException.class,
                () -> projection.moveLoadoutsByCombatant().get("pkm-1").moveIds().add("Scratch"));
    }

    @Test
    void rejectsLegacyRosterMemberWithoutCanonicalMoveLoadout() {
        BattlePokemonSnapshot legacyPokemon = new BattlePokemonSnapshot(
                "pkm-legacy",
                "trainer-1",
                "eevee",
                12,
                Set.of("Overland"),
                Set.of(),
                new CanonicalCombatStats(10, 11, 12, 13, 14),
                new CanonicalHealth(20, 20),
                null,
                5
        );
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-legacy",
                "trainer-1",
                new BattleTrainerSnapshot("trainer-1", Set.of(), Map.of(), 3),
                List.of(legacyPokemon),
                List.of(),
                99L
        );

        assertThrows(IllegalArgumentException.class, () -> BattleCoreBootstrapProjection.from(snapshot));
    }

    @Test
    void rejectsMoveLoadoutForCombatantOutsideReservedRoster() {
        Map<String, BattleCombatantMoveLoadoutProjection> injected = Map.of(
                "client-injected",
                new BattleCombatantMoveLoadoutProjection("client-injected", List.of("Hyper Beam"))
        );

        assertThrows(IllegalArgumentException.class, () -> new BattleCoreBootstrapProjection(
                "battle-1",
                7L,
                Set.of("pkm-1"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                injected
        ));
    }

    @Test
    void rejectsMoveLoadoutWhoseEmbeddedCombatantIdDoesNotMatchMapKey() {
        Map<String, BattleCombatantMoveLoadoutProjection> mismatched = Map.of(
                "pkm-1",
                new BattleCombatantMoveLoadoutProjection("pkm-2", List.of("Tackle"))
        );

        assertThrows(IllegalArgumentException.class, () -> new BattleCoreBootstrapProjection(
                "battle-1",
                7L,
                Set.of("pkm-1"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                mismatched
        ));
    }

    private static BattlePokemonSnapshot pokemon(String id, CanonicalMoveLoadout loadout) {
        return new BattlePokemonSnapshot(
                id,
                "trainer-1",
                "charmander",
                10,
                Set.of("Overland"),
                Set.of(),
                new CanonicalCombatStats(10, 11, 12, 13, 14),
                new CanonicalHealth(20, 20),
                loadout,
                null,
                5
        );
    }
}
