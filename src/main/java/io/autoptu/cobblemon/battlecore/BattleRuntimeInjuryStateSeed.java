package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adapter-neutral seed for AutoPTU-Java BattleRuntimeState.injuryHistory().
 *
 * Only current canonical injury counts are transported. Previous-round and last-round
 * history are intentionally absent because AutoPTU-Java RoundInjuryHistoryState owns
 * those snapshots and rotates them through the authoritative battle lifecycle.
 */
public record BattleRuntimeInjuryStateSeed(
        String reservationId,
        Set<String> combatantRoster,
        Map<String, Integer> currentInjuriesByCombatant
) {
    public BattleRuntimeInjuryStateSeed {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        if (combatantRoster == null || combatantRoster.isEmpty()) {
            throw new IllegalArgumentException("combatantRoster is required");
        }
        if (currentInjuriesByCombatant == null) {
            throw new IllegalArgumentException("currentInjuriesByCombatant is required");
        }

        Set<String> normalizedRoster = combatantRoster.stream()
                .map(id -> normalizeId(id, "combatant roster id"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        LinkedHashMap<String, Integer> injuries = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : currentInjuriesByCombatant.entrySet()) {
            String combatantId = normalizeId(entry.getKey(), "injury combatant id");
            Integer count = Objects.requireNonNull(entry.getValue(), "injury count");
            if (count < 0) {
                throw new IllegalArgumentException("injury count cannot be negative");
            }
            if (injuries.put(combatantId, count) != null) {
                throw new IllegalArgumentException("duplicate injury combatant");
            }
        }
        if (!injuries.keySet().equals(normalizedRoster)) {
            throw new IllegalArgumentException("current injuries must exactly cover the prepared combatant roster");
        }
        combatantRoster = normalizedRoster;
        currentInjuriesByCombatant = Map.copyOf(injuries);
    }

    public static BattleRuntimeInjuryStateSeed from(BattleInjuryRuntimePreparationEnvelope preparation) {
        Objects.requireNonNull(preparation, "preparation");
        LinkedHashMap<String, Integer> injuries = new LinkedHashMap<>();
        preparation.injuriesByCombatant().forEach((combatantId, projection) ->
                injuries.put(combatantId, projection.injuries()));
        return new BattleRuntimeInjuryStateSeed(
                preparation.reservationId(),
                preparation.runtimePreparation().combatants().keySet(),
                injuries
        );
    }

    private static String normalizeId(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
