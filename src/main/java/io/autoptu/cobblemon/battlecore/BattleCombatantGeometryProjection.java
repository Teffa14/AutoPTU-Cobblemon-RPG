package io.autoptu.cobblemon.battlecore;

/**
 * Immutable server-owned PTU spatial metadata for one combatant.
 *
 * The size label is canonical battle input. Minecraft/Cobblemon model dimensions are
 * presentation data and must never be used to determine PTU footprint size.
 */
public record BattleCombatantGeometryProjection(String combatantId, String sizeLabel) {
    public BattleCombatantGeometryProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        if (sizeLabel == null || sizeLabel.isBlank()) {
            throw new IllegalArgumentException("sizeLabel must not be blank");
        }
        combatantId = combatantId.strip();
        sizeLabel = sizeLabel.strip();
    }
}
