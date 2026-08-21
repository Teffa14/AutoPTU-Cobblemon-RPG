package io.autoptu.cobblemon.battlecore;

/**
 * Display-only HP projection bound to the exact presentation entity registered for the combatant.
 * The opaque presentation entity carries no PTU authority.
 */
public record EntityBoundBattleHealthProjection(
        long sequence,
        int ordinal,
        String combatantId,
        String presentationEntityId,
        int damage,
        int targetHp
) implements EntityBoundPresentationOutput {
    public EntityBoundBattleHealthProjection {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal cannot be negative");
        if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("combatantId is required");
        if (presentationEntityId == null || presentationEntityId.isBlank()) throw new IllegalArgumentException("presentationEntityId is required");
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
        combatantId = combatantId.strip();
        presentationEntityId = presentationEntityId.strip();
    }
}
