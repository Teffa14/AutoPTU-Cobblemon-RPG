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
    void freezesCanonicalTrainerFeaturesSkillsApInitiativeAndControllerBindings() {
        CanonicalPlayerState player = new CanonicalPlayerState(
                "trainer-1",
                Set.of("Ace Trainer"),
                Map.of("Command", 4, "Intimidate", 6),
                Set.of("Overland"),
                Set.of("Defense Mastery", "Stat Mastery", "Press On!"),
                3,
                -2,
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
        assertEquals(Set.of("Defense Mastery", "Stat Mastery", "Press On!"), projection.trainer().trainerFeatures());
        assertEquals(Map.of("Command", 4, "Intimidate", 6), projection.trainer().skillRanks());
        assertEquals(3, projection.trainer().actionPoints());
        assertEquals(-2, projection.trainer().initiativeModifier());
        assertEquals(Set.of("pokemon-1", "pokemon-2"), projection.trainer().controlledCombatantIds());
        assertThrows(UnsupportedOperationException.class,
                () -> projection.trainer().skillRanks().put("Intimidate", 1));
    }

    @Test
    void legacyProjectionCarriesPythonDefaultsForBattleApInitiativeAndSkills() {
        CanonicalPlayerState legacy = new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of("Defense Mastery"), 4
        );
        assertEquals(0, legacy.actionPoints());
        assertEquals(0, legacy.initiativeModifier());
        assertEquals(0, BattleTrainerSnapshot.from(legacy).actionPoints());
        assertEquals(0, BattleTrainerSnapshot.from(legacy).initiativeModifier());

        BattleTrainerRuntimeProjection projection = new BattleTrainerRuntimeProjection(
                "trainer-1", Set.of("Defense Mastery"), 0, 0, Set.of("pokemon-1"));
        assertEquals(Map.of(), projection.skillRanks());
    }

    @Test
    void negativeCanonicalApFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of(), -1, 1
        ));
    }

    @Test
    void trainerRuntimeProjectionRejectsMissingCombatantsAndDuplicateSkillIdentities() {
        assertThrows(IllegalArgumentException.class, () -> new BattleTrainerRuntimeProjection(
                "trainer-1", Set.of("Defense Mastery"), 2, 1, Set.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleTrainerRuntimeProjection(
                "trainer-1", Set.of("Press On!"), 2, 1,
                Map.of("Intimidate", 6, " intimidate ", 5), Set.of("pokemon-1")
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
