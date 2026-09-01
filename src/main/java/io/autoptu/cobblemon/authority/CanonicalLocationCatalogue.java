package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authored RPG locations. This catalogue carries world identity only, never PTU rules. */
public final class CanonicalLocationCatalogue {
    public static final CanonicalLocationCatalogue DEFAULT = new CanonicalLocationCatalogue(List.of(
            new Location("overworld_spawn", "Overworld Spawn", "minecraft:overworld", 12.0D),
            new Location("ouros.marea.puerto_bruma", "Puerto Bruma", "minecraft:overworld", 54.0D),
            new Location("ouros.marea.bruma_market_hall", "Bruma Market Hall", "minecraft:overworld", 12.0D),
            new Location("ouros.marea.marea_field_office", "Marea Field Office", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.tideglass_archive", "Tideglass Archive", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.bruma_battle_yard", "Bruma Battle Yard", "minecraft:overworld", 14.0D),
            new Location("ouros.marea.ferry_landing", "Puerto Bruma Ferry Landing", "minecraft:overworld", 12.0D),
            new Location("ouros.marea.clinic", "Puerto Bruma Clinic", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.sendero_vidrio", "Sendero del Vidrio", "minecraft:overworld", 22.0D),
            new Location("ouros.marea.sendero_crossing", "Sendero Seasonal Crossing", "minecraft:overworld", 12.0D),
            new Location("ouros.marea.loma_clara", "Loma Clara", "minecraft:overworld", 44.0D),
            new Location("ouros.marea.loma_storehouse", "Loma Clara Cooperative Storehouse", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.loma_communal_kitchen", "Loma Clara Communal Kitchen", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.loma_field_school", "Loma Clara Field School", "minecraft:overworld", 10.0D),
            new Location("ouros.marea.estacion_mirador", "Estacion Mirador", "minecraft:overworld", 30.0D),
            new Location("ouros.marea.mirador_weather_mast", "Mirador Weather Mast", "minecraft:overworld", 8.0D),
            new Location("ouros.marea.mirador_transect", "Mirador Transect Trailhead", "minecraft:overworld", 9.0D)
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
