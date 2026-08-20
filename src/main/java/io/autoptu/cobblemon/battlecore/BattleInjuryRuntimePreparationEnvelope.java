package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Reservation-scoped handoff that attaches canonical persistent injury counts to the
 * existing pre-runtime preparation envelope without executing injury-sensitive PTU rules.
 *
 * AutoPTU-Java still owns Aura Storm scaling, Aura Break suppression/inversion, injury
 * generation, healing/rest, and any future injury-sensitive hooks. This record only proves
 * that the server-owned injury inputs belong to the exact prepared battle roster.
 */
public record BattleInjuryRuntimePreparationEnvelope(
        String reservationId,
        BattleRuntimePreparationEnvelope runtimePreparation,
        Map<String, BattleCombatantInjuryProjection> injuriesByCombatant
) {
    public BattleInjuryRuntimePreparationEnvelope {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        runtimePreparation = Objects.requireNonNull(runtimePreparation, "runtimePreparation");
        if (!reservationId.equals(runtimePreparation.reservationId())) {
            throw new IllegalArgumentException("runtime preparation belongs to a different battle reservation");
        }
        if (injuriesByCombatant == null) {
            throw new IllegalArgumentException("injuriesByCombatant is required");
        }

        LinkedHashMap<String, BattleCombatantInjuryProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantInjuryProjection> entry : injuriesByCombatant.entrySet()) {
            String combatantId = normalizeId(entry.getKey(), "injury map key");
            BattleCombatantInjuryProjection projection = Objects.requireNonNull(
                    entry.getValue(), "injury projection");
            if (!combatantId.equals(projection.combatantId())) {
                throw new IllegalArgumentException("injury map key must match embedded combatantId");
            }
            if (copy.put(combatantId, projection) != null) {
                throw new IllegalArgumentException("duplicate injury combatant");
            }
        }
        if (!copy.keySet().equals(runtimePreparation.combatants().keySet())) {
            throw new IllegalArgumentException("canonical injuries must exactly cover the runtime preparation roster");
        }
        injuriesByCombatant = Map.copyOf(copy);
    }

    public static BattleInjuryRuntimePreparationEnvelope from(
            BattleRuntimePreparationEnvelope runtimePreparation,
            BattleCoreInjuryBootstrapProjection injuryBootstrap
    ) {
        Objects.requireNonNull(runtimePreparation, "runtimePreparation");
        Objects.requireNonNull(injuryBootstrap, "injuryBootstrap");
        if (!runtimePreparation.reservationId().equals(injuryBootstrap.reservationId())) {
            throw new IllegalArgumentException("injury and runtime preparation artifacts span different battle reservations");
        }
        return new BattleInjuryRuntimePreparationEnvelope(
                runtimePreparation.reservationId(),
                runtimePreparation,
                injuryBootstrap.injuriesByCombatant()
        );
    }

    private static String normalizeId(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
