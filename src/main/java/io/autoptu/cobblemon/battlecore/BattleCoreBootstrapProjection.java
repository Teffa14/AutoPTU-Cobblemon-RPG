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
        Map<String, BattleCombatantStatProjection> combatStatsByCombatant,
        Map<String, BattleCombatantHealthProjection> healthByCombatant,
        Map<String, BattleCombatantAffiliationProjection> affiliationByCombatant,
        Map<String, BattleCombatantMoveLoadoutProjection> moveLoadoutsByCombatant
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

        LinkedHashMap<String, BattleCombatantHealthProjection> copiedHealth = new LinkedHashMap<>();
        if (healthByCombatant != null) {
            for (Map.Entry<String, BattleCombatantHealthProjection> entry : healthByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.contains(combatantId)) {
                    throw new IllegalArgumentException("health state references unknown combatant: " + combatantId);
                }
                BattleCombatantHealthProjection health = entry.getValue();
                if (health == null) {
                    throw new IllegalArgumentException("health must not be null for combatant: " + combatantId);
                }
                if (!combatantId.equals(health.combatantId())) {
                    throw new IllegalArgumentException("health projection id mismatch for combatant: " + combatantId);
                }
                copiedHealth.put(combatantId, health);
            }
        }

        LinkedHashMap<String, BattleCombatantAffiliationProjection> copiedAffiliations = new LinkedHashMap<>();
        if (affiliationByCombatant != null) {
            for (Map.Entry<String, BattleCombatantAffiliationProjection> entry : affiliationByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.contains(combatantId)) {
                    throw new IllegalArgumentException("affiliation state references unknown combatant: " + combatantId);
                }
                BattleCombatantAffiliationProjection affiliation = entry.getValue();
                if (affiliation == null) {
                    throw new IllegalArgumentException("affiliation must not be null for combatant: " + combatantId);
                }
                if (!combatantId.equals(affiliation.combatantId())) {
                    throw new IllegalArgumentException("affiliation projection id mismatch for combatant: " + combatantId);
                }
                copiedAffiliations.put(combatantId, affiliation);
            }
        }

        LinkedHashMap<String, BattleCombatantMoveLoadoutProjection> copiedMoveLoadouts = new LinkedHashMap<>();
        if (moveLoadoutsByCombatant != null) {
            for (Map.Entry<String, BattleCombatantMoveLoadoutProjection> entry : moveLoadoutsByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                if (!copiedCombatantIds.contains(combatantId)) {
                    throw new IllegalArgumentException("move loadout references unknown combatant: " + combatantId);
                }
                BattleCombatantMoveLoadoutProjection loadout = entry.getValue();
                if (loadout == null) {
                    throw new IllegalArgumentException("move loadout must not be null for combatant: " + combatantId);
                }
                if (!combatantId.equals(loadout.combatantId())) {
                    throw new IllegalArgumentException("move loadout projection id mismatch for combatant: " + combatantId);
                }
                copiedMoveLoadouts.put(combatantId, loadout);
            }
        }

        combatantIds = Set.copyOf(copiedCombatantIds);
        statusesByCombatant = Map.copyOf(copiedStatuses);
        combatStatsByCombatant = Map.copyOf(copiedCombatStats);
        healthByCombatant = Map.copyOf(copiedHealth);
        affiliationByCombatant = Map.copyOf(copiedAffiliations);
        moveLoadoutsByCombatant = Map.copyOf(copiedMoveLoadouts);
    }

    /** Compatibility constructor retained for callers created before move loadout bootstrap projection. */
    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Set<String> combatantIds,
            Map<String, Set<String>> statusesByCombatant,
            Map<String, BattleCombatantStatProjection> combatStatsByCombatant,
            Map<String, BattleCombatantHealthProjection> healthByCombatant,
            Map<String, BattleCombatantAffiliationProjection> affiliationByCombatant
    ) {
        this(reservationId, rngSeed, combatantIds, statusesByCombatant, combatStatsByCombatant,
                healthByCombatant, affiliationByCombatant, Map.of());
    }

    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Set<String> combatantIds,
            Map<String, Set<String>> statusesByCombatant,
            Map<String, BattleCombatantStatProjection> combatStatsByCombatant,
            Map<String, BattleCombatantHealthProjection> healthByCombatant
    ) {
        this(reservationId, rngSeed, combatantIds, statusesByCombatant, combatStatsByCombatant,
                healthByCombatant, Map.of(), Map.of());
    }

    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Set<String> combatantIds,
            Map<String, Set<String>> statusesByCombatant,
            Map<String, BattleCombatantStatProjection> combatStatsByCombatant
    ) {
        this(reservationId, rngSeed, combatantIds, statusesByCombatant, combatStatsByCombatant,
                Map.of(), Map.of(), Map.of());
    }

    /** Compatibility constructor retained for pre-stat bootstrap callers. */
    public BattleCoreBootstrapProjection(
            String reservationId,
            long rngSeed,
            Set<String> combatantIds,
            Map<String, Set<String>> statusesByCombatant
    ) {
        this(reservationId, rngSeed, combatantIds, statusesByCombatant,
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    /**
     * Compatibility constructor for callers that only projected status-bearing
     * combatants. New bootstrap code should use {@link #from(BattleAuthoritySnapshot)}.
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
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public static BattleCoreBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        BattleCoreCombatantRosterProjection roster = BattleCoreCombatantRosterProjection.from(snapshot);
        LinkedHashMap<String, BattleCombatantStatProjection> combatStats = new LinkedHashMap<>();
        LinkedHashMap<String, BattleCombatantHealthProjection> health = new LinkedHashMap<>();
        LinkedHashMap<String, BattleCombatantAffiliationProjection> affiliations = new LinkedHashMap<>();
        LinkedHashMap<String, BattleCombatantMoveLoadoutProjection> moveLoadouts = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantStatProjection statProjection = BattleCombatantStatProjection.from(pokemon);
            combatStats.put(statProjection.combatantId(), statProjection);
            BattleCombatantHealthProjection healthProjection = BattleCombatantHealthProjection.from(pokemon);
            health.put(healthProjection.combatantId(), healthProjection);
            BattleCombatantAffiliationProjection affiliationProjection = BattleCombatantAffiliationProjection.from(pokemon);
            affiliations.put(affiliationProjection.combatantId(), affiliationProjection);
            BattleCombatantMoveLoadoutProjection moveLoadoutProjection = BattleCombatantMoveLoadoutProjection.from(pokemon);
            moveLoadouts.put(moveLoadoutProjection.combatantId(), moveLoadoutProjection);
        }
        if (!combatStats.keySet().equals(roster.combatantIds())) {
            throw new IllegalArgumentException("canonical combat stats must cover the authoritative roster exactly");
        }
        if (!health.keySet().equals(roster.combatantIds())) {
            throw new IllegalArgumentException("canonical health must cover the authoritative roster exactly");
        }
        if (!affiliations.keySet().equals(roster.combatantIds())) {
            throw new IllegalArgumentException("canonical affiliation must cover the authoritative roster exactly");
        }
        if (!moveLoadouts.keySet().equals(roster.combatantIds())) {
            throw new IllegalArgumentException("canonical move loadouts must cover the authoritative roster exactly");
        }

        return new BattleCoreBootstrapProjection(
                snapshot.reservationId(),
                snapshot.rngSeed(),
                roster.combatantIds(),
                roster.statusesByCombatant(),
                combatStats,
                health,
                affiliations,
                moveLoadouts
        );
    }
}
