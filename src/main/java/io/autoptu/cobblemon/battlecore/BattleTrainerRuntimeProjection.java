package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server-owned trainer/controller state prepared for AutoPTU-Java TrainerRuntimeState.
 * Feature ownership and AP are frozen from canonical trainer state before battle start.
 */
public record BattleTrainerRuntimeProjection(
        String trainerId,
        Set<String> trainerFeatures,
        int actionPoints,
        Set<String> controlledCombatantIds
) {
    public BattleTrainerRuntimeProjection {
        if (trainerId == null || trainerId.isBlank()) {
            throw new IllegalArgumentException("trainerId is required");
        }
        trainerId = trainerId.strip();
        trainerFeatures = trainerFeatures == null ? Set.of() : Set.copyOf(trainerFeatures);
        if (actionPoints < 0) {
            throw new IllegalArgumentException("actionPoints must be >= 0");
        }
        if (controlledCombatantIds == null || controlledCombatantIds.isEmpty()) {
            throw new IllegalArgumentException("controlledCombatantIds are required");
        }
        LinkedHashSet<String> combatants = new LinkedHashSet<>();
        for (String combatantId : controlledCombatantIds) {
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("controlled combatant identity must not be blank");
            }
            if (!combatants.add(combatantId.strip())) {
                throw new IllegalArgumentException("duplicate controlled combatant: " + combatantId);
            }
        }
        controlledCombatantIds = Set.copyOf(combatants);
    }
}
