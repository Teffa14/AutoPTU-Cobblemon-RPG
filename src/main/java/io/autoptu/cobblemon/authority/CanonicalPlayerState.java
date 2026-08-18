package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Set;

public record CanonicalPlayerState(
        String playerId,
        Set<String> trainerClasses,
        Map<String, Integer> skillRanks,
        Set<String> availablePokemonCapabilities,
        long revision
) {
    public CanonicalPlayerState {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        trainerClasses = trainerClasses == null ? Set.of() : Set.copyOf(trainerClasses);
        skillRanks = skillRanks == null ? Map.of() : Map.copyOf(skillRanks);
        availablePokemonCapabilities = availablePokemonCapabilities == null
                ? Set.of()
                : Set.copyOf(availablePokemonCapabilities);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public int skillRank(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }
}
