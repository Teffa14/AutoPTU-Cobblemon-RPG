package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCombatantHealthProjectionTest {
    @Test
    void projectsFrozenCanonicalHealth() {
        BattlePokemonSnapshot snapshot = new BattlePokemonSnapshot(
                "pokemon-1", "trainer-1", "pikachu", 12, Set.of(), Set.of(),
                new CanonicalCombatStats(8, 7, 9, 8, 11), new CanonicalHealth(37, 52), null, 4L);

        BattleCombatantHealthProjection projection = BattleCombatantHealthProjection.from(snapshot);

        assertEquals("pokemon-1", projection.combatantId());
        assertEquals(37, projection.currentHp());
        assertEquals(52, projection.maxHp());
    }

    @Test
    void rejectsSnapshotWithoutCanonicalHealth() {
        BattlePokemonSnapshot snapshot = new BattlePokemonSnapshot(
                "pokemon-legacy", "trainer-1", "eevee", 8, Set.of(), Set.of(),
                new CanonicalCombatStats(8, 8, 8, 8, 8), null, 1L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleCombatantHealthProjection.from(snapshot));
        assertEquals("canonical health is required for combatant: pokemon-legacy", error.getMessage());
    }

    @Test
    void validatesHealthBoundsAtProjectionBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCombatantHealthProjection("pokemon-1", -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCombatantHealthProjection("pokemon-1", 11, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleCombatantHealthProjection("pokemon-1", 0, 0));
    }
}
