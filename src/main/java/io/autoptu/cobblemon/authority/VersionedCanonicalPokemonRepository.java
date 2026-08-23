package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Server-owned revisioned persistence contract for complete canonical Pokemon aggregates. */
public interface VersionedCanonicalPokemonRepository {
    Optional<CanonicalPokemonState> findPokemon(String pokemonId);

    boolean createPokemonIfAbsent(CanonicalPokemonState initialState);

    boolean replacePokemonIfRevision(String pokemonId, long expectedRevision, CanonicalPokemonState replacement);
}
