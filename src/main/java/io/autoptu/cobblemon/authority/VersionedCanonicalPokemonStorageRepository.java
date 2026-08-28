package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Durable owner-scoped boxed Pokemon aggregate with optimistic concurrency. */
public interface VersionedCanonicalPokemonStorageRepository {
    Optional<CanonicalPokemonStorageState> findStorage(String playerId);

    CanonicalPokemonStorageState findOrCreate(String playerId);

    boolean replaceIfRevision(String playerId, long expectedRevision, CanonicalPokemonStorageState replacement);
}
