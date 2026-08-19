package io.autoptu.cobblemon.authority;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Immutable server-owned status identity plus scalar PTU metadata. */
public record CanonicalStatusEntry(String name, Map<String, Object> payload) {
    public CanonicalStatusEntry {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("status name must not be blank");
        name = name.strip().toLowerCase(Locale.ROOT);
        payload = immutableScalarPayload(payload);
    }

    public CanonicalStatusEntry(String name) {
        this(name, Map.of());
    }

    private static Map<String, Object> immutableScalarPayload(Map<String, ?> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) throw new IllegalArgumentException("status payload keys must be non-blank");
            Object value = entry.getValue();
            if (value != null
                    && !(value instanceof String)
                    && !(value instanceof Integer)
                    && !(value instanceof Long)
                    && !(value instanceof Double)
                    && !(value instanceof Boolean)) {
                throw new IllegalArgumentException("status payload values must be scalar: " + key);
            }
            copied.put(key, value);
        }
        return Collections.unmodifiableMap(copied);
    }
}
