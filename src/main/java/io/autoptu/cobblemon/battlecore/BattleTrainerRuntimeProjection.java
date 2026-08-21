package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server-owned trainer/controller state prepared for AutoPTU-Java TrainerRuntimeState.
 * Feature ownership, skill ranks, AP and the initiative modifier are frozen from canonical trainer state before battle start.
 */
public record BattleTrainerRuntimeProjection(
        String trainerId,
        Set<String> trainerFeatures,
        int actionPoints,
        int initiativeModifier,
        Map<String, Integer> skillRanks,
        Set<String> controlledCombatantIds
) {
    /** Backwards-compatible projection using Python defaults for initiative modifier and skill ranks. */
    public BattleTrainerRuntimeProjection(
            String trainerId,
            Set<String> trainerFeatures,
            int actionPoints,
            Set<String> controlledCombatantIds
    ) {
        this(trainerId, trainerFeatures, actionPoints, 0, Map.of(), controlledCombatantIds);
    }

    /** Backwards-compatible projection for callers created before Trainer skill ranks entered runtime state. */
    public BattleTrainerRuntimeProjection(
            String trainerId,
            Set<String> trainerFeatures,
            int actionPoints,
            int initiativeModifier,
            Set<String> controlledCombatantIds
    ) {
        this(trainerId, trainerFeatures, actionPoints, initiativeModifier, Map.of(), controlledCombatantIds);
    }

    public BattleTrainerRuntimeProjection {
        if (trainerId == null || trainerId.isBlank()) {
            throw new IllegalArgumentException("trainerId is required");
        }
        trainerId = trainerId.strip();
        trainerFeatures = trainerFeatures == null ? Set.of() : Set.copyOf(trainerFeatures);
        if (actionPoints < 0) {
            throw new IllegalArgumentException("actionPoints must be >= 0");
        }
        skillRanks = copySkillRanks(skillRanks);
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

    private static Map<String, Integer> copySkillRanks(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
        LinkedHashSet<String> normalizedNames = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String skillName = entry.getKey();
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("Trainer skill name is required");
            }
            String value = skillName.strip();
            String normalized = value.toLowerCase(Locale.ROOT);
            if (!normalizedNames.add(normalized)) {
                throw new IllegalArgumentException("duplicate Trainer skill identity: " + value);
            }
            Integer rank = entry.getValue();
            if (rank == null) {
                throw new IllegalArgumentException("Trainer skill rank is required: " + value);
            }
            copy.put(value, rank);
        }
        return Map.copyOf(copy);
    }
}
