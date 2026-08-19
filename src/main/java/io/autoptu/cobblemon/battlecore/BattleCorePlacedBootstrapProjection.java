package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.Objects;

/**
 * Immutable adapter-owned bundle that binds canonical battle bootstrap state to the
 * initial authoritative grid placement for the same reservation.
 *
 * This record does not decide placement legality. Footprint overlap, collision,
 * facing, terrain, forced movement, targeting and every other PTU spatial rule remain
 * owned by AutoPTU-Java or deferred until an explicit upstream contract exists.
 */
public record BattleCorePlacedBootstrapProjection(
        String reservationId,
        BattleCoreBootstrapProjection combatState,
        BattleInitialPlacementSnapshot initialPlacement
) {
    public BattleCorePlacedBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        combatState = Objects.requireNonNull(combatState, "combatState");
        initialPlacement = Objects.requireNonNull(initialPlacement, "initialPlacement");

        if (!reservationId.equals(combatState.reservationId())) {
            throw new IllegalArgumentException("combat bootstrap belongs to a different battle reservation");
        }
        if (!reservationId.equals(initialPlacement.reservationId())) {
            throw new IllegalArgumentException("initial placement belongs to a different battle reservation");
        }
        if (!combatState.combatantIds().equals(initialPlacement.placementsByCombatant().keySet())) {
            throw new IllegalArgumentException("initial placements must exactly cover the bootstrapped combatant roster");
        }
    }

    public static BattleCorePlacedBootstrapProjection from(
            BattleAuthoritySnapshot battle,
            BattleInitialPlacementSnapshot initialPlacement
    ) {
        Objects.requireNonNull(battle, "battle");
        Objects.requireNonNull(initialPlacement, "initialPlacement");
        return new BattleCorePlacedBootstrapProjection(
                battle.reservationId(),
                BattleCoreBootstrapProjection.from(battle),
                initialPlacement
        );
    }
}
