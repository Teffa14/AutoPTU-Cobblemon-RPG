package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonHealthSnapshotTest {
    @Test
    void freezesCanonicalHealthIntoBattleSnapshot() {
        CanonicalHealth health = new CanonicalHealth(37, 52);
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1",
                "trainer-1",
                "pikachu",
                12,
                Set.of("Overland"),
                Set.of("burned"),
                new CanonicalCombatStats(8, 7, 9, 8, 11),
                health,
                null,
                4L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertEquals(health, snapshot.health());
        assertEquals(37, snapshot.health().currentHp());
        assertEquals(52, snapshot.health().maxHp());
    }

    @Test
    void rejectsInvalidCanonicalHealth() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalHealth(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalHealth(11, 10));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalHealth(0, 0));
    }

    @Test
    void legacyPokemonConstructorsRemainExplicitlyWithoutCanonicalHealth() {
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-legacy",
                "trainer-1",
                "eevee",
                8,
                Set.of(),
                null,
                0L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertNull(state.health());
        assertNull(snapshot.health());
    }
}
