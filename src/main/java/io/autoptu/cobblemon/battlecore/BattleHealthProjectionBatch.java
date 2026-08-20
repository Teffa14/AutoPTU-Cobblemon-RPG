package io.autoptu.cobblemon.battlecore;

import java.util.List;

/** Reservation-bound display health updates derived from authoritative battle playback. */
public record BattleHealthProjectionBatch(
        String reservationId,
        List<BattleHealthProjection> healthUpdates
) {
    public BattleHealthProjectionBatch {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        healthUpdates = healthUpdates == null ? List.of() : List.copyOf(healthUpdates);
    }
}
