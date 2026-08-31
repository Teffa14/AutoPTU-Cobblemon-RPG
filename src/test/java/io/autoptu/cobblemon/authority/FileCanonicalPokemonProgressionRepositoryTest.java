package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileCanonicalPokemonProgressionRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void createsNeutralOwnerScopedBaselineAndSurvivesRepositoryReopen() {
        var first = new FileCanonicalPokemonProgressionRepository(tempDir);
        var created = first.findOrCreate("player-1", "pokemon-1");
        assertEquals("player-1", created.ownerPlayerId());
        assertEquals("pokemon-1", created.pokemonId());
        assertEquals(0L, created.pokemonXp());
        assertNull(created.pendingEvolutionChoiceId());
        assertEquals(0L, created.revision());

        var reopened = new FileCanonicalPokemonProgressionRepository(tempDir);
        assertEquals(created, reopened.findOrCreate("player-1", "pokemon-1"));
    }

    @Test
    void revisionCasPersistsOnlyExplicitServerProvidedFacts() {
        var repository = new FileCanonicalPokemonProgressionRepository(tempDir);
        var initial = repository.findOrCreate("player-2", "pokemon-2");
        var replacement = new FileCanonicalPokemonProgressionRepository.ProgressionState(
                initial.ownerPlayerId(), initial.pokemonId(), 850L, "species:ivysaur", 1L);

        assertTrue(repository.replaceIfRevision(replacement, 0L));
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalPokemonProgressionRepository.ProgressionState(
                        "player-2", "pokemon-2", 999999L, "species:venusaur", 1L),
                0L));

        var reopened = new FileCanonicalPokemonProgressionRepository(tempDir);
        assertEquals(replacement, reopened.findOrCreate("player-2", "pokemon-2"));
    }

    @Test
    void queryRequiresCanonicalOwnershipAndUsesCanonicalLevel() {
        var pokemonRepository = new FakePokemonRepository();
        pokemonRepository.states.put("pokemon-owned", new CanonicalPokemonState(
                "pokemon-owned", "player-3", "species:bulbasaur", 7, Set.of(), 0L));
        pokemonRepository.states.put("pokemon-other", new CanonicalPokemonState(
                "pokemon-other", "other-player", "species:charmander", 9, Set.of(), 0L));
        var progressionRepository = new FileCanonicalPokemonProgressionRepository(tempDir);
        var service = new CanonicalPokemonProgressionQueryService(pokemonRepository, progressionRepository);

        var owned = service.inspect("player-3", "pokemon-owned").orElseThrow();
        assertEquals(7, owned.canonicalLevel());
        assertEquals(0L, owned.pokemonXp());
        assertTrue(service.inspect("player-3", "pokemon-other").isEmpty());
        assertTrue(service.inspect("player-3", "missing").isEmpty());
        assertTrue(progressionRepository.find("pokemon-other").isEmpty());
    }

    private static final class FakePokemonRepository implements VersionedCanonicalPokemonRepository {
        private final Map<String, CanonicalPokemonState> states = new HashMap<>();

        @Override public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return Optional.ofNullable(states.get(pokemonId));
        }

        @Override public boolean createPokemonIfAbsent(CanonicalPokemonState initialState) {
            return states.putIfAbsent(initialState.pokemonId(), initialState) == null;
        }

        @Override public boolean replacePokemonIfRevision(
                String pokemonId, long expectedRevision, CanonicalPokemonState replacement) {
            CanonicalPokemonState current = states.get(pokemonId);
            if (current == null || current.revision() != expectedRevision) return false;
            states.put(pokemonId, replacement);
            return true;
        }
    }
}
