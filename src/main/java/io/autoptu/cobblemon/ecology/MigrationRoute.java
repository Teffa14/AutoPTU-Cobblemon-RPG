package io.autoptu.cobblemon.ecology;

import java.util.List;
import java.util.Objects;

public record MigrationRoute(
        String id,
        long departureTick,
        long transitTicksBetweenStops,
        long finalTransitTicks,
        List<MigrationStopover> stopovers
) {
    public MigrationRoute {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stopovers, "stopovers");
        if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (departureTick < 0) throw new IllegalArgumentException("departureTick must be >= 0");
        if (transitTicksBetweenStops <= 0) throw new IllegalArgumentException("transitTicksBetweenStops must be > 0");
        if (finalTransitTicks <= 0) throw new IllegalArgumentException("finalTransitTicks must be > 0");
        stopovers = List.copyOf(stopovers);
    }
}
