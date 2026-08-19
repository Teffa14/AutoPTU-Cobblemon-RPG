package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Set;

public record BattleTrainerSnapshot(
        String playerId,
        Set<String> trainerClasses,
        Map<String, Integer> skillRanks,
        Set<String> trainerFeatures,
        long revision
) {
    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, Set.of(), revision);
    }

    public BattleTrainerSnapshot {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        trainerClasses = trainerClasses == null ? Set.of() : Set.copyOf(trainerClasses);
        skillRanks = skillRanks == null ? Map.of() : Map.copyOf(skillRanks);
        trainerFeatures = trainerFeatures == null ? Set.of() : Set.copyOf(trainerFeatures);
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public static BattleTrainerSnapshot from(CanonicalPlayerState state) {
        return new BattleTrainerSnapshot(
                state.playerId(),
                state.trainerClasses(),
                state.skillRanks(),
                state.trainerFeatures(),
                state.revision());
    }
}
