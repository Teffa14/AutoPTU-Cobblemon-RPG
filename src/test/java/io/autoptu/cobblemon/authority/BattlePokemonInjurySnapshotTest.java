package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonInjurySnapshotTest {
    @Test
    void freezesCanonicalInjuryCount() {
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1", "player-1", "pikachu", 12, Set.of(), Set.of(), CanonicalStatusState.empty(),
                null, null, null, null, null, null, new CanonicalInjuryState(3), null, 4L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertEquals(3, snapshot.injuryState().injuries());
    }

    @Test
    void rejectsNegativeInjuryCounts() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalInjuryState(-1));
    }

    @Test
    void legacyConstructorsDoNotInventTrustedInjuries() {
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1", "player-1", "pikachu", 12, Set.of(), null, 0L);

        assertNull(state.injuryState());
        assertNull(BattlePokemonSnapshot.from(state).injuryState());
    }
}
