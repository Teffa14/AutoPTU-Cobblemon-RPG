package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Result of server-side PTU Trainer action frequency enforcement. */
public record TrainerActionUsageDecision(
        Status status,
        Optional<TrainerActionUsageReservation> reservation,
        int used,
        int limit,
        String windowId
) {
    public enum Status {
        ALLOWED_AT_WILL,
        RESERVED,
        ALREADY_RESERVED,
        ALREADY_COMMITTED,
        LIMIT_REACHED,
        CONTEXT_REQUIRED,
        BATTLE_CORE_OWNED,
        FEATURE_NOT_OWNED,
        OPERATION_CONFLICT
    }

    public TrainerActionUsageDecision {
        if (status == null) throw new IllegalArgumentException("status is required");
        reservation = reservation == null ? Optional.empty() : reservation;
        if (used < 0) throw new IllegalArgumentException("used must not be negative");
        if (limit < 0) throw new IllegalArgumentException("limit must not be negative");
        windowId = windowId == null ? "" : windowId;
    }

    public boolean allowed() {
        return status == Status.ALLOWED_AT_WILL
                || status == Status.RESERVED
                || status == Status.ALREADY_RESERVED
                || status == Status.ALREADY_COMMITTED;
    }

    public static TrainerActionUsageDecision withoutReservation(Status status, int used, int limit, String windowId) {
        return new TrainerActionUsageDecision(status, Optional.empty(), used, limit, windowId);
    }

    public static TrainerActionUsageDecision withReservation(
            Status status,
            TrainerActionUsageReservation reservation,
            int used,
            int limit
    ) {
        return new TrainerActionUsageDecision(
                status,
                Optional.of(reservation),
                used,
                limit,
                reservation.windowId()
        );
    }
}
