package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import io.autoptu.cobblemon.authority.CanonicalStatusState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildEncounterTableTest {
    @Test
    void zoneAndContextSelectAndFreezeAnAlreadyAuthoredCanonicalBlueprint() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:cedar_meadow", "grass_day");
        var sentret = new ServerOwnedWildEncounterTable.WeightedRoster("sentret-common", 7, List.of(seed("sentret", 11)));
        var hoppip = new ServerOwnedWildEncounterTable.WeightedRoster("hoppip-uncommon", 3, List.of(seed("hoppip", 10)));
        var table = new ServerOwnedWildEncounterTable(Map.of(key, List.of(sentret, hoppip)));

        var selected = table.select("ouros:cedar-meadow:encounter-001", "ouros:cedar_meadow", "grass_day", 1)
                .orElseThrow();

        assertEquals(key, selected.context());
        assertEquals("ouros:cedar-meadow:encounter-001", selected.blueprint().canonicalEncounterId());
        assertEquals(1, selected.blueprint().side());
        assertTrue(Set.of("sentret-common", "hoppip-uncommon").contains(selected.entryId()));
        assertEquals(selected.blueprint(), table.resolve("ouros:cedar-meadow:encounter-001").orElseThrow());
        assertThrows(IllegalStateException.class,
                () -> table.select("ouros:cedar-meadow:encounter-001", "ouros:cedar_meadow", "grass_day", 1));
    }

    @Test
    void sameCanonicalContextAndEncounterProduceStableSelectionAcrossRuntimeInstances() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:cedar_meadow", "grass_day");
        var entries = List.of(
                new ServerOwnedWildEncounterTable.WeightedRoster("a", 1, List.of(seed("sentret", 11))),
                new ServerOwnedWildEncounterTable.WeightedRoster("b", 1, List.of(seed("hoppip", 10))),
                new ServerOwnedWildEncounterTable.WeightedRoster("c", 1, List.of(seed("caterpie", 9)))
        );

        var first = new ServerOwnedWildEncounterTable(Map.of(key, entries));
        var second = new ServerOwnedWildEncounterTable(Map.of(key, entries));

        String firstEntry = first.select("ouros:encounter:stable-17", key.zoneId(), key.contextId(), 1)
                .orElseThrow().entryId();
        String secondEntry = second.select("ouros:encounter:stable-17", key.zoneId(), key.contextId(), 1)
                .orElseThrow().entryId();

        assertEquals(firstEntry, secondEntry);
    }

    @Test
    void differentEncounterIdsCanChooseDifferentWeightedEntriesWithoutBattleRngInput() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:route_1", "grass");
        var entries = List.of(
                new ServerOwnedWildEncounterTable.WeightedRoster("a", 1, List.of(seed("sentret", 8))),
                new ServerOwnedWildEncounterTable.WeightedRoster("b", 1, List.of(seed("hoppip", 8)))
        );
        var table = new ServerOwnedWildEncounterTable(Map.of(key, entries));

        String first = table.select("ouros:encounter:0", key.zoneId(), key.contextId(), 1).orElseThrow().entryId();
        boolean foundDifferent = false;
        for (int i = 1; i < 100; i++) {
            String next = table.select("ouros:encounter:" + i, key.zoneId(), key.contextId(), 1).orElseThrow().entryId();
            if (!first.equals(next)) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent);
    }

    @Test
    void unknownContextFailsClosedAndRegistersNothing() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:cedar_meadow", "grass_day");
        var table = new ServerOwnedWildEncounterTable(Map.of(
                key,
                List.of(new ServerOwnedWildEncounterTable.WeightedRoster("sentret", 1, List.of(seed("sentret", 11))))
        ));

        assertFalse(table.select("ouros:encounter:missing", "ouros:cedar_meadow", "cave", 1).isPresent());
        assertFalse(table.resolve("ouros:encounter:missing").isPresent());
    }

    @Test
    void selectedBlueprintPreservesTrustedRosterValuesExactlyAndCanBeReleased() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:cedar_meadow", "grass_night");
        var trusted = seed("hoothoot", 13);
        var table = new ServerOwnedWildEncounterTable(Map.of(
                key,
                List.of(new ServerOwnedWildEncounterTable.WeightedRoster("hoothoot", 1, List.of(trusted)))
        ));

        var selected = table.select("ouros:encounter:night-1", key.zoneId(), key.contextId(), 2).orElseThrow();

        assertEquals(trusted, selected.blueprint().pokemon().getFirst());
        assertTrue(table.release("ouros:encounter:night-1"));
        assertFalse(table.resolve("ouros:encounter:night-1").isPresent());
    }

    @Test
    void duplicateEntryIdsAndInvalidWeightsAreRejected() {
        var key = new ServerOwnedWildEncounterTable.ContextKey("ouros:route_1", "grass");
        var roster = List.of(seed("sentret", 8));

        assertThrows(IllegalArgumentException.class,
                () -> new ServerOwnedWildEncounterTable.WeightedRoster("bad", 0, roster));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerOwnedWildEncounterTable(Map.of(key, List.of(
                        new ServerOwnedWildEncounterTable.WeightedRoster("same", 1, roster),
                        new ServerOwnedWildEncounterTable.WeightedRoster("same", 2, roster)
                ))));
    }

    private static ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint seed(String species, int level) {
        return new ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint(
                species,
                level,
                Set.of("tracker"),
                Set.of(),
                new CanonicalStatusState(List.of()),
                new CanonicalCombatStats(8, 9, 10, 11, 12),
                new CanonicalHealth(37, 41),
                new CanonicalMoveLoadout(List.of("tackle", "quick-attack")),
                new CanonicalBaseMovement(5, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("normal"), List.of("run-away")),
                new CanonicalAccuracyEvasion(0, 0, 0, 0),
                new CanonicalInjuryState(0),
                null,
                7L
        );
    }
}
