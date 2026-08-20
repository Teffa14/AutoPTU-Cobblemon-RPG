package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;

/**
 * Reservation-scoped handoff for one already-resolved combat-stage mutation and the
 * ordered semantic events emitted by AutoPTU-Java reaction hooks.
 *
 * The bundle never applies a PTU stage delta, searches for ability holders, selects a
 * reaction source, suppresses recursive hooks, or derives event semantics. Those rules
 * remain authoritative in AutoPTU-Java. Minecraft/Cobblemon may update presentation
 * state from the mutation projection and render the event batch in the supplied order.
 */
public record BattleCombatStageMutationPlaybackBundle(
        String reservationId,
        BattleCombatStageMutationProjection mutation,
        BattlePlaybackBatch reactionPlayback
) {
    public BattleCombatStageMutationPlaybackBundle {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        mutation = Objects.requireNonNull(mutation, "mutation");
        reactionPlayback = Objects.requireNonNull(reactionPlayback, "reactionPlayback");
        if (!reservationId.equals(reactionPlayback.reservationId())) {
            throw new IllegalArgumentException("reaction playback must belong to the same battle reservation");
        }
    }

    public List<BattleEventPlaybackEnvelope> events() {
        return reactionPlayback.events();
    }

    public boolean hasReactionPlayback() {
        return !reactionPlayback.events().isEmpty();
    }
}
