package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Project-owned presentation instruction derived only from an authoritative semantic battle event.
 *
 * Commands are rendering inputs. They never decide PTU legality, damage, status changes, movement,
 * item/ability effects, lifecycle state, or any other battle outcome.
 */
public record BattlePresentationCommand(
        long sequence,
        int ordinal,
        Kind kind,
        String subjectId,
        Map<String, String> data
) {
    public enum Kind {
        MOVE_ANIMATION,
        HP_PROJECTION,
        ENTITY_RELOCATION,
        STATUS_SKIP_CUE,
        TRAINER_FEATURE_CUE,
        RULE_EFFECT_CUE,
        FIELD_EFFECT_CUE,
        PHASE_CUE,
        TURN_START_CUE,
        TURN_END_CUE
    }

    public BattlePresentationCommand {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal cannot be negative");
        kind = Objects.requireNonNull(kind, "kind");
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId is required");
        }
        subjectId = subjectId.strip();
        data = immutableData(data);
    }

    private static Map<String, String> immutableData(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("data key is required");
            String normalizedKey = key.strip();
            if (copy.putIfAbsent(normalizedKey, value == null ? "" : value.strip()) != null) {
                throw new IllegalArgumentException("duplicate data key: " + normalizedKey);
            }
        });
        return Map.copyOf(copy);
    }
}
