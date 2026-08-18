package io.autoptu.cobblemon.authority;

public record ReservationDecision(boolean allowed, String reason, ItemReservation reservation) {
    public ReservationDecision {
        reason = reason == null ? "" : reason;
    }

    public static ReservationDecision allow(ItemReservation reservation) {
        return new ReservationDecision(true, "", reservation);
    }

    public static ReservationDecision deny(String reason) {
        return new ReservationDecision(false, reason, null);
    }
}
