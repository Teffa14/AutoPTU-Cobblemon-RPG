package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Final persistent Pokemon fields already resolved by the authoritative battle engine.
 *
 * <p>No PTU rule is evaluated by this value object. The expected revision is the revision frozen
 * into the server-owned battle reservation before the engine started.</p>
 */
public record AuthoritativePostBattlePokemonFinalState(
        String pokemonId,
        long expectedRevision,
        CanonicalHealth health,
        CanonicalStatusState statusState,
        CanonicalInjuryState injuryState
) {
    public AuthoritativePostBattlePokemonFinalState {
        if (pokemonId == null || pokemonId.isBlank()) {
            throw new IllegalArgumentException("pokemonId must not be blank");
        }
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow one revision advance");
        }
        health = Objects.requireNonNull(health, "health");
        statusState = Objects.requireNonNull(statusState, "statusState");
        injuryState = Objects.requireNonNull(injuryState, "injuryState");
    }
}
