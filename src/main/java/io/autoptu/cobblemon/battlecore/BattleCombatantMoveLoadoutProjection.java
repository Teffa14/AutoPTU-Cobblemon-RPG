package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;

import java.util.List;

/**
 * Immutable project-owned projection of one combatant's server-authoritative move IDs.
 *
 * The integration layer freezes move identity before battle. AutoPTU-Java remains
 * responsible for resolving authoritative move metadata such as targeting,
 * accuracy, damage profile, type, and frequency. Minecraft/Cobblemon clients may
 * request a move ID, but cannot add or replace entries in this projection.
 */
public record BattleCombatantMoveLoadoutProjection(
        String combatantId,
        List<String> moveIds
) {
    public BattleCombatantMoveLoadoutProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        moveIds = moveIds == null ? List.of() : List.copyOf(moveIds);
    }

    public static BattleCombatantMoveLoadoutProjection from(BattlePokemonSnapshot pokemon) {
        if (pokemon == null) {
            throw new IllegalArgumentException("pokemon snapshot is required");
        }
        CanonicalMoveLoadout loadout = pokemon.moveLoadout();
        if (loadout == null) {
            throw new IllegalArgumentException(
                    "canonical move loadout is required for combatant: " + pokemon.pokemonId());
        }
        return new BattleCombatantMoveLoadoutProjection(pokemon.pokemonId(), loadout.moveIds());
    }
}
