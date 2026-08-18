package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-owned combatant roster projection for AutoPTU-Java bootstrap.
 *
 * The integration layer derives both combatant identity and canonical statuses
 * from the same immutable battle reservation. Minecraft/Cobblemon adapters may
 * render these values, but cannot introduce extra combatants or status entries.
 */
public record BattleCoreCombatantRosterProjection(
        Set<String> combatantIds,
        Map<String, Set<String>> statusesByCombatant
) {
    public BattleCoreCombatantRosterProjection {
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

    public static BattleCoreCombatantRosterProjection from(BattleAuthoritySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        LinkedHashSet<String> combatantIds = new LinkedHashSet<>();
        LinkedHashMap<String, Set<String>> statuses = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            String combatantId = pokemon.pokemonId();
            if (!combatantIds.add(combatantId)) {
                throw new IllegalArgumentException("duplicate combatantId in battle snapshot: " + combatantId);
            }
            if (!pokemon.statuses().isEmpty()) {
                statuses.put(combatantId, pokemon.statuses());
            }
        }

        return new BattleCoreCombatantRosterProjection(combatantIds, statuses);
    }
}
