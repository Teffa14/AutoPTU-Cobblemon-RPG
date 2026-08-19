package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreTrainerRuntimeBootstrapProjectionTest {
    @Test
    void freezesCanonicalTrainerFeaturesApAndControllerBindings() {
        CanonicalPlayerState player = new CanonicalPlayerState(
                "trainer-1",
                Set.of("Ace Trainer"),
                Map.of("Command", 4),
                Set.of("Overland"),
                Set.of("Defense Mastery", "Stat Mastery"),
                3,
                9
        );
        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "reservation-1",
                "trainer-1",
                BattleTrainerSnapshot.from(player),
                List.of(
                        pokemon("pokemon-1", "trainer-1"),
                        pokemon("pokemon-2", "trainer-1")
                ),
                List.of(),
                42L
        );

        BattleCoreTrainerRuntimeBootstrapProjection projection =
                BattleCoreTrainerRuntimeBootstrapProjection.from(snapshot);

        assertEquals("reservation-1", projection.reservationId());
        assertEquals("trainer-1", projection.trainer().trainerId());
        assertEquals(Set.of("Defense Mastery", "Stat Mastery"), projection.trainer().trainerFeatures());
        assertEquals(3, projection.trainer().actionPoints());
        assertEquals(Set.of("pokemon-1", "pokemon-2"), projection.trainer().controlledCombatantIds());
    }

    @Test
    void legacyPlayerStateCarriesNoInventedBattleAp() {
        CanonicalPlayerState legacy = new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of("Defense Mastery"), 4
        );
        assertEquals(0, legacy.actionPoints());
        assertEquals(0, BattleTrainerSnapshot.from(legacy).actionPoints());
    }

    @Test
    void negativeCanonicalApFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of(), -1, 1
        ));
    }

    @Test
    void trainerRuntimeProjectionRequiresControlledCombatants() {
        assertThrows(IllegalArgumentException.class, () -> new BattleTrainerRuntimeProjection(
                "trainer-1", Set.of("Defense Mastery"), 2, Set.of()
        ));
    }

    private static BattlePokemonSnapshot pokemon(String pokemonId, String ownerId) {
        return new BattlePokemonSnapshot(
                pokemonId,
                ownerId,
                "species:test",
                10,
                Set.of(),
                null,
                1
        );
    }
}
