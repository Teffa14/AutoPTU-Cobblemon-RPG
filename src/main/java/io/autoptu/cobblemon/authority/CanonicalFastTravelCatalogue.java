package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Server-authored fast-travel destinations. Unlocks remain durable discovery state. */
public final class CanonicalFastTravelCatalogue {
    public static final String OVERWORLD_SPAWN_ID = "overworld_spawn";
    public static final String PUERTO_BRUMA_ID = "ouros.marea.puerto_bruma";
    public static final String SENDERO_VIDRIO_ID = "ouros.marea.sendero_vidrio";
    public static final String LOMA_CLARA_ID = "ouros.marea.loma_clara";
    public static final String ESTACION_MIRADOR_ID = "ouros.marea.estacion_mirador";

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination(OVERWORLD_SPAWN_ID, "Overworld Spawn"),
            new Destination(PUERTO_BRUMA_ID, "Puerto Bruma"),
            new Destination(SENDERO_VIDRIO_ID, "Sendero del Vidrio"),
            new Destination(LOMA_CLARA_ID, "Loma Clara"),
            new Destination(ESTACION_MIRADOR_ID, "Estacion Mirador")
    );

    private CanonicalFastTravelCatalogue() {}

    public static List<Destination> destinations() { return DESTINATIONS; }

    public static Optional<Destination> find(String destinationId) {
        if (destinationId == null || destinationId.isBlank()) return Optional.empty();
        return DESTINATIONS.stream().filter(destination -> destination.id().equals(destinationId)).findFirst();
    }

    public record Destination(String id, String displayName) {
        public Destination {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            if (id.isBlank()) throw new IllegalArgumentException("destination id is required");
            if (displayName.isBlank()) throw new IllegalArgumentException("destination display name is required");
            if (!OVERWORLD_SPAWN_ID.equals(id) && CanonicalWorldMapCatalogue.DEFAULT.site(id).isEmpty()) {
                throw new IllegalArgumentException("fast-travel destination requires an authored world-map site: " + id);
            }
        }
    }
}
