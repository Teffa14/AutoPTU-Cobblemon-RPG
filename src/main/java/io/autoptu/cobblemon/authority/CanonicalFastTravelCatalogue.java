package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-authored fast-travel destination catalogue.
 *
 * <p>This catalogue owns destination identity and presentation metadata only. It does not infer
 * discovery, progression, PTU legality, or client-provided coordinates. Availability remains an
 * explicit server input to {@link CanonicalFastTravelService} until durable discovery/unlock state
 * exists.
 */
public final class CanonicalFastTravelCatalogue {
    public static final String OVERWORLD_SPAWN_ID = "overworld_spawn";

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination(OVERWORLD_SPAWN_ID, "Overworld Spawn")
    );

    private CanonicalFastTravelCatalogue() {}

    public static List<Destination> destinations() {
        return DESTINATIONS;
    }

    public static Optional<Destination> find(String destinationId) {
        if (destinationId == null || destinationId.isBlank()) return Optional.empty();
        return DESTINATIONS.stream()
                .filter(destination -> destination.id().equals(destinationId))
                .findFirst();
    }

    public record Destination(String id, String displayName) {
        public Destination {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            if (id.isBlank()) throw new IllegalArgumentException("destination id is required");
            if (displayName.isBlank()) throw new IllegalArgumentException("destination display name is required");
        }
    }
}
