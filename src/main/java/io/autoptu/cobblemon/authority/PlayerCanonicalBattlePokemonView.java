package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Set;

/**
 * Explicit adapter from player-owned canonical persistence state to the owner-neutral battle view.
 * Ownership remains available on the wrapped state for authorization, but is not part of the
 * combatant snapshot contract.
 */
public final class PlayerCanonicalBattlePokemonView implements CanonicalBattlePokemonView {
    private final CanonicalPokemonState state;

    public PlayerCanonicalBattlePokemonView(CanonicalPokemonState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public CanonicalPokemonState canonicalState() {
        return state;
    }

    @Override public String pokemonId() { return state.pokemonId(); }
    @Override public String speciesId() { return state.speciesId(); }
    @Override public int level() { return state.level(); }
    @Override public Set<String> capabilities() { return state.capabilities(); }
    @Override public Set<String> statuses() { return state.statuses(); }
    @Override public CanonicalStatusState statusState() { return state.statusState(); }
    @Override public CanonicalCombatStats combatStats() { return state.combatStats(); }
    @Override public CanonicalHealth health() { return state.health(); }
    @Override public CanonicalMoveLoadout moveLoadout() { return state.moveLoadout(); }
    @Override public CanonicalBaseMovement baseMovement() { return state.baseMovement(); }
    @Override public CanonicalBattleTraits battleTraits() { return state.battleTraits(); }
    @Override public CanonicalAccuracyEvasion accuracyEvasion() { return state.accuracyEvasion(); }
    @Override public CanonicalInjuryState injuryState() { return state.injuryState(); }
    @Override public String heldItemInstanceId() { return state.heldItemInstanceId(); }
    @Override public long revision() { return state.revision(); }
}
