package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;

/** Project-owned projection of persistent PTU injury count. */
public record BattleCombatantInjuryProjection(String combatantId, int injuries) {
    public BattleCombatantInjuryProjection {
        if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("combatantId must not be blank");
        combatantId = combatantId.strip();
        if (injuries < 0) throw new IllegalArgumentException("injuries must be >= 0");
    }

    public static BattleCombatantInjuryProjection from(BattlePokemonSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        CanonicalInjuryState injuryState = snapshot.injuryState();
        if (injuryState == null) {
            throw new IllegalArgumentException("canonical injury state is required for combatant: " + snapshot.pokemonId());
        }
        return new BattleCombatantInjuryProjection(snapshot.pokemonId(), injuryState.injuries());
    }
}
