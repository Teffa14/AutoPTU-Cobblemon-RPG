package io.autoptu.cobblemon.battlecore;

/**
 * Primitive transport shape for a client battle action request.
 *
 * This record is intentionally transport-only. It does not carry an authenticated principal,
 * legality results, stats, modifiers, inventory truth, action budgets, move metadata or outcomes.
 * Server networking obtains the authenticated principal from the connection context and decodes
 * this packet into the narrower BattleClientActionRequest intent before consulting authoritative state.
 */
public record BattleClientActionPacket(
        String reservationId,
        String actorId,
        String actionKind,
        String moveId,
        String targetMode,
        String targetCombatantId,
        Integer targetX,
        Integer targetY
) {}
