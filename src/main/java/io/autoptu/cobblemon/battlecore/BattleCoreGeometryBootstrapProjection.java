package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Binds canonical PTU combatant geometry to an already validated movement/placement bootstrap.
 *
 * This boundary transports authoritative size labels only. AutoPTU-Java owns footprint
 * expansion, range/collision semantics and every legality decision. Minecraft/Cobblemon
 * model scale, bounding boxes and render dimensions are never battle inputs here.
 */
public record BattleCoreGeometryBootstrapProjection(
        String reservationId,
        BattleCoreMovementBootstrapProjection movementBootstrap,
        Map<String, BattleCombatantGeometryProjection> geometryByCombatant
) {
    public BattleCoreGeometryBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        movementBootstrap = Objects.requireNonNull(movementBootstrap, "movementBootstrap");
        if (!reservationId.equals(movementBootstrap.reservationId())) {
            throw new IllegalArgumentException("movement bootstrap belongs to a different battle reservation");
        }
        if (geometryByCombatant == null) {
            throw new IllegalArgumentException("geometryByCombatant is required");
        }

        LinkedHashMap<String, BattleCombatantGeometryProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantGeometryProjection> entry : geometryByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("geometry map key must not be blank");
            }
            combatantId = combatantId.strip();
            BattleCombatantGeometryProjection geometry = Objects.requireNonNull(entry.getValue(), "geometry projection");
            if (!combatantId.equals(geometry.combatantId())) {
                throw new IllegalArgumentException("geometry map key must match embedded combatantId");
            }
            if (copy.put(combatantId, geometry) != null) {
                throw new IllegalArgumentException("duplicate geometry projection");
            }
        }

        if (!copy.keySet().equals(movementBootstrap.placedBootstrap().combatState().combatantIds())) {
            throw new IllegalArgumentException("canonical geometry must exactly cover the bootstrapped combatant roster");
        }
        geometryByCombatant = Map.copyOf(copy);
    }
}
