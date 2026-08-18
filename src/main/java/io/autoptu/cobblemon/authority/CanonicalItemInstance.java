package io.autoptu.cobblemon.authority;

public record CanonicalItemInstance(
        String itemInstanceId,
        String ownerPlayerId,
        String templateId,
        int quantity,
        long revision
) {
    public CanonicalItemInstance {
        if (itemInstanceId == null || itemInstanceId.isBlank()) {
            throw new IllegalArgumentException("itemInstanceId must not be blank");
        }
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            throw new IllegalArgumentException("ownerPlayerId must not be blank");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0");
        }
    }
}
