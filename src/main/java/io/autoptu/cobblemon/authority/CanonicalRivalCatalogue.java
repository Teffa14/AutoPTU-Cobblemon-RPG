package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-authored rival identities. Rival metadata carries no PTU combat rules or battle outcomes. */
public final class CanonicalRivalCatalogue {
    public static final CanonicalRivalCatalogue DEFAULT = new CanonicalRivalCatalogue(Map.of(
            "cedar_challenger", new Rival("cedar_challenger", "Cedar Challenger")
    ));

    private final Map<String, Rival> rivals;

    public CanonicalRivalCatalogue(Map<String, Rival> rivals) {
        Objects.requireNonNull(rivals, "rivals");
        Map<String, Rival> copy = new LinkedHashMap<>();
        for (Rival rival : rivals.values()) {
            if (copy.putIfAbsent(rival.rivalId(), rival) != null) {
                throw new IllegalArgumentException("duplicate canonical rivalId: " + rival.rivalId());
            }
        }
        this.rivals = Map.copyOf(copy);
    }

    public Optional<Rival> rival(String rivalId) {
        if (rivalId == null || rivalId.isBlank()) return Optional.empty();
        return Optional.ofNullable(rivals.get(rivalId.trim()));
    }

    public record Rival(String rivalId, String displayName) {
        public Rival {
            rivalId = requireText(rivalId, "rivalId");
            displayName = requireText(displayName, "displayName");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
