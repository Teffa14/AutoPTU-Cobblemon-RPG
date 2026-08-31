package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Optional;

/** Read-only owner-scoped projection of durable Pokemon progression. */
public final class CanonicalPokemonProgressionQueryService {
    private final VersionedCanonicalPokemonRepository pokemonRepository;
    private final FileCanonicalPokemonProgressionRepository progressionRepository;

    public CanonicalPokemonProgressionQueryService(
            VersionedCanonicalPokemonRepository pokemonRepository,
            FileCanonicalPokemonProgressionRepository progressionRepository
    ) {
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
        this.progressionRepository = Objects.requireNonNull(progressionRepository, "progressionRepository");
    }

    public Optional<Snapshot> inspect(String authenticatedPlayerId, String pokemonId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()
                || pokemonId == null || pokemonId.isBlank()) return Optional.empty();
        String playerId = authenticatedPlayerId.strip();
        String canonicalPokemonId = pokemonId.strip();
        CanonicalPokemonState pokemon = pokemonRepository.findPokemon(canonicalPokemonId).orElse(null);
        if (pokemon == null || !pokemon.ownerPlayerId().equals(playerId)) return Optional.empty();

        var state = progressionRepository.findOrCreate(playerId, canonicalPokemonId);
        if (!state.ownerPlayerId().equals(playerId)) {
            throw new IllegalStateException("Pokemon progression repository returned mismatched owner");
        }
        return Optional.of(new Snapshot(
                state.pokemonId(),
                pokemon.speciesId(),
                pokemon.level(),
                state.pokemonXp(),
                state.pendingEvolutionChoiceId(),
                state.revision()
        ));
    }

    public record Snapshot(
            String pokemonId,
            String speciesId,
            int canonicalLevel,
            long pokemonXp,
            String pendingEvolutionChoiceId,
            long progressionRevision
    ) { }
}
