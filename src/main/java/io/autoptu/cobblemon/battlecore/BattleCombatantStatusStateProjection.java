package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalStatusEntry;

import java.util.List;

/** Ordered server-owned status metadata for one authoritative combatant. */
public record BattleCombatantStatusStateProjection(
        String combatantId,
        List<CanonicalStatusEntry> entries
) {
    public BattleCombatantStatusStateProjection {
        if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("combatantId must not be blank");
        combatantId = combatantId.strip();
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static BattleCombatantStatusStateProjection from(BattlePokemonSnapshot pokemon) {
        if (pokemon == null) throw new IllegalArgumentException("pokemon snapshot is required");
        return new BattleCombatantStatusStateProjection(pokemon.pokemonId(), pokemon.statusState().entries());
    }
}
