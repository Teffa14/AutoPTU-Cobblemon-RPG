package io.autoptu.cobblemon.battlecore;

import java.util.Objects;
import java.util.Set;

/**
 * Final integration-owned assembly boundary before AutoPTU-Java runtime materialization.
 *
 * This record joins the prepared combatant/move/item/status state, canonical Trainer runtime input,
 * and canonical battle rule/environment seed for exactly one server reservation. It deliberately
 * carries no resolved MovementProfile, dynamic accuracy/evasion flags, resolved damage modifiers,
 * lifecycle clock, initiative order/cursor, initiative rebuilder/strategy, temporary effects, or
 * rule outcomes. Current AutoPTU-Java production rollover selects InitiativeRoundRebuilder.authoritative
 * inside BattleRoundController, so an adapter-side lifecycle strategy has no valid place in this seed.
 *
 * Trainer and combatant identities must also be disjoint before runtime construction. AutoPTU-Java
 * initiative rollover treats both identity families as initiative actors; allowing one stable ID to
 * exist in both families can create a duplicate canonical order and partial cleanup before the core
 * rejects that duplicate. The integration fails closed before any runtime mutation.
 */
public record BattleRuntimeAssemblySeed(
        String reservationId,
        BattleTrainerRuntimePreparationEnvelope trainerPreparation,
        BattleRuntimeCanonicalStateSeed canonicalState
) {
    public BattleRuntimeAssemblySeed {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        trainerPreparation = Objects.requireNonNull(trainerPreparation, "trainerPreparation");
        canonicalState = Objects.requireNonNull(canonicalState, "canonicalState");

        requireReservation(reservationId, trainerPreparation.reservationId());
        requireReservation(reservationId, canonicalState.reservationId());
        if (!trainerPreparation.battle().equals(canonicalState.runtimePreparation())) {
            throw new IllegalArgumentException(
                    "runtime assembly must bind one identical prepared battle");
        }
        if (trainerPreparation.battle().combatants().containsKey(trainerPreparation.trainer().trainerId())) {
            throw new IllegalArgumentException(
                    "trainer identity must not collide with a combatant identity");
        }
    }

    public static BattleRuntimeAssemblySeed from(
            BattleTrainerRuntimePreparationEnvelope trainerPreparation,
            BattleRuntimeCanonicalStateSeed canonicalState
    ) {
        Objects.requireNonNull(trainerPreparation, "trainerPreparation");
        Objects.requireNonNull(canonicalState, "canonicalState");
        return new BattleRuntimeAssemblySeed(
                trainerPreparation.reservationId(), trainerPreparation, canonicalState);
    }

    public BattleRuntimePreparationEnvelope battle() {
        return trainerPreparation.battle();
    }

    public BattleTrainerRuntimeProjection trainer() {
        return trainerPreparation.trainer();
    }

    public BattleRuntimeRuleStateSeed ruleState() {
        return canonicalState.ruleState();
    }

    public BattleRuntimeEnvironmentSeed environmentState() {
        return canonicalState.environmentState();
    }

    public long rngSeed() {
        return battle().rngSeed();
    }

    public Set<RuntimeCombatantMaterializationReadiness.Requirement> unresolvedCoreRequirements() {
        return battle().unresolvedCoreRequirements();
    }

    public boolean readyForRuntimeMaterialization() {
        return battle().readyForRuntimeMaterialization();
    }

    private static void requireReservation(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "runtime assembly artifacts span different battle reservations");
        }
    }
}
