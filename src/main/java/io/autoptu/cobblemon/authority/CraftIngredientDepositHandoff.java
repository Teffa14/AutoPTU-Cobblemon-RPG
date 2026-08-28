package io.autoptu.cobblemon.authority;

/** Durable cross-store handoff from one server-observed Minecraft inventory slot into canonical RPG inventory. */
public record CraftIngredientDepositHandoff(
        String handoffId,
        String playerId,
        String itemTemplateId,
        int inventorySlot,
        int beforeCount,
        int quantity,
        Phase phase
) {
    public enum Phase {
        PREPARED,
        WITHDRAWN,
        CANONICAL_APPLIED,
        COMMITTED,
        ABORTED
    }

    public CraftIngredientDepositHandoff {
        if (handoffId == null || handoffId.isBlank()) throw new IllegalArgumentException("handoffId is required");
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
        if (itemTemplateId == null || itemTemplateId.isBlank()) throw new IllegalArgumentException("itemTemplateId is required");
        if (inventorySlot < 0) throw new IllegalArgumentException("inventorySlot must be >= 0");
        if (beforeCount <= 0) throw new IllegalArgumentException("beforeCount must be positive");
        if (quantity <= 0 || quantity > beforeCount) throw new IllegalArgumentException("quantity must be positive and <= beforeCount");
        if (phase == null) throw new IllegalArgumentException("phase is required");
        handoffId = handoffId.strip();
        playerId = playerId.strip();
        itemTemplateId = itemTemplateId.strip();
    }

    public CraftIngredientDepositHandoff withPhase(Phase replacement) {
        return new CraftIngredientDepositHandoff(
                handoffId, playerId, itemTemplateId, inventorySlot, beforeCount, quantity, replacement);
    }

    public boolean terminal() {
        return phase == Phase.COMMITTED || phase == Phase.ABORTED;
    }
}
