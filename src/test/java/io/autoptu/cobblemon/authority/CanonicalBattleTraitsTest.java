package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalBattleTraitsTest {
    @Test
    void freezesOrderedTypesAndAbilityIdentitiesIntoBattleSnapshot() {
        CanonicalBattleTraits traits = new CanonicalBattleTraits(
                List.of(" Fire ", "Flying"),
                List.of(" Blaze ", "Solar Power")
        );
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1", "player-1", "charizard", 36, Set.of(), Set.of(),
                null, null, null, null, traits, null, 7L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertEquals(List.of("Fire", "Flying"), snapshot.battleTraits().types());
        assertEquals(List.of("Blaze", "Solar Power"), snapshot.battleTraits().abilities());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.battleTraits().types().add("Dragon"));
    }

    @Test
    void failsClosedOnMissingTypesAndMalformedIdentities() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBattleTraits(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBattleTraits(List.of("Fire", " "), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBattleTraits(List.of("Fire", "Fire"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBattleTraits(List.of("Fire"), List.of("Blaze", "Blaze")));
    }

    @Test
    void legacyPokemonConstructorsDoNotInventTrustedTraits() {
        CanonicalPokemonState legacy = new CanonicalPokemonState(
                "pokemon-1", "player-1", "charmander", 5, Set.of(), 1L);
        assertNull(legacy.battleTraits());
        assertNull(BattlePokemonSnapshot.from(legacy).battleTraits());
    }
}
