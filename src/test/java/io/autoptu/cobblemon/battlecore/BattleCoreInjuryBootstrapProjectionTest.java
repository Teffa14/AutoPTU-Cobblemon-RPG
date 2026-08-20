package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreInjuryBootstrapProjectionTest {
    @Test
    void bindsInjuriesToExactBootstrapRoster() {
        BattleCoreBootstrapProjection bootstrap = new BattleCoreBootstrapProjection(
                "reservation-1", 7L, Set.of("pokemon-1"), Map.of());
        BattleCombatantInjuryProjection injury = new BattleCombatantInjuryProjection("pokemon-1", 2);

        BattleCoreInjuryBootstrapProjection projection = new BattleCoreInjuryBootstrapProjection(
                "reservation-1", bootstrap, Map.of("pokemon-1", injury));

        assertEquals(2, projection.injuriesByCombatant().get("pokemon-1").injuries());
        assertThrows(UnsupportedOperationException.class, () -> projection.injuriesByCombatant().put(
                "pokemon-2", new BattleCombatantInjuryProjection("pokemon-2", 0)));
    }

    @Test
    void rejectsMissingInjectedOrMismatchedInjuryEntries() {
        BattleCoreBootstrapProjection bootstrap = new BattleCoreBootstrapProjection(
                "reservation-1", 7L, Set.of("pokemon-1"), Map.of());

        assertThrows(IllegalArgumentException.class, () -> new BattleCoreInjuryBootstrapProjection(
                "reservation-1", bootstrap, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreInjuryBootstrapProjection(
                "reservation-1", bootstrap,
                Map.of("pokemon-2", new BattleCombatantInjuryProjection("pokemon-2", 1))));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreInjuryBootstrapProjection(
                "reservation-1", bootstrap,
                Map.of("pokemon-1", new BattleCombatantInjuryProjection("pokemon-2", 1))));
    }

    @Test
    void rejectsCrossReservationBootstrap() {
        BattleCoreBootstrapProjection bootstrap = new BattleCoreBootstrapProjection(
                "reservation-2", 7L, Set.of("pokemon-1"), Map.of());

        assertThrows(IllegalArgumentException.class, () -> new BattleCoreInjuryBootstrapProjection(
                "reservation-1", bootstrap,
                Map.of("pokemon-1", new BattleCombatantInjuryProjection("pokemon-1", 1))));
    }
}
