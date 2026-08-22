package io.autoptu.cobblemon.authority;

public record BattleEncounterRosterReservationDecision(
        boolean allowed,
        String reason,
        BattleEncounterRosterReservation reservation
) {
    public BattleEncounterRosterReservationDecision {
        reason = reason == null ? "" : reason;
        if (allowed && reservation == null) {
            throw new IllegalArgumentException("allowed decision requires reservation");
        }
        if (!allowed && reservation != null) {
            throw new IllegalArgumentException("denied decision cannot contain reservation");
        }
    }

    public static BattleEncounterRosterReservationDecision allow(BattleEncounterRosterReservation reservation) {
        return new BattleEncounterRosterReservationDecision(true, "", reservation);
    }

    public static BattleEncounterRosterReservationDecision deny(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("deny reason is required");
        return new BattleEncounterRosterReservationDecision(false, reason, null);
    }
}
