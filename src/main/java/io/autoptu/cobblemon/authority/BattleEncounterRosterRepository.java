package io.autoptu.cobblemon.authority;

import java.util.Optional;

public interface BattleEncounterRosterRepository {
    Optional<BattleEncounterRosterReservation> findReservation(String reservationId);

    /** Atomically revalidates canonical combatant revisions and locks every combatant in the encounter. */
    boolean tryReserve(BattleEncounterRosterReservation reservation);

    boolean release(String reservationId);
}
