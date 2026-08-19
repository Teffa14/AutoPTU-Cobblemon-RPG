package io.autoptu.cobblemon.battlecore;

import java.util.List;

/** Immutable ordered playback batch bound to one authoritative battle reservation. */
public record BattlePlaybackBatch(
        String reservationId,
        List<BattleEventPlaybackEnvelope> events
) {
    public BattlePlaybackBatch {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        events = events == null ? List.of() : List.copyOf(events);

        long previousSequence = -1;
        for (BattleEventPlaybackEnvelope event : events) {
            if (event == null) throw new IllegalArgumentException("events cannot contain null");
            if (event.sequence() <= previousSequence) {
                throw new IllegalArgumentException("event sequence must be strictly increasing");
            }
            previousSequence = event.sequence();
        }
    }
}
