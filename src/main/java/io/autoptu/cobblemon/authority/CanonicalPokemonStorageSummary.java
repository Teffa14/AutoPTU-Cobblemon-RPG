package io.autoptu.cobblemon.authority;

import java.util.List;

/** Read-only owner-scoped projection for boxed canonical Pokemon. */
public record CanonicalPokemonStorageSummary(
        String playerId,
        List<Member> members,
        long storageRevision
) {
    public CanonicalPokemonStorageSummary {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        playerId = playerId.strip();
        members = members == null ? List.of() : List.copyOf(members);
        if (storageRevision < 0) throw new IllegalArgumentException("storageRevision must be >= 0");
    }

    public record Member(int boxSlot, String pokemonId, String speciesId, int level, long pokemonRevision) {
        public Member {
            if (boxSlot < 1) throw new IllegalArgumentException("boxSlot must be >= 1");
            if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId is required");
            if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
            if (level < 1) throw new IllegalArgumentException("level must be >= 1");
            if (pokemonRevision < 0) throw new IllegalArgumentException("pokemonRevision must be >= 0");
        }
    }
}
