package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Project-owned DTO for bootstrapping the evolving AutoPTU-Java battle runtime.
 *
 * This boundary deliberately contains no Minecraft or Cobblemon types. The
 * integration layer owns the projection from its validated immutable battle
 * reservation, while AutoPTU-Java remains the authority for battle resolution.
 */
public record BattleCoreBootstrapProjection(
        String reservationId,
        long rngSeed,
        Set<String> combatantIds,
        Map<String, Set<String>> statusesByCombatant
) {
    public BattleCoreBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }

        LinkedHashSet<String> copiedCombatantIds = new LinkedHashSet<>();
        if (combatantIds != null) {
            for (String combatantId : combatantIds) {
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.add(combatantId)) {
                    throw new IllegalArgumentException("duplicate combatantId: " + combatantId);
                }
            }
        }

        LinkedHashMap<String, Set<String>> copiedStatuses = new LinkedHashMap<>();
        if (statusesByCombatant != null) {
            for (Map.Entry<String, Set<String>> entry : statusesByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.contains(combatantId)) {
                    throw new IllegalArgumentException("status state references unknown combatant: " + combatantId);
                }
                Set<String> statuses = entry.getValue() == null ? Set.of() : Set.copyOf(entry.getValue());
                if (!statuses.isEmpty()) {
                    copiedStatuses.put(combatantId, statuses);
                }
            }
        }

        combatantIds = Set.copyOf(copiedCombatantIds);
        statusesByCombatant = Map.copyOf(copiedStatuses);
    }

    /**
     * Compatibility constructor for callers that only projected status-bearing
     * combatants. New bootstrap code should use {@link #from(BattleAuthoritySnapshot)}
     * so clean combatants are included in the authoritative roster as well.
     */
    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Map<String, Set<String>> statusesByCombatant
    ) {
        this(
                reservationId,
                rngSeed,
                statusesByCombatant == null ? Set.of() : statusesByCombatant.keySet(),
                statusesByCombatant
        );
    }

    public static BattleCoreBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        BattleCoreCombatantRosterProjection roster = BattleCoreCombatantRosterProjection.from(snapshot);
        return new BattleCoreBootstrapProjection(
                snapshot.reservationId(),
                snapshot.rngSeed(),
                roster.combatantIds(),
                roster.statusesByCombatant()
        );
    }
}
