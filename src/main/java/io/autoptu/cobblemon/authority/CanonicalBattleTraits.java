package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Server-owned Pokemon identities that the battle core may use as rule inputs.
 *
 * Type and ability names are frozen from canonical PTU state. This record carries
 * identities only; it does not implement any ability behavior or modifier.
 */
public record CanonicalBattleTraits(List<String> types, List<String> abilities) {
    public CanonicalBattleTraits {
        types = normalizeRequiredUnique(types, "types");
        abilities = normalizeOptionalUnique(abilities, "abilities");
    }

    private static List<String> normalizeRequiredUnique(List<String> values, String field) {
        List<String> normalized = normalizeOptionalUnique(values, field);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must contain at least one canonical identity");
        }
        return normalized;
    }

    private static List<String> normalizeOptionalUnique(List<String> values, String field) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not contain blank identities");
            }
            String identity = value.strip();
            if (!seen.add(identity)) {
                throw new IllegalArgumentException(field + " must not contain duplicate identity: " + identity);
            }
            normalized.add(identity);
        }
        return List.copyOf(normalized);
    }
}
