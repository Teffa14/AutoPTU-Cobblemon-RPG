package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Binds ordered canonical status metadata to the same reservation and roster as the base bootstrap. */
public record BattleCoreStatusStateBootstrapProjection(
        String reservationId,
        BattleCoreBootstrapProjection bootstrap,
        Map<String, BattleCombatantStatusStateProjection> statusStateByCombatant
) {
    public BattleCoreStatusStateBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) throw new IllegalArgumentException("reservationId must not be blank");
        reservationId = reservationId.strip();
        bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        if (!reservationId.equals(bootstrap.reservationId())) {
            throw new IllegalArgumentException("bootstrap belongs to a different battle reservation");
        }
        if (statusStateByCombatant == null) throw new IllegalArgumentException("statusStateByCombatant is required");

        LinkedHashMap<String, BattleCombatantStatusStateProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantStatusStateProjection> entry : statusStateByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("status metadata map key must not be blank");
            combatantId = combatantId.strip();
            BattleCombatantStatusStateProjection projection = Objects.requireNonNull(entry.getValue(), "status projection");
            if (!combatantId.equals(projection.combatantId())) {
                throw new IllegalArgumentException("status metadata map key must match embedded combatantId");
            }
            if (copy.put(combatantId, projection) != null) throw new IllegalArgumentException("duplicate status metadata projection");
        }
        if (!copy.keySet().equals(bootstrap.combatantIds())) {
            throw new IllegalArgumentException("canonical status metadata must exactly cover the bootstrapped combatant roster");
        }
        for (Map.Entry<String, BattleCombatantStatusStateProjection> entry : copy.entrySet()) {
            Set<String> projectedNames = new LinkedHashSet<>();
            entry.getValue().entries().forEach(status -> projectedNames.add(status.name()));
            Set<String> legacyNames = bootstrap.statusesByCombatant().getOrDefault(entry.getKey(), Set.of());
            if (!projectedNames.equals(legacyNames)) {
                throw new IllegalArgumentException("status metadata names must match legacy canonical status view for combatant: " + entry.getKey());
            }
        }
        statusStateByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreStatusStateBootstrapProjection from(
            BattleAuthoritySnapshot snapshot,
            BattleCoreBootstrapProjection bootstrap
    ) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (bootstrap == null) throw new IllegalArgumentException("bootstrap is required");
        if (!snapshot.reservationId().equals(bootstrap.reservationId())) {
            throw new IllegalArgumentException("snapshot and bootstrap belong to different battle reservations");
        }
        LinkedHashMap<String, BattleCombatantStatusStateProjection> statuses = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantStatusStateProjection projection = BattleCombatantStatusStateProjection.from(pokemon);
            if (statuses.put(projection.combatantId(), projection) != null) {
                throw new IllegalArgumentException("duplicate combatant in canonical status metadata: " + projection.combatantId());
            }
        }
        return new BattleCoreStatusStateBootstrapProjection(snapshot.reservationId(), bootstrap, statuses);
    }
}
