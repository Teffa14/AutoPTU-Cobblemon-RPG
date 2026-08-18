package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;

/**
 * Project-owned immutable base-stat projection for one reserved combatant.
 *
 * The projection contains only trusted PTU base stats frozen in the battle
 * snapshot. Battle stages, temporary modifiers, status penalties, held-item
 * effects, abilities, and Trainer Feature effects remain battle-runtime state
 * and must not be supplied by Minecraft/Cobblemon adapters.
 */
public record BattleCombatantStatProjection(
        String combatantId,
        int atk,
        int def,
        int spatk,
        int spdef,
        int spd
) {
    public BattleCombatantStatProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        requirePositive("atk", atk);
        requirePositive("def", def);
        requirePositive("spatk", spatk);
        requirePositive("spdef", spdef);
        requirePositive("spd", spd);
    }

    public static BattleCombatantStatProjection from(BattlePokemonSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }

        CanonicalCombatStats stats = snapshot.combatStats();
        if (stats == null) {
            throw new IllegalArgumentException(
                    "canonical combat stats are required for combatant: " + snapshot.pokemonId()
            );
        }

        return new BattleCombatantStatProjection(
                snapshot.pokemonId(),
                stats.atk(),
                stats.def(),
                stats.spatk(),
                stats.spdef(),
                stats.spd()
        );
    }

    private static void requirePositive(String name, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be >= 1");
        }
    }
}
