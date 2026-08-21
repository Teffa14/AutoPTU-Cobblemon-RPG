package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation-scoped seed for server-owned battle environment and spatial relationship state.
 *
 * Weather, PTU terrain identity, Tailwind team state, grounded state and mounted rider->mount
 * relationships must already be resolved by trusted server/domain code before this boundary.
 * Minecraft world observations, entity pose/passenger state and client payloads are not accepted
 * as PTU semantics.
 */
public record BattleRuntimeEnvironmentSeed(
        String reservationId,
        BattleRuntimePreparationEnvelope runtimePreparation,
        String weather,
        String terrainName,
        Set<String> tailwindTeams,
        Map<String, Boolean> groundedByCombatant,
        Map<String, String> mountedPairs
) {
    /** Compatibility constructor for callers without explicit mounted relationship state. */
    public BattleRuntimeEnvironmentSeed(
            String reservationId,
            BattleRuntimePreparationEnvelope runtimePreparation,
            String weather,
            String terrainName,
            Set<String> tailwindTeams,
            Map<String, Boolean> groundedByCombatant
    ) {
        this(reservationId, runtimePreparation, weather, terrainName, tailwindTeams, groundedByCombatant, Map.of());
    }

    public BattleRuntimeEnvironmentSeed {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        runtimePreparation = Objects.requireNonNull(runtimePreparation, "runtimePreparation");
        if (!reservationId.equals(runtimePreparation.reservationId())) {
            throw new IllegalArgumentException("runtime environment must match the prepared battle reservation");
        }

        weather = normalizeOptional(weather);
        terrainName = normalizeOptional(terrainName);
        tailwindTeams = copyIds(tailwindTeams, "tailwind team");
        groundedByCombatant = copyGrounded(groundedByCombatant);

        Set<String> roster = runtimePreparation.combatants().keySet();
        if (!groundedByCombatant.keySet().equals(roster)) {
            throw new IllegalArgumentException("grounded state must exactly cover the authoritative combatant roster");
        }

        LinkedHashSet<String> authoritativeTeams = new LinkedHashSet<>();
        runtimePreparation.combatants().values().forEach(input -> authoritativeTeams.add(input.affiliation().teamId()));
        if (!authoritativeTeams.containsAll(tailwindTeams)) {
            throw new IllegalArgumentException("Tailwind may reference only authoritative battle teams");
        }

        mountedPairs = copyMountedPairs(mountedPairs, roster);
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.strip();
    }

    private static Set<String> copyIds(Set<String> source, String label) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        if (source != null) {
            for (String value : source) {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException(label + " must not be blank");
                }
                String normalized = value.strip();
                if (!copy.add(normalized)) {
                    throw new IllegalArgumentException("duplicate " + label);
                }
            }
        }
        return Set.copyOf(copy);
    }

    private static Map<String, Boolean> copyGrounded(Map<String, Boolean> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("groundedByCombatant must not be empty");
        }
        LinkedHashMap<String, Boolean> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : source.entrySet()) {
            String id = entry.getKey();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("grounded combatantId must not be blank");
            }
            id = id.strip();
            Boolean grounded = Objects.requireNonNull(entry.getValue(), "grounded state");
            if (copy.put(id, grounded) != null) {
                throw new IllegalArgumentException("duplicate grounded combatantId");
            }
        }
        return Map.copyOf(copy);
    }

    private static Map<String, String> copyMountedPairs(Map<String, String> source, Set<String> roster) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        LinkedHashSet<String> assignedMounts = new LinkedHashSet<>();
        if (source == null) {
            return Map.of();
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String riderId = normalizeRequired(entry.getKey(), "mounted riderId");
            String mountId = normalizeRequired(entry.getValue(), "mounted mountId");
            if (!roster.contains(riderId) || !roster.contains(mountId)) {
                throw new IllegalArgumentException("mounted pairs may reference only authoritative combatants");
            }
            if (riderId.equals(mountId)) {
                throw new IllegalArgumentException("a combatant cannot mount itself");
            }
            if (copy.put(riderId, mountId) != null) {
                throw new IllegalArgumentException("duplicate mounted riderId");
            }
            if (!assignedMounts.add(mountId)) {
                throw new IllegalArgumentException("a mount may belong to only one rider in the battle snapshot");
            }
        }
        return Map.copyOf(copy);
    }

    private static String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
