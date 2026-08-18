package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonStatusSnapshotTest {
    @Test
    void canonicalStatusesAreNormalizedAndFrozenIntoBattleSnapshot() {
        LinkedHashSet<String> supplied = new LinkedHashSet<>();
        supplied.add(" Burned ");
        supplied.add("POISONED");

        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "charizard", 35,
                Set.of("overland-6"), supplied, "item-1", 9L);
        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(canonical);

        supplied.clear();

        assertEquals(Set.of("burned", "poisoned"), canonical.statuses());
        assertEquals(Set.of("burned", "poisoned"), snapshot.statuses());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.statuses().add("frozen"));
    }

    @Test
    void legacyConstructorsDefaultToNoStatuses() {
        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 12, Set.of(), "item-1", 3L);
        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(canonical);

        assertFalse(canonical.statuses().iterator().hasNext());
        assertFalse(snapshot.statuses().iterator().hasNext());
    }
}
