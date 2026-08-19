package io.autoptu.cobblemon.battlecore;

import java.util.Locale;
import java.util.Set;

/**
 * Project-owned mirror of the public AutoPTU-Java MoveOption metadata boundary.
 * Values must come from a trusted server-owned move catalog, never from Minecraft/client payloads.
 */
public record AuthoritativeMoveMetadata(
        String moveId,
        Targeting targeting,
        String actionType,
        boolean requiresLineOfSight,
        Combat combat,
        String frequency
) {
    private static final Set<String> ACTION_TYPES = Set.of("standard", "shift", "swift", "full", "free");

    public AuthoritativeMoveMetadata {
        moveId = required(moveId, "moveId");
        if (targeting == null) throw new IllegalArgumentException("targeting is required");
        actionType = actionType == null ? "standard" : required(actionType, "actionType").toLowerCase(Locale.ROOT);
        if (!ACTION_TYPES.contains(actionType)) throw new IllegalArgumentException("unsupported actionType: " + actionType);
        frequency = optional(frequency);
    }

    public record Targeting(
            String targetKind,
            String rangeKind,
            Integer targetRange,
            Integer rangeValue,
            String areaKind,
            Integer areaValue,
            String rangeText
    ) {
        public Targeting {
            targetKind = optional(targetKind);
            rangeKind = optional(rangeKind);
            areaKind = optional(areaKind);
            rangeText = optional(rangeText);
            nonNegative(targetRange, "targetRange");
            nonNegative(rangeValue, "rangeValue");
            nonNegative(areaValue, "areaValue");
        }
    }

    /** Null combat metadata is valid for moves that do not use the damage pipeline. */
    public record Combat(Integer ac, int damageBase, int critRange, String damageCategory, String moveType) {
        public Combat {
            if (damageBase < 0) throw new IllegalArgumentException("damageBase cannot be negative");
            if (critRange < 1 || critRange > 20) throw new IllegalArgumentException("critRange must be between 1 and 20");
            damageCategory = required(damageCategory, "damageCategory").toLowerCase(Locale.ROOT);
            if (!damageCategory.equals("physical") && !damageCategory.equals("special")) {
                throw new IllegalArgumentException("damageCategory must be physical or special");
            }
            moveType = optional(moveType);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static String optional(String value) {
        if (value == null) return null;
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static void nonNegative(Integer value, String field) {
        if (value != null && value < 0) throw new IllegalArgumentException(field + " cannot be negative");
    }
}
