package io.autoptu.cobblemon.battlecore;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One immutable action-space snapshot projected from AutoPTU-Java for a single actor.
 * The reservation/actor scope prevents stale or cross-battle legal choices from being
 * reused for a client request.
 */
public record BattleCoreLegalChoiceSet(
        String reservationId,
        String actorId,
        List<BattleCoreLegalChoice> choices
) {
    public BattleCoreLegalChoiceSet {
        reservationId = normalize(reservationId, "reservationId");
        actorId = normalize(actorId, "actorId");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        Set<String> stableKeys = new HashSet<>();
        for (BattleCoreLegalChoice choice : choices) {
            Objects.requireNonNull(choice, "choice");
            if (!actorId.equals(choice.actorId())) {
                throw new IllegalArgumentException("legal choice actor differs from action-space actor");
            }
            if (!stableKeys.add(choice.stableKey())) {
                throw new IllegalArgumentException("duplicate legal choice stableKey");
            }
        }
    }

    private static String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
