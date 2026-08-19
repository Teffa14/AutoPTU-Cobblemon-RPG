package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Adapter-neutral world-space relocation derived from an authoritative core relocation event.
 *
 * This record contains presentation coordinates only. It never decides whether movement is legal,
 * whether a path is traversable, or whether displacement/forced-movement rules apply.
 */
public record BattleWorldRelocation(
        long sequence,
        int ordinal,
        String combatantId,
        WorldBlockCoordinate origin,
        WorldBlockCoordinate destination
) {
    public BattleWorldRelocation {
        if (sequence < 0) throw new IllegalArgumentException("sequence cannot be negative");
        if (ordinal < 0) throw new IllegalArgumentException("ordinal cannot be negative");
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId is required");
        }
        combatantId = combatantId.strip();
        origin = Objects.requireNonNull(origin, "origin");
        destination = Objects.requireNonNull(destination, "destination");
        if (!origin.dimensionId().equals(destination.dimensionId())) {
            throw new IllegalArgumentException("relocation endpoints must share one dimension");
        }
    }
}
