package io.autoptu.cobblemon.authority;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record CanonicalPokemonState(
        String pokemonId,
        String ownerPlayerId,
        String speciesId,
        int level,
        Set<String> capabilities,
        Set<String> statuses,
        String heldItemInstanceId,
        long revision
) {
    public CanonicalPokemonState {
        if (pokemonId == null || pokemonId.isBlank()) {
            throw new IllegalArgumentException("pokemonId must not be blank");
        }
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            throw new IllegalArgumentException("ownerPlayerId must not be blank");
        }
        if (speciesId == null || speciesId.isBlank()) {
            throw new IllegalArgumentException("speciesId must not be blank");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        statuses = normalizeStatuses(statuses);
        heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank()
                ? null
                : heldItemInstanceId;
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public CanonicalPokemonState(
            String pokemonId,
            String ownerPlayerId,
            String speciesId,
            int level,
            Set<String> capabilities,
            String heldItemInstanceId,
            long revision
    ) {
        this(pokemonId, ownerPlayerId, speciesId, level, capabilities, Set.of(), heldItemInstanceId, revision);
    }

    public CanonicalPokemonState(
            String pokemonId,
            String ownerPlayerId,
            String speciesId,
            int level,
            Set<String> capabilities,
            long revision
    ) {
        this(pokemonId, ownerPlayerId, speciesId, level, capabilities, Set.of(), null, revision);
    }

    private static Set<String> normalizeStatuses(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            normalized.add(value.strip().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }
}
