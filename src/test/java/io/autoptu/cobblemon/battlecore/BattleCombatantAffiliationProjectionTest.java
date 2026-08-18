package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCombatantAffiliationProjectionTest {
    @Test
    void derivesTeamFromFrozenCanonicalOwner() {
        BattlePokemonSnapshot snapshot = new BattlePokemonSnapshot(
                "pokemon-1",
                "trainer-server-id",
                "pikachu",
                12,
                Set.of(),
                Set.of(),
                new CanonicalCombatStats(8, 7, 9, 8, 11),
                new CanonicalHealth(37, 52),
                null,
                4L
        );

        BattleCombatantAffiliationProjection projection = BattleCombatantAffiliationProjection.from(snapshot);

        assertEquals("pokemon-1", projection.combatantId());
        assertEquals("trainer-server-id", projection.teamId());
        assertTrue(projection.active());
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCombatantAffiliationProjection(" ", "team-1", true));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCombatantAffiliationProjection("pokemon-1", " ", true));
    }
}
