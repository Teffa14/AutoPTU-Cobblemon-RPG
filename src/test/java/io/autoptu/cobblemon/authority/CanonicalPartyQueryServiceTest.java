package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPartyQueryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void projectsSlotOrderHealthAndStatusesFromCanonicalState() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        String playerId = "minecraft-player:test";

        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "poke-b", playerId, "squirtle", 7, Set.of(), Set.of("poisoned", "burned"),
                null, new CanonicalHealth(9, 24), null, 2L
        ));
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "poke-a", playerId, "bulbasaur", 5, Set.of(), 1L
        ));
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                playerId,
                List.of("poke-b", "poke-a"),
                Map.of(),
                arena(),
                3L
        ));

        CanonicalPartySummary summary = new CanonicalPartyQueryService(parties, pokemon)
                .findParty(playerId)
                .orElseThrow();

        assertEquals(3L, summary.partyRevision());
        assertEquals(2, summary.members().size());
        assertEquals(1, summary.members().get(0).slot());
        assertEquals("squirtle", summary.members().get(0).speciesId());
        assertEquals(9, summary.members().get(0).currentHp());
        assertEquals(24, summary.members().get(0).maxHp());
        assertEquals(List.of("burned", "poisoned"), summary.members().get(0).statuses());
        assertEquals(2, summary.members().get(1).slot());
        assertFalse(summary.members().get(1).hasHealth());
    }

    @Test
    void returnsEmptyWhenPlayerHasNoCanonicalParty() {
        CanonicalPartyQueryService service = new CanonicalPartyQueryService(
                new FileCanonicalPlayerEncounterProfileRepository(tempDir),
                new FileCanonicalPokemonRepository(tempDir)
        );
        assertTrue(service.findParty("minecraft-player:none").isEmpty());
    }

    @Test
    void failsClosedWhenPartyReferencesForeignPokemon() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "foreign", "minecraft-player:other", "pikachu", 5, Set.of(), 0L
        ));
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                "minecraft-player:test", List.of("foreign"), Map.of(), arena(), 0L
        ));

        CanonicalPartyQueryService service = new CanonicalPartyQueryService(parties, pokemon);
        assertThrows(IllegalStateException.class, () -> service.findParty("minecraft-player:test"));
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);
    }
}
