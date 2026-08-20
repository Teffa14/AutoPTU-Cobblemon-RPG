package io.autoptu.cobblemon.battlecore;

/**
 * Authoritative move presentation command with both combatant endpoints bound to the exact
 * opaque presentation entities registered for the battle reservation.
 *
 * This record carries rendering identity only. It never resolves targeting, hit/crit,
 * damage, move effects, or any other PTU rule.
 */
public record EntityBoundMoveAnimation(
        BattlePresentationCommand command,
        String attackerPresentationEntityId,
        String targetPresentationEntityId
) {
    public EntityBoundMoveAnimation {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (command.kind() != BattlePresentationCommand.Kind.MOVE_ANIMATION) {
            throw new IllegalArgumentException("command must be MOVE_ANIMATION");
        }
        attackerPresentationEntityId = requireId(attackerPresentationEntityId, "attackerPresentationEntityId");
        targetPresentationEntityId = requireId(targetPresentationEntityId, "targetPresentationEntityId");
        requireId(command.data().get("targetId"), "targetId");
        requireId(command.data().get("moveId"), "moveId");
    }

    public String attackerCombatantId() {
        return command.subjectId();
    }

    public String targetCombatantId() {
        return requireId(command.data().get("targetId"), "targetId");
    }

    public String moveId() {
        return requireId(command.data().get("moveId"), "moveId");
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
