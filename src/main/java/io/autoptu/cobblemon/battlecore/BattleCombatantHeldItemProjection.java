package io.autoptu.cobblemon.battlecore;

/**
 * Server-owned held-item identity prepared for AutoPTU-Java BattleRuntimeState.
 *
 * The stable item instance ID remains separate from its canonical template/catalog
 * identity so duplicate copies can later be consumed or replaced independently.
 * This DTO carries identity only; it never executes or approximates item effects.
 */
public record BattleCombatantHeldItemProjection(
        String combatantId,
        String itemInstanceId,
        String itemTemplateId
) {
    public BattleCombatantHeldItemProjection {
        combatantId = requireIdentifier(combatantId, "combatantId");
        itemInstanceId = requireIdentifier(itemInstanceId, "itemInstanceId");
        itemTemplateId = requireIdentifier(itemTemplateId, "itemTemplateId");
    }

    private static String requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
