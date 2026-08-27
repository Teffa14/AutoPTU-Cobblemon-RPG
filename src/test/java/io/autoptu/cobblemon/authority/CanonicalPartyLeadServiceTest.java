package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPartyLeadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void promotesRequestedSlotAndPersistsOrder() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b", "poke-c"), 4L));

        CanonicalPartyLeadService.Decision decision = new CanonicalPartyLeadService(repository).setLead(playerId, 3);

        assertEquals(CanonicalPartyLeadService.Outcome.APPLIED, decision.outcome());
        assertTrue(decision.changedState());
        assertEquals(List.of("poke-c", "poke-a", "poke-b"), decision.profile().pokemonIds());
        assertEquals(5L, decision.profile().revision());
        CanonicalPlayerEncounterProfile reloaded = repository.findProfile(playerId).orElseThrow();
        assertEquals(List.of("poke-c", "poke-a", "poke-b"), reloaded.pokemonIds());
        assertEquals(5L, reloaded.revision());
    }

    @Test
    void rejectsUnknownSlotWithoutMutatingCanonicalState() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b"), 2L));

        CanonicalPartyLeadService.Decision decision = new CanonicalPartyLeadService(repository).setLead(playerId, 3);

        assertEquals(CanonicalPartyLeadService.Outcome.INVALID_SLOT, decision.outcome());
        assertFalse(decision.changedState());
        CanonicalPlayerEncounterProfile reloaded = repository.findProfile(playerId).orElseThrow();
        assertEquals(List.of("poke-a", "poke-b"), reloaded.pokemonIds());
        assertEquals(2L, reloaded.revision());
    }

    @Test
    void selectingCurrentLeadIsIdempotent() {
        FileCanonicalPlayerEncounterProfileRepository repository = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        String playerId = "minecraft-player:test";
        repository.createProfileIfAbsent(profile(playerId, List.of("poke-a", "poke-b"), 7L));

        CanonicalPartyLeadService.Decision decision = new CanonicalPartyLeadService(repository).setLead(playerId, 1);

        assertEquals(CanonicalPartyLeadService.Outcome.ALREADY_LEAD, decision.outcome());
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
