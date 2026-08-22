package io.autoptu.cobblemon.authority;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Server-owned, durable selection inputs for a future player encounter reservation. */
public record CanonicalPlayerEncounterProfile(
        String playerId,
        List<String> pokemonIds,
        Map<String, Integer> consumableQuantities,
        BattleArenaSnapshot arena,
        long revision
) {
    public CanonicalPlayerEncounterProfile {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        playerId = playerId.strip();
        if (pokemonIds == null || pokemonIds.isEmpty()) throw new IllegalArgumentException("pokemonIds must not be empty");
        pokemonIds = List.copyOf(pokemonIds);
        Set<String> uniquePokemon = new HashSet<>();
        for (String pokemonId : pokemonIds) {
            if (pokemonId == null || pokemonId.isBlank() || !uniquePokemon.add(pokemonId)) {
                throw new IllegalArgumentException("pokemonIds must contain unique non-blank ids");
            }
        }
        LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();
        if (consumableQuantities != null) {
            for (Map.Entry<String, Integer> entry : consumableQuantities.entrySet()) {
                String itemId = entry.getKey();
                Integer quantity = entry.getValue();
                if (itemId == null || itemId.isBlank() || quantity == null || quantity <= 0) {
                    throw new IllegalArgumentException("consumable quantities require non-blank ids and positive quantities");
                }
                quantities.put(itemId.strip(), quantity);
            }
        }
        consumableQuantities = Map.copyOf(quantities);
        Objects.requireNonNull(arena, "arena");
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
    }
}
