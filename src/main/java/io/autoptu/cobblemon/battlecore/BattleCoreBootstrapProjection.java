package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
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
        Map<String, Set<String>> statusesByCombatant
) {
    public BattleCoreBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }

        LinkedHashMap<String, Set<String>> copiedStatuses = new LinkedHashMap<>();
        if (statusesByCombatant != null) {
            for (Map.Entry<String, Set<String>> entry : statusesByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("combatantId must not be blank");
                }
                Set<String> statuses = entry.getValue() == null ? Set.of() : Set.copyOf(entry.getValue());
                if (!statuses.isEmpty()) {
                    copiedStatuses.put(combatantId, statuses);
                }
            }
        }
        statusesByCombatant = Map.copyOf(copiedStatuses);
    }

    public static BattleCoreBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        LinkedHashMap<String, Set<String>> statuses = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            if (!pokemon.statuses().isEmpty()) {
                statuses.put(pokemon.pokemonId(), pokemon.statuses());
            }
        }

        return new BattleCoreBootstrapProjection(snapshot.reservationId(), snapshot.rngSeed(), statuses);
    }
}
