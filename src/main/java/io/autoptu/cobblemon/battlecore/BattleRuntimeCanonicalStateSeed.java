package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Reservation-scoped package of the canonical rule/environment state that integration may seed
 * before AutoPTU-Java takes over lifecycle, initiative and hook execution.
 *
 * This boundary combines only state that has already been resolved by trusted server/domain code:
 * current injuries plus weather, PTU terrain identity, Tailwind teams, grounded state and mounted
 * rider relationships. It never seeds lifecycle clocks, initiative order/cursor, Trainer action
 * buckets, injury history, temporary effects or effect outcomes.
 */
public record BattleRuntimeCanonicalStateSeed(
        String reservationId,
        BattleRuntimeRuleStateSeed ruleState,
        BattleRuntimeEnvironmentSeed environmentState
) {
    public BattleRuntimeCanonicalStateSeed {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        ruleState = Objects.requireNonNull(ruleState, "ruleState");
        environmentState = Objects.requireNonNull(environmentState, "environmentState");

        requireReservation(reservationId, ruleState.reservationId());
        requireReservation(reservationId, environmentState.reservationId());
        if (!ruleState.runtimePreparation().equals(environmentState.runtimePreparation())) {
            throw new IllegalArgumentException(
                    "runtime canonical state must bind one identical prepared battle");
        }
    }

    public BattleRuntimePreparationEnvelope runtimePreparation() {
        return ruleState.runtimePreparation();
    }

    private static void requireReservation(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "runtime canonical-state artifacts span different battle reservations");
        }
    }
}
