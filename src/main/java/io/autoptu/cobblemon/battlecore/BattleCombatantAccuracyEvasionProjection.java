package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;

/**
 * Project-owned bridge for baseline PTU accuracy/evasion inputs.
 *
 * This deliberately does not calculate final evasion. AutoPTU-Java owns stat-derived
 * evasion plus ability/item/Feature/status/terrain/temporary-effect hooks.
 */
public record BattleCombatantAccuracyEvasionProjection(
        String combatantId,
        int accuracyStage,
        int physicalEvasionBonus,
        int specialEvasionBonus,
        int statusEvasionBonus
) {
    public BattleCombatantAccuracyEvasionProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        if (accuracyStage < -6 || accuracyStage > 6) {
            throw new IllegalArgumentException("accuracyStage must be between -6 and 6");
        }
    }

    public static BattleCombatantAccuracyEvasionProjection from(BattlePokemonSnapshot pokemon) {
        if (pokemon == null) throw new IllegalArgumentException("pokemon snapshot is required");
        CanonicalAccuracyEvasion canonical = pokemon.accuracyEvasion();
        if (canonical == null) {
            throw new IllegalArgumentException("combatant lacks canonical accuracy/evasion inputs: " + pokemon.pokemonId());
        }
        return new BattleCombatantAccuracyEvasionProjection(
                pokemon.pokemonId(),
                canonical.accuracyStage(),
                canonical.physicalEvasionBonus(),
                canonical.specialEvasionBonus(),
                canonical.statusEvasionBonus());
    }
}
