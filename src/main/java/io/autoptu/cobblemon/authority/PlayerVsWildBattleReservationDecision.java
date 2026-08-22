package io.autoptu.cobblemon.authority;

public record PlayerVsWildBattleReservationDecision(
        boolean allowed,
        String reason,
        PlayerVsWildBattleReservation reservation
) {
    public PlayerVsWildBattleReservationDecision {
        reason = reason == null ? "" : reason;
        if (allowed && reservation == null) {
            throw new IllegalArgumentException("allowed decision requires reservation");
        }
        if (!allowed && reservation != null) {
            throw new IllegalArgumentException("denied decision cannot carry reservation");
        }
    }

    public static PlayerVsWildBattleReservationDecision allow(PlayerVsWildBattleReservation reservation) {
        return new PlayerVsWildBattleReservationDecision(true, "", reservation);
    }

    public static PlayerVsWildBattleReservationDecision deny(String reason) {
        return new PlayerVsWildBattleReservationDecision(false, reason, null);
    }
}
