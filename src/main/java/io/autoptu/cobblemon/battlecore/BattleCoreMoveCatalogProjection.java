package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves frozen combatant move IDs through a server-owned authoritative catalog. */
public record BattleCoreMoveCatalogProjection(
        String reservationId,
        Map<String, List<AuthoritativeMoveMetadata>> movesByCombatant
) {
    public BattleCoreMoveCatalogProjection {
        if (reservationId == null || reservationId.isBlank()) throw new IllegalArgumentException("reservationId is required");
        LinkedHashMap<String, List<AuthoritativeMoveMetadata>> copy = new LinkedHashMap<>();
        if (movesByCombatant == null) throw new IllegalArgumentException("movesByCombatant is required");
        for (Map.Entry<String, List<AuthoritativeMoveMetadata>> entry : movesByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("combatantId is required");
            List<AuthoritativeMoveMetadata> moves = List.copyOf(Objects.requireNonNull(entry.getValue(), "move list"));
            copy.put(combatantId.strip(), moves);
        }
        movesByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreMoveCatalogProjection resolve(
            BattleCoreBootstrapProjection bootstrap,
            AuthoritativeMoveCatalog catalog
    ) {
        Objects.requireNonNull(bootstrap, "bootstrap");
        Objects.requireNonNull(catalog, "catalog");
        if (!bootstrap.moveLoadoutsByCombatant().keySet().equals(bootstrap.combatantIds())) {
            throw new IllegalArgumentException("canonical move loadouts must cover the authoritative roster exactly");
        }

        LinkedHashMap<String, List<AuthoritativeMoveMetadata>> resolved = new LinkedHashMap<>();
        for (String combatantId : bootstrap.combatantIds()) {
            BattleCombatantMoveLoadoutProjection loadout = bootstrap.moveLoadoutsByCombatant().get(combatantId);
            if (loadout == null || !combatantId.equals(loadout.combatantId())) {
                throw new IllegalArgumentException("invalid move loadout for combatant: " + combatantId);
            }
            ArrayList<AuthoritativeMoveMetadata> moves = new ArrayList<>();
            for (String moveId : loadout.moveIds()) {
                AuthoritativeMoveMetadata metadata = catalog.findByMoveId(moveId)
                        .orElseThrow(() -> new IllegalArgumentException("authoritative move metadata missing for: " + moveId));
                if (!moveId.equals(metadata.moveId())) {
                    throw new IllegalArgumentException("catalog move id mismatch: requested " + moveId + " but resolved " + metadata.moveId());
                }
                moves.add(metadata);
            }
            resolved.put(combatantId, List.copyOf(moves));
        }
        return new BattleCoreMoveCatalogProjection(bootstrap.reservationId(), resolved);
    }
}
