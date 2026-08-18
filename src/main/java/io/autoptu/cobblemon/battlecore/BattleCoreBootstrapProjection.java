package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

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
        Map<String, Set<String>> statusesByCombatant,
        Map<String, BattleCombatantStatProjection> combatStatsByCombatant
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

        LinkedHashMap<String, BattleCombatantStatProjection> copiedCombatStats = new LinkedHashMap<>();
        if (combatStatsByCombatant != null) {
            for (Map.Entry<String, BattleCombatantStatProjection> entry : combatStatsByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.contains(combatantId)) {
                    throw new IllegalArgumentException("combat stat state references unknown combatant: " + combatantId);
                }
                BattleCombatantStatProjection stats = entry.getValue();
                if (stats == null) {
                    throw new IllegalArgumentException("combat stats must not be null for combatant: " + combatantId);
                }
                if (!combatantId.equals(stats.combatantId())) {
                    throw new IllegalArgumentException("combat stat projection id mismatch for combatant: " + combatantId);
                }
                copiedCombatStats.put(combatantId, stats);
            }
        }

        combatantIds = Set.copyOf(copiedCombatantIds);
        statusesByCombatant = Map.copyOf(copiedStatuses);
        combatStatsByCombatant = Map.copyOf(copiedCombatStats);
    }

    /** Compatibility constructor retained for pre-stat bootstrap callers. */
    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Set<String> combatantIds,
            Map<String, Set<String>> statusesByCombatant
    ) {
        this(reservationId, rngSeed, combatantIds, statusesByCombatant, Map.of());
    }

    /**
     * Compatibility constructor for callers that only projected status-bearing
     * combatants. New bootstrap code should use {@link #from(BattleAuthoritySnapshot)}
     * so clean combatants and canonical combat stats are included as well.
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
                statusesByCombatant,
                Map.of()
        );
    }

    public static BattleCoreBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        BattleCoreCombatantRosterProjection roster = BattleCoreCombatantRosterProjection.from(snapshot);
        LinkedHashMap<String, BattleCombatantStatProjection> combatStats = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantStatProjection projection = BattleCombatantStatProjection.from(pokemon);
            combatStats.put(projection.combatantId(), projection);
        }
        if (!combatStats.keySet().equals(roster.combatantIds())) {
            throw new IllegalArgumentException("canonical combat stats must cover the authoritative roster exactly");
        }

        return new BattleCoreBootstrapProjection(
                snapshot.reservationId(),
                snapshot.rngSeed(),
                roster.combatantIds(),
                roster.statusesByCombatant(),
                combatStats
        );
    }
}
