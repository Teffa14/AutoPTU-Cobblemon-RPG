package io.autoptu.cobblemon.battlecore;

/**
 * Display-only health update copied from an authoritative HP_PROJECTION command.
 * Damage calculation and HP mutation have already happened inside AutoPTU-Java.
 */
public record BattleHealthProjection(
        long sequence,
        int ordinal,
        String combatantId,
        int damage,
        int targetHp
) {
    public BattleHealthProjection {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal cannot be negative");
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId is required");
        }
        combatantId = combatantId.strip();
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
    }
}
