package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;

import java.util.List;

/**
 * Immutable server-owned combatant type and ability identities.
 *
 * This record carries canonical identities only. AutoPTU-Java owns type calculations
 * and every ability hook/effect; Minecraft/Cobblemon cannot grant or interpret either.
 */
public record BattleCombatantTraitsProjection(
        String combatantId,
        List<String> types,
        List<String> abilities
) {
    public BattleCombatantTraitsProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        types = types == null ? List.of() : List.copyOf(types);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        if (types.isEmpty()) {
            throw new IllegalArgumentException("canonical combatant types are required");
        }
    }

    public static BattleCombatantTraitsProjection from(BattlePokemonSnapshot pokemon) {
        if (pokemon == null) throw new IllegalArgumentException("pokemon is required");
        CanonicalBattleTraits traits = pokemon.battleTraits();
        if (traits == null) {
            throw new IllegalArgumentException("combatant lacks canonical battle traits: " + pokemon.pokemonId());
        }
        return new BattleCombatantTraitsProjection(pokemon.pokemonId(), traits.types(), traits.abilities());
    }
}
