package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalStatusMetadataTest {
    @Test
    void freezesOrderedScalarMetadataIntoBattleSnapshot() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("applied_round", 7);
        payload.put("source", "move:fake-out");
        CanonicalStatusState statusState = new CanonicalStatusState(List.of(
                new CanonicalStatusEntry(" Flinched ", payload),
                new CanonicalStatusEntry("Burned")
        ));

        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 20,
                Set.of(), Set.of("flinched", "burned"), statusState,
                null, null, null, null, null, null, null, 4L);
        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(canonical);
        payload.put("applied_round", 99);

        assertEquals(List.of("flinched", "burned"), snapshot.statusState().entries().stream().map(CanonicalStatusEntry::name).toList());
        assertEquals(7, snapshot.statusState().entries().get(0).payload().get("applied_round"));
        assertEquals("move:fake-out", snapshot.statusState().entries().get(0).payload().get("source"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.statusState().entries().get(0).payload().put("x", 1));
    }

    @Test
    void legacyStatusNamesRemainMetadataFreeAndConsistent() {
        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 20,
                Set.of(), Set.of(" Sleep "), null, null, null, null, null, null, 1L);

        assertEquals(Set.of("sleep"), canonical.statuses());
        assertEquals(List.of("sleep"), canonical.statusState().entries().stream().map(CanonicalStatusEntry::name).toList());
        assertTrue(canonical.statusState().entries().get(0).payload().isEmpty());
    }

    @Test
    void preservesOrderedStackedStatusesWhileLegacyNameViewRemainsUnique() {
        CanonicalStatusState stacked = new CanonicalStatusState(List.of(
                new CanonicalStatusEntry("flinch", Map.of("source", "move:a")),
                new CanonicalStatusEntry("Burned"),
                new CanonicalStatusEntry("FLINCH", Map.of("source", "trainer_feature:b"))
        ));
        CanonicalPokemonState canonical = new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 20,
                Set.of(), Set.of("flinch", "burned"), stacked,
                null, null, null, null, null, null, null, 1L);

        assertEquals(List.of("flinch", "burned", "flinch"),
                canonical.statusState().entries().stream().map(CanonicalStatusEntry::name).toList());
        assertEquals(Set.of("flinch", "burned"), canonical.statuses());
        assertEquals("move:a", canonical.statusState().entries().get(0).payload().get("source"));
        assertEquals("trainer_feature:b", canonical.statusState().entries().get(2).payload().get("source"));
    }

    @Test
    void rejectsNameDriftAndNonScalarPayloads() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalStatusEntry("flinch", Map.of("nested", List.of(1))));
        CanonicalStatusState flinch = new CanonicalStatusState(List.of(new CanonicalStatusEntry("flinch")));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalPokemonState(
                "pkmn-1", "player-1", "pikachu", 20,
                Set.of(), Set.of("burned"), flinch,
                null, null, null, null, null, null, null, 1L));
    }
}
