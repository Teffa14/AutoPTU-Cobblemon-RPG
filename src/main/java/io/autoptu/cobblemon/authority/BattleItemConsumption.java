package io.autoptu.cobblemon.authority;

public record BattleItemConsumption(
        String itemInstanceId,
        String templateId,
        int quantity,
        long expectedRevision
) {
    public BattleItemConsumption {
        if (itemInstanceId == null || itemInstanceId.isBlank()) {
            throw new IllegalArgumentException("itemInstanceId must not be blank");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must be >= 0");
        }
    }
}
