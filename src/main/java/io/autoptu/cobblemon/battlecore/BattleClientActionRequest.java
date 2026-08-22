package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Minimal client-originated battle intent. These records carry identity and intent only;
 * AutoPTU-Java remains responsible for action budget, frequency, targeting, movement,
 * range, line of sight, accuracy, damage, effects and every other legality/result.
 */
public sealed interface BattleClientActionRequest
        permits BattleClientActionRequest.Shift, BattleClientActionRequest.Move {

    String reservationId();
    String actorId();

    record Shift(String reservationId, String actorId, BattleGridCoordinate destination)
            implements BattleClientActionRequest {
        public Shift {
            reservationId = normalize(reservationId, "reservationId");
            actorId = normalize(actorId, "actorId");
            destination = Objects.requireNonNull(destination, "destination");
        }
    }

    record Move(String reservationId, String actorId, String moveId, Target target)
            implements BattleClientActionRequest {
        public Move {
            reservationId = normalize(reservationId, "reservationId");
            actorId = normalize(actorId, "actorId");
            moveId = normalize(moveId, "moveId");
            target = Objects.requireNonNull(target, "target");
        }
    }

    record Target(Mode mode, String combatantId, BattleGridCoordinate tile) {
        public enum Mode { COMBATANT, TILE, SELF, FIELD }

        public Target {
            mode = Objects.requireNonNull(mode, "mode");
            combatantId = combatantId == null ? null : normalize(combatantId, "combatantId");
            switch (mode) {
                case COMBATANT -> {
                    if (combatantId == null || tile != null) {
                        throw new IllegalArgumentException("COMBATANT target requires only combatantId");
                    }
                }
                case TILE -> {
                    if (tile == null || combatantId != null) {
                        throw new IllegalArgumentException("TILE target requires only tile");
                    }
                }
                case SELF, FIELD -> {
                    if (combatantId != null || tile != null) {
                        throw new IllegalArgumentException(mode + " target carries no adapter-supplied target payload");
                    }
                }
            }
        }

        public static Target combatant(String combatantId) {
            return new Target(Mode.COMBATANT, combatantId, null);
        }

        public static Target tile(BattleGridCoordinate tile) {
            return new Target(Mode.TILE, null, tile);
        }

        public static Target self() {
            return new Target(Mode.SELF, null, null);
        }

        public static Target field() {
            return new Target(Mode.FIELD, null, null);
        }
    }

    private static String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
