package io.autoptu.cobblemon.ecology;

import java.util.Objects;

public record MigrationStopover(
        String id,
        long minimumArrivalTick,
        long maximumDepartureTick,
        int capacity,
        double resourceRecoveryPerTick
) {
    public MigrationStopover {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (minimumArrivalTick < 0) throw new IllegalArgumentException("minimumArrivalTick must be >= 0");
        if (maximumDepartureTick < minimumArrivalTick) {
            throw new IllegalArgumentException("maximumDepartureTick must be >= minimumArrivalTick");
        }
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        if (resourceRecoveryPerTick < 0.0) {
            throw new IllegalArgumentException("resourceRecoveryPerTick must be >= 0");
        }
    }
}
