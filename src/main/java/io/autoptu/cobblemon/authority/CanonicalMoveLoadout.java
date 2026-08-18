package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Immutable server-owned move identity set for one Pokémon.
 *
 * Move IDs are frozen from canonical PTU state before battle. Minecraft/Cobblemon
 * clients may request one of these IDs, but they cannot add moves to this loadout.
 * An explicit empty loadout remains distinct from legacy unspecified move state.
 */
public record CanonicalMoveLoadout(List<String> moveIds) {
    public CanonicalMoveLoadout {
        if (moveIds == null || moveIds.isEmpty()) {
            moveIds = List.of();
        } else {
            ArrayList<String> copied = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String moveId : moveIds) {
                if (moveId == null || moveId.isBlank()) {
                    throw new IllegalArgumentException("moveId must not be blank");
                }
                String normalized = moveId.strip();
                if (!seen.add(normalized)) {
                    throw new IllegalArgumentException("duplicate moveId: " + normalized);
                }
                copied.add(normalized);
            }
            moveIds = List.copyOf(copied);
        }
    }

    public boolean contains(String moveId) {
        if (moveId == null || moveId.isBlank()) {
            return false;
        }
        return moveIds.contains(moveId.strip());
    }
}
