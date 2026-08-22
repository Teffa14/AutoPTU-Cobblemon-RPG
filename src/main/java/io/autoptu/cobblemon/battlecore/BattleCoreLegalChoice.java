package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Adapter-neutral projection of one BattleChoice already produced by AutoPTU-Java.
 *
 * This type does not calculate legality. The integration layer may only select from
 * instances projected from the core's legal choice list for the current runtime state.
 */
public sealed interface BattleCoreLegalChoice
        permits BattleCoreLegalChoice.Shift, BattleCoreLegalChoice.Move {

    String actorId();
    String stableKey();

    record Shift(String actorId, BattleGridCoordinate destination, String stableKey)
            implements BattleCoreLegalChoice {
        public Shift {
            actorId = normalize(actorId, "actorId");
            destination = Objects.requireNonNull(destination, "destination");
            stableKey = normalize(stableKey, "stableKey");
        }
    }

    record Move(
            String actorId,
            String moveId,
            BattleClientActionRequest.Target.Mode targetMode,
            String targetId,
            BattleGridCoordinate targetAnchor,
            String actionType,
            String stableKey
    ) implements BattleCoreLegalChoice {
        public Move {
            actorId = normalize(actorId, "actorId");
            moveId = normalize(moveId, "moveId");
            targetMode = Objects.requireNonNull(targetMode, "targetMode");
            targetAnchor = Objects.requireNonNull(targetAnchor, "targetAnchor");
            actionType = normalize(actionType, "actionType");
            stableKey = normalize(stableKey, "stableKey");
            targetId = targetId == null ? null : normalize(targetId, "targetId");
            if (targetMode == BattleClientActionRequest.Target.Mode.COMBATANT) {
                if (targetId == null) {
                    throw new IllegalArgumentException("COMBATANT legal choice requires targetId");
                }
            } else if (targetId != null) {
                throw new IllegalArgumentException(targetMode + " legal choice must not carry targetId");
            }
        }
    }

    private static String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
