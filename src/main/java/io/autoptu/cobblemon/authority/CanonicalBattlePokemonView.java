package io.autoptu.cobblemon.authority;

import java.util.Set;

/**
 * Owner-neutral canonical Pokémon state required to freeze one battle combatant.
 *
 * Player ownership remains a persistence/authorization concern of CanonicalPokemonState.
 * Encounter opponents can implement this contract without inventing a fake player owner.
 */
public interface CanonicalBattlePokemonView {
    String pokemonId();

    String speciesId();

    int level();

    Set<String> capabilities();

    Set<String> statuses();

    CanonicalStatusState statusState();

    CanonicalCombatStats combatStats();

    CanonicalHealth health();

    CanonicalMoveLoadout moveLoadout();

    CanonicalBaseMovement baseMovement();

    CanonicalBattleTraits battleTraits();

    CanonicalAccuracyEvasion accuracyEvasion();

    CanonicalInjuryState injuryState();

    String heldItemInstanceId();

    long revision();
}
