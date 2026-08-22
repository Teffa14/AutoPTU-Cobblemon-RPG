package io.autoptu.cobblemon.battlecore;

import java.util.Locale;
import java.util.Objects;

/**
 * Strict decoder from primitive transport fields into the minimal battle-intent contract.
 * Unknown action/target modes and contradictory payload fields fail closed.
 */
public final class BattleClientActionPacketDecoder {
    private BattleClientActionPacketDecoder() {}

    public static BattleClientActionRequest decode(BattleClientActionPacket packet) {
        Objects.requireNonNull(packet, "packet");
        String actionKind = normalizeEnum(packet.actionKind(), "actionKind");
        return switch (actionKind) {
            case "SHIFT" -> decodeShift(packet);
            case "MOVE" -> decodeMove(packet);
            default -> throw new IllegalArgumentException("unsupported actionKind: " + actionKind);
        };
    }

    private static BattleClientActionRequest.Shift decodeShift(BattleClientActionPacket packet) {
        requireAbsent(packet.moveId(), "moveId", "SHIFT");
        requireAbsent(packet.targetMode(), "targetMode", "SHIFT");
        requireAbsent(packet.targetCombatantId(), "targetCombatantId", "SHIFT");
        BattleGridCoordinate destination = requireTile(packet.targetX(), packet.targetY(), "SHIFT destination");
        return new BattleClientActionRequest.Shift(packet.reservationId(), packet.actorId(), destination);
    }

    private static BattleClientActionRequest.Move decodeMove(BattleClientActionPacket packet) {
        String moveId = requireText(packet.moveId(), "moveId");
        String targetMode = normalizeEnum(packet.targetMode(), "targetMode");
        BattleClientActionRequest.Target target = switch (targetMode) {
            case "COMBATANT" -> {
                requireCoordinatesAbsent(packet.targetX(), packet.targetY(), "COMBATANT");
                yield BattleClientActionRequest.Target.combatant(
                        requireText(packet.targetCombatantId(), "targetCombatantId"));
            }
            case "TILE" -> {
                requireAbsent(packet.targetCombatantId(), "targetCombatantId", "TILE");
                yield BattleClientActionRequest.Target.tile(
                        requireTile(packet.targetX(), packet.targetY(), "TILE target"));
            }
            case "SELF" -> {
                requireAbsent(packet.targetCombatantId(), "targetCombatantId", "SELF");
                requireCoordinatesAbsent(packet.targetX(), packet.targetY(), "SELF");
                yield BattleClientActionRequest.Target.self();
            }
            case "FIELD" -> {
                requireAbsent(packet.targetCombatantId(), "targetCombatantId", "FIELD");
                requireCoordinatesAbsent(packet.targetX(), packet.targetY(), "FIELD");
                yield BattleClientActionRequest.Target.field();
            }
            default -> throw new IllegalArgumentException("unsupported targetMode: " + targetMode);
        };
        return new BattleClientActionRequest.Move(packet.reservationId(), packet.actorId(), moveId, target);
    }

    private static BattleGridCoordinate requireTile(Integer x, Integer y, String label) {
        if (x == null || y == null) {
            throw new IllegalArgumentException(label + " requires targetX and targetY");
        }
        return new BattleGridCoordinate(x, y);
    }

    private static void requireCoordinatesAbsent(Integer x, Integer y, String mode) {
        if (x != null || y != null) {
            throw new IllegalArgumentException(mode + " target must not carry tile coordinates");
        }
    }

    private static void requireAbsent(String value, String label, String mode) {
        if (value != null) {
            throw new IllegalArgumentException(mode + " must not carry " + label);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeEnum(String value, String label) {
        return requireText(value, label).toUpperCase(Locale.ROOT);
    }
}
