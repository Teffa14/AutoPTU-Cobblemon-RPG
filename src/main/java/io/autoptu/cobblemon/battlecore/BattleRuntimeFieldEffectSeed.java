package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral seed for one server-owned duration-bearing PTU field effect.
 *
 * The integration layer transports semantic field state only. Minecraft block identities,
 * particles, biome/weather visuals, collision data and client payloads cannot define PTU
 * terrain/zone/room identity or duration through this record.
 */
public record BattleRuntimeFieldEffectSeed(
        Kind kind,
        String name,
        Integer remainingRounds,
        Map<String, Object> payload
) {
    public enum Kind {
        TERRAIN,
        ZONE,
        ROOM
    }

    public BattleRuntimeFieldEffectSeed {
        if (kind == null) throw new IllegalArgumentException("field effect kind is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("field effect name is required");
        name = name.strip();
        payload = immutableScalarPayload(payload);
    }

    public BattleRuntimeFieldEffectSeed(Kind kind, String name, Integer remainingRounds) {
        this(kind, name, remainingRounds, Map.of());
    }

    private static Map<String, Object> immutableScalarPayload(Map<String, ?> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("field-effect payload keys must be non-blank");
            }
            Object value = entry.getValue();
            if (value != null
                    && !(value instanceof String)
                    && !(value instanceof Integer)
                    && !(value instanceof Long)
                    && !(value instanceof Double)
                    && !(value instanceof Boolean)) {
                throw new IllegalArgumentException("field-effect payload values must be scalar: " + key);
            }
            copied.put(key.strip(), value);
        }
        return Collections.unmodifiableMap(copied);
    }
}
