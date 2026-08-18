package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonCombatStatsSnapshotTest {
    @Test
    void canonicalCombatStatsAreFrozenIntoBattleSnapshot() {
        CanonicalCombatStats stats = new CanonicalCombatStats(73, 61, 84, 72, 95);
        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "jolteon", 30,
                Set.of("overland-8"), Set.of("paralyzed"), stats, "item-1", 4L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(canonical);

        assertEquals(stats, canonical.combatStats());
        assertEquals(stats, snapshot.combatStats());
        assertEquals(73, snapshot.combatStats().atk());
        assertEquals(61, snapshot.combatStats().def());
        assertEquals(84, snapshot.combatStats().spatk());
        assertEquals(72, snapshot.combatStats().spdef());
        assertEquals(95, snapshot.combatStats().spd());
    }

    @Test
    void combatStatsRejectNonPositiveCanonicalValues() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalCombatStats(0, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalCombatStats(1, -1, 1, 1, 1));
    }

    @Test
    void legacyPokemonConstructorsRemainExplicitlyUnspecified() {
        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 12, Set.of(), "item-1", 3L);
        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(canonical);

        assertNull(canonical.combatStats());
        assertNull(snapshot.combatStats());
    }
}
