package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * World-space relocation bound to the exact presentation entity registered for the combatant.
 * Movement legality and all PTU displacement rules have already been decided upstream.
 */
public record EntityBoundBattleWorldRelocation(
        long sequence,
        int ordinal,
        String combatantId,
        String presentationEntityId,
        WorldBlockCoordinate origin,
        WorldBlockCoordinate destination
) implements EntityBoundPresentationOutput {
    public EntityBoundBattleWorldRelocation {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal cannot be negative");
        if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("combatantId is required");
        if (presentationEntityId == null || presentationEntityId.isBlank()) throw new IllegalArgumentException("presentationEntityId is required");
        combatantId = combatantId.strip();
        presentationEntityId = presentationEntityId.strip();
        origin = Objects.requireNonNull(origin, "origin");
        destination = Objects.requireNonNull(destination, "destination");
        if (!origin.dimensionId().equals(destination.dimensionId())) {
            throw new IllegalArgumentException("relocation endpoints must share one dimension");
        }
    }
}
