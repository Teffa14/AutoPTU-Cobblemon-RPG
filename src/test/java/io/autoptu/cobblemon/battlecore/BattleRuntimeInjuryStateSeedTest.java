package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRuntimeInjuryStateSeedTest {
    @Test
    void carriesOnlyCurrentCanonicalInjuriesForExactRoster() {
        BattleRuntimeInjuryStateSeed seed = new BattleRuntimeInjuryStateSeed(
                " battle-1 ",
                Set.of("mon-1", "mon-2"),
                Map.of("mon-1", 2, "mon-2", 0)
        );

        assertEquals("battle-1", seed.reservationId());
        assertEquals(Set.of("mon-1", "mon-2"), seed.combatantRoster());
        assertEquals(Map.of("mon-1", 2, "mon-2", 0), seed.currentInjuriesByCombatant());
        assertThrows(UnsupportedOperationException.class,
                () -> seed.currentInjuriesByCombatant().put("mon-3", 1));
    }

    @Test
    void rejectsIncompleteInjectedOrNegativeInjuryState() {
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeInjuryStateSeed(
                "battle-1", Set.of("mon-1", "mon-2"), Map.of("mon-1", 1)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeInjuryStateSeed(
                "battle-1", Set.of("mon-1"), Map.of("other", 1)
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeInjuryStateSeed(
                "battle-1", Set.of("mon-1"), Map.of("mon-1", -1)
        ));
    }

    @Test
    void contractCannotAcceptRoundHistoryFromMinecraft() {
        Set<String> componentNames = Arrays.stream(BattleRuntimeInjuryStateSeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(componentNames.contains("currentInjuriesByCombatant"));
        assertFalse(componentNames.contains("injuriesLastRound"));
        assertFalse(componentNames.contains("injuriesPreviousRound"));
    }
}
