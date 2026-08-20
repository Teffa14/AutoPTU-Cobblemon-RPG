package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlePresentationEntityBindingsTest {
    @Test
    void bindsEveryFrozenCombatantToExactlyOneOpaquePresentationEntity() {
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        assertEquals("battle-bindings", bindings.reservationId());
        assertEquals("entity-a", bindings.requireBinding("battle-bindings", "pokemon-1").presentationEntityId());
        assertEquals("entity-b", bindings.requireBinding("battle-bindings", "pokemon-2").presentationEntityId());
        assertThrows(UnsupportedOperationException.class, () -> bindings.byCombatant().clear());
    }

    @Test
    void rejectsIncompleteInjectedDuplicateAndCrossReservationBindings() {
        BattleAuthoritySnapshot snapshot = snapshot();
        assertThrows(IllegalArgumentException.class, () -> BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a")));
        assertThrows(IllegalArgumentException.class, () -> BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b", "pokemon-3", "entity-c")));
        assertThrows(IllegalArgumentException.class, () -> BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-a")));

        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));
        assertThrows(IllegalArgumentException.class, () -> bindings.requireBinding("other-battle", "pokemon-1"));
        assertThrows(IllegalArgumentException.class, () -> bindings.requireBinding("battle-bindings", "pokemon-3"));
    }

    @Test
    void defensivelyCopiesAdapterSuppliedBindingMapAndCarriesNoRuleState() {
        HashMap<String, String> input = new HashMap<>();
        input.put("pokemon-1", "entity-a");
        input.put("pokemon-2", "entity-b");
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(snapshot(), input);
        input.put("pokemon-1", "entity-forged");

        assertEquals("entity-a", bindings.requireBinding("battle-bindings", "pokemon-1").presentationEntityId());
        Set<String> componentNames = Set.of(PresentationEntityBinding.class.getRecordComponents()[0].getName(),
                PresentationEntityBinding.class.getRecordComponents()[1].getName());
        assertTrue(componentNames.containsAll(Set.of("combatantId", "presentationEntityId")));
        assertEquals(2, componentNames.size());
    }

    private static BattleAuthoritySnapshot snapshot() {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot first = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:charizard", 40, Set.of("Sky"), null, 2);
        BattlePokemonSnapshot second = new BattlePokemonSnapshot(
                "pokemon-2", "player-1", "cobblemon:blastoise", 40, Set.of("Swim"), null, 2);
        return new BattleAuthoritySnapshot(
                "battle-bindings", "player-1", trainer, List.of(first, second), List.of(), 1234L);
    }
}
