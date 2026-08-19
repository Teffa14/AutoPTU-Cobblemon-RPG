package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Binds canonical type/ability identities to the same reservation and roster as geometry. */
public record BattleCoreTraitsBootstrapProjection(
        String reservationId,
        BattleCoreGeometryBootstrapProjection geometryBootstrap,
        Map<String, BattleCombatantTraitsProjection> traitsByCombatant
) {
    public BattleCoreTraitsBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        geometryBootstrap = Objects.requireNonNull(geometryBootstrap, "geometryBootstrap");
        if (!reservationId.equals(geometryBootstrap.reservationId())) {
            throw new IllegalArgumentException("geometry bootstrap belongs to a different battle reservation");
        }
        if (traitsByCombatant == null) {
            throw new IllegalArgumentException("traitsByCombatant is required");
        }

        LinkedHashMap<String, BattleCombatantTraitsProjection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantTraitsProjection> entry : traitsByCombatant.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("traits map key must not be blank");
            }
            combatantId = combatantId.strip();
            BattleCombatantTraitsProjection traits = Objects.requireNonNull(entry.getValue(), "traits projection");
            if (!combatantId.equals(traits.combatantId())) {
                throw new IllegalArgumentException("traits map key must match embedded combatantId");
            }
            if (copy.put(combatantId, traits) != null) {
                throw new IllegalArgumentException("duplicate traits projection");
            }
        }

        if (!copy.keySet().equals(geometryBootstrap.geometryByCombatant().keySet())) {
            throw new IllegalArgumentException("canonical battle traits must exactly cover the bootstrapped combatant roster");
        }
        traitsByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreTraitsBootstrapProjection from(
            BattleAuthoritySnapshot snapshot,
            BattleCoreGeometryBootstrapProjection geometryBootstrap
    ) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (geometryBootstrap == null) throw new IllegalArgumentException("geometryBootstrap is required");
        if (!snapshot.reservationId().equals(geometryBootstrap.reservationId())) {
            throw new IllegalArgumentException("snapshot and geometry bootstrap belong to different battle reservations");
        }
        LinkedHashMap<String, BattleCombatantTraitsProjection> traits = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            BattleCombatantTraitsProjection projection = BattleCombatantTraitsProjection.from(pokemon);
            if (traits.put(projection.combatantId(), projection) != null) {
                throw new IllegalArgumentException("duplicate combatant in canonical battle traits: " + projection.combatantId());
            }
        }
        return new BattleCoreTraitsBootstrapProjection(snapshot.reservationId(), geometryBootstrap, traits);
    }
}
