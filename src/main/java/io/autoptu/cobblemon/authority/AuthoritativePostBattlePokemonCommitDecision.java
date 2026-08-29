package io.autoptu.cobblemon.authority;

/**
 * Result of attempting to persist one engine-authored post-battle Pokemon state.
 */
public record AuthoritativePostBattlePokemonCommitDecision(
        boolean accepted,
        boolean idempotent,
        String reason,
        CanonicalPokemonState state
) {
    public AuthoritativePostBattlePokemonCommitDecision {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (accepted && state == null) {
            throw new IllegalArgumentException("accepted decision requires state");
        }
        if (!accepted && idempotent) {
            throw new IllegalArgumentException("rejected decision cannot be idempotent");
        }
    }

    public static AuthoritativePostBattlePokemonCommitDecision committed(CanonicalPokemonState state) {
        return new AuthoritativePostBattlePokemonCommitDecision(true, false, "committed", state);
    }

    public static AuthoritativePostBattlePokemonCommitDecision replay(CanonicalPokemonState state) {
        return new AuthoritativePostBattlePokemonCommitDecision(true, true, "already_committed", state);
    }

    public static AuthoritativePostBattlePokemonCommitDecision rejected(String reason) {
        return new AuthoritativePostBattlePokemonCommitDecision(false, false, reason, null);
    }
}
