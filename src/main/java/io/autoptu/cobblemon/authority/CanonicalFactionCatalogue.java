package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-authored RPG faction identities. Faction metadata carries no PTU combat rules. */
public final class CanonicalFactionCatalogue {
    public static final CanonicalFactionCatalogue DEFAULT = new CanonicalFactionCatalogue(Map.of(
            "cedar_rangers", new Faction("cedar_rangers", "Cedar Rangers")
    ));

    private final Map<String, Faction> factions;

    public CanonicalFactionCatalogue(Map<String, Faction> factions) {
        Objects.requireNonNull(factions, "factions");
        Map<String, Faction> copy = new LinkedHashMap<>();
        for (Faction faction : factions.values()) {
            if (copy.putIfAbsent(faction.factionId(), faction) != null) {
                throw new IllegalArgumentException("duplicate canonical factionId: " + faction.factionId());
            }
        }
        this.factions = Map.copyOf(copy);
    }

    public Optional<Faction> faction(String factionId) {
        if (factionId == null || factionId.isBlank()) return Optional.empty();
        return Optional.ofNullable(factions.get(factionId.trim()));
    }

    public Map<String, Faction> factions() {
        return factions;
    }

    public record Faction(String factionId, String displayName) {
        public Faction {
            factionId = requireText(factionId, "factionId");
            displayName = requireText(displayName, "displayName");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
