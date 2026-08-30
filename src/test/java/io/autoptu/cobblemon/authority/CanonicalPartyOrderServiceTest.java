package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPartyOrderServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void movesRequestedMemberAndPersistsOrder() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b", "poke-c", "poke-d"), 4L));

        CanonicalPartyOrderService.Decision decision = new CanonicalPartyOrderService(repository).move(playerId, 4, 2);

        assertEquals(CanonicalPartyOrderService.Outcome.APPLIED, decision.outcome());
        assertTrue(decision.changedState());
        assertEquals(List.of("poke-a", "poke-d", "poke-b", "poke-c"), decision.profile().pokemonIds());
        assertEquals(5L, decision.profile().revision());
        CanonicalPlayerEncounterProfile reloaded = repository.findProfile(playerId).orElseThrow();
        assertEquals(List.of("poke-a", "poke-d", "poke-b", "poke-c"), reloaded.pokemonIds());
        assertEquals(5L, reloaded.revision());
    }

    @Test
    void movingForwardUsesFinalRequestedSlot() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b", "poke-c", "poke-d"), 1L));

        CanonicalPartyOrderService.Decision decision = new CanonicalPartyOrderService(repository).move(playerId, 1, 3);

        assertEquals(CanonicalPartyOrderService.Outcome.APPLIED, decision.outcome());
        assertEquals(List.of("poke-b", "poke-c", "poke-a", "poke-d"), decision.profile().pokemonIds());
    }

    @Test
    void rejectsUnknownSlotWithoutMutation() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b"), 2L));

        CanonicalPartyOrderService.Decision decision = new CanonicalPartyOrderService(repository).move(playerId, 1, 3);

        assertEquals(CanonicalPartyOrderService.Outcome.INVALID_SLOT, decision.outcome());
        assertFalse(decision.changedState());
        assertEquals(List.of("poke-a", "poke-b"), repository.findProfile(playerId).orElseThrow().pokemonIds());
        assertEquals(2L, repository.findProfile(playerId).orElseThrow().revision());
    }

    @Test
    void sameSlotIsIdempotent() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b"), 7L));

        CanonicalPartyOrderService.Decision decision = new CanonicalPartyOrderService(repository).move(playerId, 2, 2);

        assertEquals(CanonicalPartyOrderService.Outcome.ALREADY_ORDERED, decision.outcome());
        assertFalse(decision.changedState());
        assertEquals(7L, repository.findProfile(playerId).orElseThrow().revision());
    }

    private static CanonicalPlayerEncounterProfile profile(String playerId, List<String> pokemonIds, long revision) {
        return new CanonicalPlayerEncounterProfile(playerId, pokemonIds, Map.of(), arena(), revision);
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);
    }
}
