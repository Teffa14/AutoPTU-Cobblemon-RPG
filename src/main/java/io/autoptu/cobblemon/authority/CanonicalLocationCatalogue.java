package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authored RPG locations. This catalogue carries world identity only, never PTU rules. */
public final class CanonicalLocationCatalogue {
    public static final CanonicalLocationCatalogue DEFAULT = new CanonicalLocationCatalogue(List.of(
            new Location("overworld_spawn", "Overworld Spawn", "minecraft:overworld", 12.0D)
    ));

    private final Map<String, Location> locations;

    public CanonicalLocationCatalogue(List<Location> locations) {
        if (locations == null) throw new IllegalArgumentException("locations are required");
        LinkedHashMap<String, Location> indexed = new LinkedHashMap<>();
        for (Location location : locations) {
            if (location == null) throw new IllegalArgumentException("location is required");
            if (indexed.put(location.id(), location) != null) {
                throw new IllegalArgumentException("duplicate location id: " + location.id());
            }
        }
        this.locations = Map.copyOf(indexed);
    }

    public Optional<Location> location(String locationId) {
        if (locationId == null || locationId.isBlank()) return Optional.empty();
        return Optional.ofNullable(locations.get(locationId.trim()));
    }

    public List<Location> locations() { return List.copyOf(locations.values()); }

    public record Location(String id, String displayName, String dimensionId, double triggerRadius) {
        public Location {
            id = requireText(id, "id");
            displayName = requireText(displayName, "displayName");
            dimensionId = requireText(dimensionId, "dimensionId");
            if (!Double.isFinite(triggerRadius) || triggerRadius <= 0.0D) {
                throw new IllegalArgumentException("triggerRadius must be positive and finite");
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
