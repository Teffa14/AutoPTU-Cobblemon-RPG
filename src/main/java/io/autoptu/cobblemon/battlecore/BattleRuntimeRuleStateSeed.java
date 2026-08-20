package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Reservation-scoped handoff for the server-owned rule state that integration may
 * seed before AutoPTU-Java takes over battle lifecycle and hook execution.
 *
 * The runtime preparation already carries canonical combatant identity, position,
 * HP, team/active affiliation, types, abilities, moves, statuses and held items.
 * This seed binds that preparation to the current canonical injury counts required
 * by BattleRuntimeState injury history.
 *
 * Battle round, injury-history rotation, Aura Break blocker selection, Aura Storm
 * resolution and every later rule mutation are deliberately absent. AutoPTU-Java
 * owns those values and behaviors.
 */
public record BattleRuntimeRuleStateSeed(
        String reservationId,
        BattleRuntimePreparationEnvelope runtimePreparation,
        BattleRuntimeInjuryStateSeed injuryState
) {
    public BattleRuntimeRuleStateSeed {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        runtimePreparation = Objects.requireNonNull(runtimePreparation, "runtimePreparation");
        injuryState = Objects.requireNonNull(injuryState, "injuryState");

        requireReservation(reservationId, runtimePreparation.reservationId());
        requireReservation(reservationId, injuryState.reservationId());
        if (!runtimePreparation.combatants().keySet().equals(injuryState.combatantRoster())) {
            throw new IllegalArgumentException("runtime rule state must exactly cover the prepared combatant roster");
        }
    }

    public static BattleRuntimeRuleStateSeed from(BattleInjuryRuntimePreparationEnvelope preparation) {
        Objects.requireNonNull(preparation, "preparation");
        BattleRuntimePreparationEnvelope runtimePreparation = preparation.runtimePreparation();
        return new BattleRuntimeRuleStateSeed(
                preparation.reservationId(),
                runtimePreparation,
                BattleRuntimeInjuryStateSeed.from(preparation)
        );
    }

    private static void requireReservation(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("runtime rule-state artifacts span different battle reservations");
        }
    }
}
