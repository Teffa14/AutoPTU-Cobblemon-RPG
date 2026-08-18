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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCoreBootstrapAffiliationTest {
    @Test
    void canonicalBootstrapCoversRosterWithServerOwnedTeamIdentity() {
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-team",
                "player-red",
                new BattleTrainerSnapshot("player-red", Set.of(), Map.of(), 1),
                List.of(
                        pokemon("pokemon-1", "player-red", 20),
                        pokemon("pokemon-2", "player-red", 0)
                ),
                List.of(),
                77L
        );

        BattleCoreBootstrapProjection projection = BattleCoreBootstrapProjection.from(snapshot);

        assertEquals(Set.of("pokemon-1", "pokemon-2"), projection.affiliationByCombatant().keySet());
        assertEquals("player-red", projection.affiliationByCombatant().get("pokemon-1").teamId());
        assertEquals("player-red", projection.affiliationByCombatant().get("pokemon-2").teamId());
        assertTrue(projection.affiliationByCombatant().get("pokemon-1").active());
        assertTrue(projection.affiliationByCombatant().get("pokemon-2").active());
        assertEquals(0, projection.healthByCombatant().get("pokemon-2").currentHp());
        assertThrows(UnsupportedOperationException.class,
                () -> projection.affiliationByCombatant().put(
                        "client-injected",
                        new BattleCombatantAffiliationProjection("client-injected", "blue", true)));
    }

    @Test
    void rejectsInjectedOrMismatchedAffiliationState() {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-team",
                        1L,
                        Set.of("pokemon-1"),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of("client-injected",
                                new BattleCombatantAffiliationProjection("client-injected", "blue", true))));

        assertThrows(IllegalArgumentException.class,
                () -> new BattleCoreBootstrapProjection(
                        "battle-team",
                        1L,
                        Set.of("pokemon-1"),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of("pokemon-1",
                                new BattleCombatantAffiliationProjection("pokemon-other", "blue", true))));
    }

    private static BattlePokemonSnapshot pokemon(String pokemonId, String ownerPlayerId, int currentHp) {
        return new BattlePokemonSnapshot(
                pokemonId,
                ownerPlayerId,
                "eevee",
                10,
                Set.of(),
                Set.of(),
                new CanonicalCombatStats(10, 10, 10, 10, 10),
                new CanonicalHealth(currentHp, 20),
                new CanonicalMoveLoadout(List.of("Tackle")),
                null,
                1L
        );
    }
}
