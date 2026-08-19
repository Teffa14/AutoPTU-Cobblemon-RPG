package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Server-owned initial anchor for one reserved combatant on the authoritative PTU grid.
 *
 * This record carries identity and an already-chosen grid coordinate only. It does not
 * decide footprint overlap, collision, terrain, facing, movement legality, forced
 * movement, targeting, or any other PTU rule.
 */
public record BattleCombatantInitialPlacement(
        String combatantId,
        BattleGridCoordinate anchor
) {
    public BattleCombatantInitialPlacement {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        anchor = Objects.requireNonNull(anchor, "anchor");
    }
}
