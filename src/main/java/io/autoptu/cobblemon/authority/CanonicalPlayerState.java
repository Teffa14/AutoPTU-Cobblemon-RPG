package io.autoptu.cobblemon.authority;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record CanonicalPlayerState(
        String playerId,
        Set<String> trainerClasses,
        Map<String, Integer> skillRanks,
        Set<String> availablePokemonCapabilities,
        Set<String> trainerFeatures,
        long revision
) {
    public CanonicalPlayerState(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            Set<String> availablePokemonCapabilities,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, availablePokemonCapabilities, Set.of(), revision);
    }

    public CanonicalPlayerState {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        trainerClasses = trainerClasses == null ? Set.of() : Set.copyOf(trainerClasses);
        skillRanks = skillRanks == null ? Map.of() : Map.copyOf(skillRanks);
        availablePokemonCapabilities = availablePokemonCapabilities == null
                ? Set.of()
                : Set.copyOf(availablePokemonCapabilities);
        trainerFeatures = normalizeTrainerFeatures(trainerFeatures);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public int skillRank(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }

    private static Set<String> normalizeTrainerFeatures(Set<String> features) {
        if (features == null || features.isEmpty()) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        LinkedHashSet<String> normalizedKeys = new LinkedHashSet<>();
        for (String feature : features) {
            if (feature == null || feature.isBlank()) {
                throw new IllegalArgumentException("trainer feature identity must not be blank");
            }
            String value = feature.strip();
            String key = value.toLowerCase(Locale.ROOT);
            if (!normalizedKeys.add(key)) {
                throw new IllegalArgumentException("duplicate trainer feature identity: " + value);
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }
}
