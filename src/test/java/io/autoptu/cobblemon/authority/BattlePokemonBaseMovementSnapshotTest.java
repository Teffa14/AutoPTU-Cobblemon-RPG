package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonBaseMovementSnapshotTest {
    @Test
    void freezesServerOwnedBaseMovementIntoBattleSnapshot() {
        CanonicalBaseMovement movement = new CanonicalBaseMovement(7, 4, 0, 2, 1);
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1",
                "player-1",
                "eevee",
                12,
                Set.of("Naturewalk"),
                Set.of(),
                new CanonicalCombatStats(8, 7, 9, 8, 10),
                new CanonicalHealth(31, 31),
                new CanonicalMoveLoadout(java.util.List.of("tackle")),
                movement,
                null,
                3
        );

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertEquals(movement, snapshot.baseMovement());
        assertEquals(7, snapshot.baseMovement().overland());
        assertEquals(4, snapshot.baseMovement().swim());
        assertEquals(2, snapshot.baseMovement().longJump());
    }

    @Test
    void rejectsNegativePersistentMovementValues() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBaseMovement(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBaseMovement(0, 0, 0, 0, -1));
    }

    @Test
    void legacyPokemonStateCarriesNoInventedTrustedMovement() {
        CanonicalPokemonState legacy = new CanonicalPokemonState(
                "pokemon-legacy",
                "player-1",
                "pikachu",
                5,
                Set.of("Overland 6"),
                0
        );

        assertNull(legacy.baseMovement());
        assertNull(BattlePokemonSnapshot.from(legacy).baseMovement());
    }
}
