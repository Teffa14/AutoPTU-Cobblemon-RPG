package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Persistent server-owned usage ledger for limited-frequency Trainer actions. */
public interface TrainerActionUsageLedger {
    long observeOverworldDay(long observedOverworldDay);

    TrainerActionUsageDecision reserve(TrainerActionUsageAttempt attempt);

    boolean commit(String reservationId, String playerId);

    boolean release(String reservationId, String playerId);

    Optional<TrainerActionUsageReservation> findByOperationId(String operationId);

    long highestObservedOverworldDay();
}
