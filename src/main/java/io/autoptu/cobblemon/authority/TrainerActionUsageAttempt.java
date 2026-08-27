package io.autoptu.cobblemon.authority;

/** Internal, server-resolved request to reserve one limited-frequency Trainer action use. */
public record TrainerActionUsageAttempt(
        String operationId,
        String playerId,
        CanonicalTrainerActionRule rule,
        String canonicalContextId,
        long observedOverworldDay,
        long createdAtEpochMs
) {
    public TrainerActionUsageAttempt {
        operationId = require("operationId", operationId);
        playerId = require("playerId", playerId);
        if (rule == null) throw new IllegalArgumentException("rule is required");
        if (observedOverworldDay < 0) throw new IllegalArgumentException("observedOverworldDay must not be negative");
        if (createdAtEpochMs < 0) throw new IllegalArgumentException("createdAtEpochMs must not be negative");
        if (canonicalContextId != null) {
            canonicalContextId = canonicalContextId.trim();
            if (canonicalContextId.isEmpty()) canonicalContextId = null;
        }
    }

    private static String require(String field, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
