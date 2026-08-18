package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCombatantStatProjectionTest {
    @Test
    void projectsOnlyCanonicalFrozenBaseStats() {
        BattlePokemonSnapshot snapshot = new BattlePokemonSnapshot(
                "pokemon-1",
                "player-1",
                "lucario",
                32,
                Set.of("Overland 7"),
                Set.of("burned"),
                new CanonicalCombatStats(81, 70, 92, 74, 88),
                "item-1",
                6L
        );

        BattleCombatantStatProjection projection = BattleCombatantStatProjection.from(snapshot);

        assertEquals("pokemon-1", projection.combatantId());
        assertEquals(81, projection.atk());
        assertEquals(70, projection.def());
        assertEquals(92, projection.spatk());
        assertEquals(74, projection.spdef());
        assertEquals(88, projection.spd());
    }

    @Test
    void rejectsLegacySnapshotWithoutCanonicalStats() {
        BattlePokemonSnapshot snapshot = new BattlePokemonSnapshot(
                "pokemon-legacy",
                "player-1",
                "pikachu",
                14,
                Set.of(),
                null,
                2L
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleCombatantStatProjection.from(snapshot)
        );
        assertEquals(
                "canonical combat stats are required for combatant: pokemon-legacy",
                error.getMessage()
        );
    }

    @Test
    void directConstructionRejectsUntrustedNonPositiveValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCombatantStatProjection("pokemon-1", 0, 1, 1, 1, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCombatantStatProjection("pokemon-1", 1, 1, 1, 1, -1)
        );
    }
}
