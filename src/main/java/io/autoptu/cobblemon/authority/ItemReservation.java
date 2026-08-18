package io.autoptu.cobblemon.authority;

public record ItemReservation(
        String reservationId,
        String playerId,
        String itemInstanceId,
        String itemTemplateId,
        int quantity,
        long itemRevision
) {
    public ItemReservation {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        if (itemInstanceId == null || itemInstanceId.isBlank()) {
            throw new IllegalArgumentException("itemInstanceId must not be blank");
        }
        if (itemTemplateId == null || itemTemplateId.isBlank()) {
            throw new IllegalArgumentException("itemTemplateId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (itemRevision < 0) {
            throw new IllegalArgumentException("itemRevision must be >= 0");
        }
    }
}
