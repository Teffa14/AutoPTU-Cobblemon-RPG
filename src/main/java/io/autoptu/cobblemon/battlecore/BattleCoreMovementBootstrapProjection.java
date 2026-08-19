package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Binds canonical base movement to the same reservation and roster as the placed battle bootstrap.
 *
 * The integration layer carries persistent base movement and initial anchors only. It does not
 * construct AutoPTU-Java MovementProfile because battle-scoped capabilities and modifiers remain
 * core-owned and may change as statuses, abilities, items, weather, terrain and Trainer Features
 * are resolved.
 */
public record BattleCoreMovementBootstrapProjection(
        String reservationId,
        BattleCorePlacedBootstrapProjection placedBootstrap,
        Map<String, BattleCombatantBaseMovementProjection> baseMovementByCombatant
) {
    public BattleCoreMovementBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        placedBootstrap = Objects.requireNonNull(placedBootstrap, "placedBootstrap");
        if (!reservationId.equals(placedBootstrap.reservationId())) {
            throw new IllegalArgumentException("placed bootstrap belongs to a different battle reservation");
        }
        if (baseMovementByCombatant == null) {
            throw new IllegalArgumentException("baseMovementByCombatant is required");
        }

        LinkedHashMap<String, BattleCombatantBaseMovementProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantBaseMovementProjection> entry : baseMovementByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("base movement map key must not be blank");
            }
            combatantId = combatantId.strip();
            BattleCombatantBaseMovementProjection movement = Objects.requireNonNull(
                    entry.getValue(), "base movement projection");
            if (!combatantId.equals(movement.combatantId())) {
                throw new IllegalArgumentException("base movement map key must match embedded combatantId");
            }
            if (copy.put(combatantId, movement) != null) {
                throw new IllegalArgumentException("duplicate base movement projection");
            }
        }

        if (!copy.keySet().equals(placedBootstrap.combatState().combatantIds())) {
            throw new IllegalArgumentException("canonical base movement must exactly cover the bootstrapped combatant roster");
        }
        baseMovementByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreMovementBootstrapProjection from(
            BattleAuthoritySnapshot battle,
            BattleInitialPlacementSnapshot initialPlacement
    ) {
        Objects.requireNonNull(battle, "battle");
        Objects.requireNonNull(initialPlacement, "initialPlacement");
        BattleCorePlacedBootstrapProjection placed = BattleCorePlacedBootstrapProjection.from(battle, initialPlacement);

        LinkedHashMap<String, BattleCombatantBaseMovementProjection> movement = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : battle.roster()) {
            BattleCombatantBaseMovementProjection projection = BattleCombatantBaseMovementProjection.from(pokemon);
            movement.put(projection.combatantId(), projection);
        }
        return new BattleCoreMovementBootstrapProjection(battle.reservationId(), placed, movement);
    }
}
