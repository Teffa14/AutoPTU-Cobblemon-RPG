package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/** Server-owned Trainer Feature identities frozen for one battle trainer. */
public record BattleTrainerFeatureProjection(
        String trainerId,
        Set<String> trainerFeatures
) {
    public BattleTrainerFeatureProjection {
        if (trainerId == null || trainerId.isBlank()) {
            throw new IllegalArgumentException("trainerId is required");
        }
        trainerId = trainerId.strip();
        trainerFeatures = trainerFeatures == null ? Set.of() : Set.copyOf(trainerFeatures);
    }
}
