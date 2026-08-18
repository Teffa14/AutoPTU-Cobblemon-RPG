package io.autoptu.cobblemon.authority;

import java.util.Optional;

public interface BattleOutcomeRepository {
    Optional<BattleOutcomeCommit> findCommittedOutcome(String reservationId);

    /**
     * Atomically verifies that the supplied snapshot is still the active locked
     * battle reservation, revalidates canonical revisions, applies the trusted
     * outcome deltas, releases the battle locks, removes the active snapshot,
     * and persists the outcome as the idempotency record.
     */
    boolean tryCommitOutcome(BattleAuthoritySnapshot snapshot, BattleOutcomeCommit outcome);
}
