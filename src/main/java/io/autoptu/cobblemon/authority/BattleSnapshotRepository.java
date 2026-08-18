package io.autoptu.cobblemon.authority;

import java.util.Optional;

public interface BattleSnapshotRepository {
    Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId);

    /**
     * Atomically revalidates every source revision/owner in the snapshot and locks
     * the trainer, roster Pokémon, held items, and consumables for this battle.
     */
    boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot);

    boolean releaseSnapshot(String reservationId, String playerId);
}
