package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Persists final Pokemon state that has already been authored by the authoritative battle engine.
 *
 * <p>This service deliberately performs no PTU calculations. The trusted server caller supplies
 * the final health, status metadata, and injury state. The service only verifies canonical
 * identity/revision ownership and advances the durable Pokemon aggregate exactly once.</p>
 */
public final class AuthoritativePostBattlePokemonCommitService {
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public AuthoritativePostBattlePokemonCommitService(VersionedCanonicalPokemonRepository pokemonRepository) {
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public AuthoritativePostBattlePokemonCommitDecision commit(
            String playerId,
            String pokemonId,
            long expectedRevision,
            CanonicalHealth finalHealth,
            CanonicalStatusState finalStatusState,
            CanonicalInjuryState finalInjuryState
    ) {
        requireId(playerId, "playerId");
        requireId(pokemonId, "pokemonId");
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow exactly one revision advance");
        }
        Objects.requireNonNull(finalHealth, "finalHealth");
        Objects.requireNonNull(finalStatusState, "finalStatusState");
        Objects.requireNonNull(finalInjuryState, "finalInjuryState");

        CanonicalPokemonState current = pokemonRepository.findPokemon(pokemonId).orElse(null);
        if (current == null) {
            return AuthoritativePostBattlePokemonCommitDecision.rejected("pokemon_not_found");
        }
        if (!current.ownerPlayerId().equals(playerId)) {
            return AuthoritativePostBattlePokemonCommitDecision.rejected("pokemon_not_owned");
        }

        if (current.revision() == expectedRevision + 1) {
            if (matchesEngineFinalState(current, finalHealth, finalStatusState, finalInjuryState)) {
                return AuthoritativePostBattlePokemonCommitDecision.replay(current);
            }
            return AuthoritativePostBattlePokemonCommitDecision.rejected("post_battle_state_already_changed");
        }
        if (current.revision() != expectedRevision) {
            return AuthoritativePostBattlePokemonCommitDecision.rejected("pokemon_revision_changed");
        }

        CanonicalPokemonState replacement = new CanonicalPokemonState(
                current.pokemonId(),
                current.ownerPlayerId(),
                current.speciesId(),
                current.level(),
                current.capabilities(),
                finalStatusState.names(),
                finalStatusState,
                current.combatStats(),
                finalHealth,
                current.moveLoadout(),
                current.baseMovement(),
                current.battleTraits(),
                current.accuracyEvasion(),
                finalInjuryState,
                current.heldItemInstanceId(),
                expectedRevision + 1);

        if (!pokemonRepository.replacePokemonIfRevision(pokemonId, expectedRevision, replacement)) {
            CanonicalPokemonState afterRace = pokemonRepository.findPokemon(pokemonId).orElse(null);
            if (afterRace != null
                    && afterRace.ownerPlayerId().equals(playerId)
                    && afterRace.revision() == expectedRevision + 1
                    && matchesEngineFinalState(afterRace, finalHealth, finalStatusState, finalInjuryState)) {
                return AuthoritativePostBattlePokemonCommitDecision.replay(afterRace);
            }
            return AuthoritativePostBattlePokemonCommitDecision.rejected("pokemon_revision_changed");
        }
        return AuthoritativePostBattlePokemonCommitDecision.committed(replacement);
    }

    private static boolean matchesEngineFinalState(
            CanonicalPokemonState state,
            CanonicalHealth health,
            CanonicalStatusState statusState,
            CanonicalInjuryState injuryState
    ) {
        return Objects.equals(state.health(), health)
                && Objects.equals(state.statusState(), statusState)
                && Objects.equals(state.injuryState(), injuryState);
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
