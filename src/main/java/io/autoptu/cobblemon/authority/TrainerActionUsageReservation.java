package io.autoptu.cobblemon.authority;

/** Durable reservation for one limited-frequency Trainer action use. */
public record TrainerActionUsageReservation(
        String reservationId,
        String operationId,
        String playerId,
        String actionId,
        TrainerActionFrequency frequency,
        int maxUses,
        String windowId,
        Status status,
        long createdAtEpochMs
) {
    public enum Status {
        RESERVED,
        COMMITTED
    }

    public TrainerActionUsageReservation {
        reservationId = require("reservationId", reservationId);
        operationId = require("operationId", operationId);
        playerId = require("playerId", playerId);
        actionId = require("actionId", actionId);
        if (frequency == null) throw new IllegalArgumentException("frequency is required");
        if (frequency == TrainerActionFrequency.AT_WILL || frequency.battleCoreOwned()) {
            throw new IllegalArgumentException("only limited overworld frequencies may be reserved");
        }
        if (maxUses < 1) throw new IllegalArgumentException("maxUses must be positive");
        windowId = require("windowId", windowId);
        if (status == null) throw new IllegalArgumentException("status is required");
        if (createdAtEpochMs < 0) throw new IllegalArgumentException("createdAtEpochMs must not be negative");
    }

    public TrainerActionUsageReservation committed() {
        if (status == Status.COMMITTED) return this;
        return new TrainerActionUsageReservation(
                reservationId,
                operationId,
                playerId,
                actionId,
                frequency,
                maxUses,
                windowId,
                Status.COMMITTED,
                createdAtEpochMs
        );
    }

    private static String require(String field, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
