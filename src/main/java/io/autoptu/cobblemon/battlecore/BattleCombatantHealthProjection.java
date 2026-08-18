package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalHealth;

/** Project-owned authoritative HP projection for the Java battle-core bootstrap. */
public record BattleCombatantHealthProjection(String combatantId, int currentHp, int maxHp) {
    public BattleCombatantHealthProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }
        if (currentHp < 0 || currentHp > maxHp) {
            throw new IllegalArgumentException("currentHp must be between 0 and maxHp");
        }
    }

    public static BattleCombatantHealthProjection from(BattlePokemonSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        CanonicalHealth health = snapshot.health();
        if (health == null) {
            throw new IllegalArgumentException("canonical health is required for combatant: " + snapshot.pokemonId());
        }
        return new BattleCombatantHealthProjection(snapshot.pokemonId(), health.currentHp(), health.maxHp());
    }
}
