package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

/**
 * Project-owned server-authoritative battle-side projection.
 *
 * Team identity comes from the canonical Pokémon owner frozen into the battle
 * reservation. Minecraft teams, scoreboards, passengers, entity metadata, and
 * client payloads are not accepted as affiliation authority.
 */
public record BattleCombatantAffiliationProjection(
        String combatantId,
        String teamId,
        boolean active
) {
    public BattleCombatantAffiliationProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        if (teamId == null || teamId.isBlank()) {
            throw new IllegalArgumentException("teamId must not be blank");
        }
        combatantId = combatantId.strip();
        teamId = teamId.strip();
    }

    public static BattleCombatantAffiliationProjection from(BattlePokemonSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        return new BattleCombatantAffiliationProjection(
                snapshot.pokemonId(),
                snapshot.ownerPlayerId(),
                true
        );
    }
}
