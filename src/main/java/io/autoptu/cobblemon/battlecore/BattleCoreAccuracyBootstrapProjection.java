package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Binds canonical baseline accuracy/evasion inputs to the exact battle reservation and roster. */
public record BattleCoreAccuracyBootstrapProjection(
        String reservationId,
        BattleCoreTraitsBootstrapProjection traitsBootstrap,
        Map<String, BattleCombatantAccuracyEvasionProjection> accuracyEvasionByCombatant
) {
    public BattleCoreAccuracyBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        traitsBootstrap = Objects.requireNonNull(traitsBootstrap, "traitsBootstrap");
        if (!reservationId.equals(traitsBootstrap.reservationId())) {
            throw new IllegalArgumentException("traits bootstrap belongs to a different battle reservation");
        }
        if (accuracyEvasionByCombatant == null) {
            throw new IllegalArgumentException("accuracyEvasionByCombatant is required");
        }

        LinkedHashMap<String, BattleCombatantAccuracyEvasionProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantAccuracyEvasionProjection> entry : accuracyEvasionByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("accuracy/evasion map key must not be blank");
            }
            combatantId = combatantId.strip();
            BattleCombatantAccuracyEvasionProjection projection = Objects.requireNonNull(entry.getValue(), "accuracy/evasion projection");
            if (!combatantId.equals(projection.combatantId())) {
                throw new IllegalArgumentException("accuracy/evasion map key must match embedded combatantId");
            }
            if (copy.put(combatantId, projection) != null) {
                throw new IllegalArgumentException("duplicate accuracy/evasion projection");
            }
        }
        if (!copy.keySet().equals(traitsBootstrap.traitsByCombatant().keySet())) {
            throw new IllegalArgumentException("canonical accuracy/evasion inputs must exactly cover the bootstrapped combatant roster");
        }
        accuracyEvasionByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreAccuracyBootstrapProjection from(
            BattleAuthoritySnapshot snapshot,
            BattleCoreTraitsBootstrapProjection traitsBootstrap
    ) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (traitsBootstrap == null) throw new IllegalArgumentException("traitsBootstrap is required");
        if (!snapshot.reservationId().equals(traitsBootstrap.reservationId())) {
            throw new IllegalArgumentException("snapshot and traits bootstrap belong to different battle reservations");
        }
        LinkedHashMap<String, BattleCombatantAccuracyEvasionProjection> values = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantAccuracyEvasionProjection projection = BattleCombatantAccuracyEvasionProjection.from(pokemon);
            if (values.put(projection.combatantId(), projection) != null) {
                throw new IllegalArgumentException("duplicate combatant in canonical accuracy/evasion inputs: " + projection.combatantId());
            }
        }
        return new BattleCoreAccuracyBootstrapProjection(snapshot.reservationId(), traitsBootstrap, values);
    }
}
