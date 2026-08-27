package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricPlayerStatusRuntimeTest {
    @Test
    void reportsLoadedTrainerRevisionPartyAndNoBlockers() {
        CanonicalPlayerState trainer = new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 7L);

        var snapshot = FabricPlayerStatusRuntime.snapshot(Optional.of(trainer), 2, true, Optional.empty());

        assertTrue(snapshot.trainerLoaded());
        assertEquals(7L, snapshot.revision());
        assertEquals(2, snapshot.partyCount());
        assertTrue(snapshot.blockers().isEmpty());
        assertTrue(FabricPlayerStatusRuntime.formatLines(snapshot).contains("Blockers: none"));
    }

    @Test
    void reportsPendingVisibleWildEncounterWithoutInventingBattleState() {
        CanonicalPlayerState trainer = new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 3L);
        var request = new WorldEncounterTriggerRequestService.Request(
                "encounter-42", "player-1", "actor-42", "ouros:overworld_surface",
                "visible_roaming_wild", "minecraft:overworld", 10, 64, 12, 100L);

        var snapshot = FabricPlayerStatusRuntime.snapshot(
                Optional.of(trainer), 1, true, Optional.of(request));

        assertEquals("encounter-42", snapshot.canonicalEncounterId());
        assertEquals("pending world encounter", snapshot.battleState());
        assertTrue(snapshot.blockers().stream().anyMatch(line -> line.contains("handoff/battle start")));
    }

    @Test
    void surfacesActionableOnboardingAndCorruptPartyBlockers() {
        var missingTrainer = FabricPlayerStatusRuntime.snapshot(Optional.empty(), 0, true, Optional.empty());
        List<String> missingLines = FabricPlayerStatusRuntime.formatLines(missingTrainer);
        assertTrue(missingLines.stream().anyMatch(line -> line.contains("Trainer state is not loaded")));
        assertTrue(missingLines.stream().anyMatch(line -> line.contains("Choose a starter")));

        CanonicalPlayerState trainer = new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 1L);
        var corruptParty = FabricPlayerStatusRuntime.snapshot(Optional.of(trainer), 0, false, Optional.empty());
        assertTrue(corruptParty.blockers().stream().anyMatch(line -> line.contains("inconsistent")));
    }
}
