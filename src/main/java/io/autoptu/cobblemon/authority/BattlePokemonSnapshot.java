package io.autoptu.cobblemon.authority;

import java.util.Set;

public record BattlePokemonSnapshot(
        String pokemonId,
        String ownerPlayerId,
        String speciesId,
        int level,
        Set<String> capabilities,
        Set<String> statuses,
        String heldItemInstanceId,
        long revision
) {
    public BattlePokemonSnapshot {
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
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank()
                ? null
                : heldItemInstanceId;
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public BattlePokemonSnapshot(
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

    public static BattlePokemonSnapshot from(CanonicalPokemonState state) {
        return new BattlePokemonSnapshot(
                state.pokemonId(),
                state.ownerPlayerId(),
                state.speciesId(),
                state.level(),
                state.capabilities(),
                state.statuses(),
                state.heldItemInstanceId(),
                state.revision());
    }
}
