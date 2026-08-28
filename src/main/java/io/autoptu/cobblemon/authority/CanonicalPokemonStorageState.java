package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Durable server-owned boxed Pokemon identity list for one authenticated Trainer. */
public record CanonicalPokemonStorageState(
        String playerId,
        List<String> pokemonIds,
        long revision
) {
    public CanonicalPokemonStorageState {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        playerId = playerId.strip();
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        ArrayList<String> normalized = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        if (pokemonIds != null) {
            for (String pokemonId : pokemonIds) {
                if (pokemonId == null || pokemonId.isBlank()) {
                    throw new IllegalArgumentException("pokemonIds must contain non-blank ids");
                }
                String id = pokemonId.strip();
                if (!unique.add(id)) throw new IllegalArgumentException("pokemonIds must be unique");
                normalized.add(id);
            }
        }
        pokemonIds = List.copyOf(normalized);
    }
}
