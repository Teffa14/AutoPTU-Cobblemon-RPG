package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Set;

public record BattleTrainerSnapshot(
        String playerId,
        Set<String> trainerClasses,
        Map<String, Integer> skillRanks,
        Set<String> trainerFeatures,
        int actionPoints,
        int initiativeModifier,
        Integer explicitInitiativeSpeed,
        String teamId,
        long revision
) {
    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, Set.of(), 0, 0, null, "", revision);
    }

    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            Set<String> trainerFeatures,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, trainerFeatures, 0, 0, null, "", revision);
    }

    /** Backwards-compatible battle snapshot constructor using the Python default initiative modifier/profile. */
    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            Set<String> trainerFeatures,
            int actionPoints,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, trainerFeatures, actionPoints, 0, null, "", revision);
    }

    /** Backwards-compatible constructor used before explicit Trainer initiative Speed/team entered the snapshot. */
    public BattleTrainerSnapshot(
            String playerId,
            Set<String> trainerClasses,
            Map<String, Integer> skillRanks,
            Set<String> trainerFeatures,
            int actionPoints,
            int initiativeModifier,
            long revision
    ) {
        this(playerId, trainerClasses, skillRanks, trainerFeatures, actionPoints, initiativeModifier, null, "", revision);
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
        teamId = teamId == null ? "" : teamId.strip();
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
                state.initiativeModifier(),
                state.explicitInitiativeSpeed(),
                state.teamId(),
                state.revision());
    }
}
