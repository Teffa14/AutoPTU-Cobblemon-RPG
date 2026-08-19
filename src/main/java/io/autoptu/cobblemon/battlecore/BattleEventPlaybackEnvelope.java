package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter-neutral envelope for one semantic event already resolved by AutoPTU-Java.
 *
 * The integration layer may preserve identifiers and presentation metadata, but it must
 * never derive PTU legality, damage, status effects, forced movement, or other outcomes
 * from this envelope. Minecraft/Cobblemon render the authoritative event only.
 */
public record BattleEventPlaybackEnvelope(
        long sequence,
        String kind,
        String stableKey,
        Map<String, String> attributes
) {
    private static final Set<String> SUPPORTED_KINDS = Set.of(
            "move_resolved",
            "shift_resolved",
            "status_skip",
            "trainer_feature",
            "rule_effect",
            "phase",
            "turn_end"
    );

    public BattleEventPlaybackEnvelope {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        kind = normalizeRequired(kind, "kind").toLowerCase(Locale.ROOT);
        if (!SUPPORTED_KINDS.contains(kind)) {
            throw new IllegalArgumentException("unsupported battle event kind: " + kind);
        }
        stableKey = normalizeRequired(stableKey, "stableKey");
        if (!stableKey.startsWith(kind + "|")) {
            throw new IllegalArgumentException("stableKey must be anchored to event kind");
        }
        attributes = immutableAttributes(attributes);
    }

    public static Set<String> supportedKinds() {
        return SUPPORTED_KINDS;
    }

    private static Map<String, String> immutableAttributes(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalizeRequired(key, "attribute key");
            if (copy.containsKey(normalizedKey)) {
                throw new IllegalArgumentException("duplicate attribute key: " + normalizedKey);
            }
            copy.put(normalizedKey, value == null ? "" : value.strip());
        });
        return Map.copyOf(copy);
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
