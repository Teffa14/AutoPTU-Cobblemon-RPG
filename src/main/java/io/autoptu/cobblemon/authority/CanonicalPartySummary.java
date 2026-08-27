package io.autoptu.cobblemon.authority;

import java.util.List;

/** Read-only server-owned party projection for Minecraft UI/commands. */
public record CanonicalPartySummary(
        String playerId,
        List<Member> members,
        long partyRevision
) {
    public CanonicalPartySummary {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        playerId = playerId.strip();
        members = members == null ? List.of() : List.copyOf(members);
        if (partyRevision < 0) throw new IllegalArgumentException("partyRevision must be >= 0");
    }

    public record Member(
            int slot,
            String pokemonId,
            String speciesId,
            int level,
            Integer currentHp,
            Integer maxHp,
            List<String> statuses,
            long pokemonRevision
    ) {
        public Member {
            if (slot < 1) throw new IllegalArgumentException("slot must be >= 1");
            if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId is required");
            if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
            if (level < 1) throw new IllegalArgumentException("level must be >= 1");
            if ((currentHp == null) != (maxHp == null)) throw new IllegalArgumentException("HP values must both be present or absent");
            if (currentHp != null && (maxHp <= 0 || currentHp < 0 || currentHp > maxHp)) {
                throw new IllegalArgumentException("invalid HP summary");
            }
            statuses = statuses == null ? List.of() : List.copyOf(statuses);
            if (pokemonRevision < 0) throw new IllegalArgumentException("pokemonRevision must be >= 0");
        }

        public boolean hasHealth() {
            return currentHp != null;
        }
    }
}
