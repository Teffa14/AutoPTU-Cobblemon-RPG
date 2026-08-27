package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalStarterSelectionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsOneConfiguredStarterAndPartyAcrossRepositoryRestart() {
        FileCanonicalPlayerEncounterProfileRepository parties =
                new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        CanonicalStarterSelectionService service = service(parties, pokemon);

        CanonicalStarterSelectionDecision decision = service.choose(
                "minecraft-player:test",
                "Charmander",
                arena()
        );

        assertTrue(decision.chosen());
        assertEquals("charmander", decision.speciesId());
        assertEquals("minecraft-player:test:starter", decision.pokemonId());

        FileCanonicalPlayerEncounterProfileRepository restartedParties =
                new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository restartedPokemon = new FileCanonicalPokemonRepository(tempDir);
        CanonicalPlayerEncounterProfile profile = restartedParties.findProfile("minecraft-player:test").orElseThrow();
        CanonicalPokemonState starter = restartedPokemon.findPokemon(decision.pokemonId()).orElseThrow();

        assertEquals(1, profile.pokemonIds().size());
        assertEquals(decision.pokemonId(), profile.pokemonIds().get(0));
        assertEquals("minecraft-player:test", starter.ownerPlayerId());
        assertEquals("charmander", starter.speciesId());
        assertEquals(5, starter.level());
        assertNull(starter.health());
        assertTrue(starter.capabilities().isEmpty());
    }

    @Test
    void refusesChangingStarterAfterPersistentPartyExists() {
        FileCanonicalPlayerEncounterProfileRepository parties =
                new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        CanonicalStarterSelectionService service = service(parties, pokemon);

        assertTrue(service.choose("minecraft-player:test", "bulbasaur", arena()).chosen());
        CanonicalStarterSelectionDecision second = service.choose("minecraft-player:test", "squirtle", arena());

        assertFalse(second.chosen());
        assertEquals(CanonicalStarterSelectionDecision.Outcome.ALREADY_CHOSEN, second.outcome());
        assertEquals("bulbasaur", second.speciesId());
        assertEquals("bulbasaur", pokemon.findPokemon("minecraft-player:test:starter").orElseThrow().speciesId());
    }

    @Test
    void rejectsSpeciesOutsideServerCatalogueWithoutCreatingState() {
        FileCanonicalPlayerEncounterProfileRepository parties =
                new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        CanonicalStarterSelectionService service = service(parties, pokemon);

        CanonicalStarterSelectionDecision decision = service.choose(
                "minecraft-player:test",
                "mewtwo",
                arena()
        );

        assertEquals(CanonicalStarterSelectionDecision.Outcome.INVALID_STARTER, decision.outcome());
        assertTrue(parties.findProfile("minecraft-player:test").isEmpty());
        assertTrue(pokemon.findPokemon("minecraft-player:test:starter").isEmpty());
    }

    private static CanonicalStarterSelectionService service(
            VersionedCanonicalPlayerEncounterProfileRepository parties,
            VersionedCanonicalPokemonRepository pokemon
    ) {
        return new CanonicalStarterSelectionService(new CanonicalStarterCatalogue(), parties, pokemon);
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);
    }
}
