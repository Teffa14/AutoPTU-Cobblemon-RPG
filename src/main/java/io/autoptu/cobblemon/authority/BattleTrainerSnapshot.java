package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Set;

public record BattleTrainerSnapshot(
        String playerId,
        Set<String> trainerClasses,
        Map<String, Integer> skillRanks,
        Set<String> trainerFeatures,
        int actionPoints,
        long revision
) {
    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, Set.of(), 0, revision);
    }

    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            Set<String> trainerFeatures,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, trainerFeatures, 0, revision);
    }

    public BattleTrainerSnapshot {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        trainerClasses = trainerClasses == null ? Set.of() : Set.copyOf(trainerClasses);
        skillRanks = skillRanks == null ? Map.of() : Map.copyOf(skillRanks);
        trainerFeatures = trainerFeatures == null ? Set.of() : Set.copyOf(trainerFeatures);
        if (actionPoints < 0) {
            throw new IllegalArgumentException("actionPoints must be >= 0");
        }
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
                state.actionPoints(),
                state.revision());
    }
}
