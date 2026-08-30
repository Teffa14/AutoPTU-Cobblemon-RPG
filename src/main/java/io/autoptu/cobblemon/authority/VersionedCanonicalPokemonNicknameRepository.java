package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Revisioned server-owned persistence contract for RPG-only Pokemon nicknames. */
public interface VersionedCanonicalPokemonNicknameRepository {
    Optional<CanonicalPokemonNicknameState> findNickname(String pokemonId);
    boolean createNicknameIfAbsent(CanonicalPokemonNicknameState initialState);
    boolean replaceNicknameIfRevision(String pokemonId, long expectedRevision, CanonicalPokemonNicknameState replacement);
}
