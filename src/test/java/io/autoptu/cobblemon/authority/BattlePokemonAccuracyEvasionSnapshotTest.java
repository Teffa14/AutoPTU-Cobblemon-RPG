package io.autoptu.cobblemon.authority;

import io.autoptu.cobblemon.battlecore.BattleCombatantAccuracyEvasionProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePokemonAccuracyEvasionSnapshotTest {
    @Test
    void freezesPythonOracleBaselineInputsWithoutResolvingDynamicEffects() {
        CanonicalAccuracyEvasion accuracy = new CanonicalAccuracyEvasion(2, 1, 3, 4);
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1", "player-1", "pikachu", 20, Set.of("Overland"), Set.of("burned"),
                new CanonicalCombatStats(12, 11, 14, 13, 15),
                new CanonicalHealth(30, 40),
                new CanonicalMoveLoadout(List.of("Thunderbolt")),
                new CanonicalBaseMovement(6, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("Electric"), List.of("Static")),
                accuracy,
                null,
                7L);

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);
        assertEquals(accuracy, snapshot.accuracyEvasion());

        BattleCombatantAccuracyEvasionProjection projection = BattleCombatantAccuracyEvasionProjection.from(snapshot);
        assertEquals("pokemon-1", projection.combatantId());
        assertEquals(2, projection.accuracyStage());
        assertEquals(1, projection.physicalEvasionBonus());
        assertEquals(3, projection.specialEvasionBonus());
        assertEquals(4, projection.statusEvasionBonus());
    }

    @Test
    void rejectsAccuracyStagesOutsidePtuBounds() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalAccuracyEvasion(7, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalAccuracyEvasion(-7, 0, 0, 0));
    }

    @Test
    void legacyPokemonDoesNotInventTrustedAccuracyOrEvasion() {
        CanonicalPokemonState legacy = new CanonicalPokemonState(
                "pokemon-1", "player-1", "pikachu", 20, Set.of("Overland"), 1L);
        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(legacy);
        assertNull(snapshot.accuracyEvasion());
        assertThrows(IllegalArgumentException.class,
                () -> BattleCombatantAccuracyEvasionProjection.from(snapshot));
    }
}
