package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Binds persistent PTU injury counts to the exact authoritative battle reservation and roster. */
public record BattleCoreInjuryBootstrapProjection(
        String reservationId,
        BattleCoreBootstrapProjection bootstrap,
        Map<String, BattleCombatantInjuryProjection> injuriesByCombatant
) {
    public BattleCoreInjuryBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) throw new IllegalArgumentException("reservationId must not be blank");
        reservationId = reservationId.strip();
        bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        if (!reservationId.equals(bootstrap.reservationId())) {
            throw new IllegalArgumentException("bootstrap belongs to a different battle reservation");
        }
        if (injuriesByCombatant == null) throw new IllegalArgumentException("injuriesByCombatant is required");

        LinkedHashMap<String, BattleCombatantInjuryProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantInjuryProjection> entry : injuriesByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) throw new IllegalArgumentException("injury map key must not be blank");
            combatantId = combatantId.strip();
            BattleCombatantInjuryProjection projection = Objects.requireNonNull(entry.getValue(), "injury projection");
            if (!combatantId.equals(projection.combatantId())) {
                throw new IllegalArgumentException("injury map key must match embedded combatantId");
            }
            if (copy.put(combatantId, projection) != null) throw new IllegalArgumentException("duplicate injury projection");
        }
        if (!copy.keySet().equals(bootstrap.combatantIds())) {
            throw new IllegalArgumentException("canonical injuries must exactly cover the bootstrapped combatant roster");
        }
        injuriesByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreInjuryBootstrapProjection from(
            BattleAuthoritySnapshot snapshot,
            BattleCoreBootstrapProjection bootstrap
    ) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (bootstrap == null) throw new IllegalArgumentException("bootstrap is required");
        if (!snapshot.reservationId().equals(bootstrap.reservationId())) {
            throw new IllegalArgumentException("snapshot and bootstrap belong to different battle reservations");
        }
        LinkedHashMap<String, BattleCombatantInjuryProjection> injuries = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantInjuryProjection projection = BattleCombatantInjuryProjection.from(pokemon);
            if (injuries.put(projection.combatantId(), projection) != null) {
                throw new IllegalArgumentException("duplicate combatant in canonical injuries: " + projection.combatantId());
            }
        }
        return new BattleCoreInjuryBootstrapProjection(snapshot.reservationId(), bootstrap, injuries);
    }
}
