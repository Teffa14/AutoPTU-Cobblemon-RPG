package io.autoptu.cobblemon.authority;

/**
 * Package-private server-issued identity for reservations that must share one deterministic battle seed.
 * Adapter/client code cannot construct this type from outside the authority package.
 */
record BattleReservationAuthority(String reservationId, long rngSeed) {
    BattleReservationAuthority {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
    }
}
