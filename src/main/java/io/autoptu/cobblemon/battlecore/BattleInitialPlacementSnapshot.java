package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable initial board placement bound to one authoritative battle reservation.
 *
 * The integration layer validates reservation/roster identity and freezes anchors. It
 * intentionally does not validate footprint overlap, collision, terrain, facing,
 * movement legality, forced movement, targeting, or other PTU semantics.
 */
public record BattleInitialPlacementSnapshot(
        String reservationId,
        Map<String, BattleCombatantInitialPlacement> placementsByCombatant
) {
    public BattleInitialPlacementSnapshot {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        if (placementsByCombatant == null || placementsByCombatant.isEmpty()) {
            throw new IllegalArgumentException("placementsByCombatant must not be empty");
        }
        LinkedHashMap<String, BattleCombatantInitialPlacement> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BattleCombatantInitialPlacement> entry : placementsByCombatant.entrySet()) {
            String key = entry.getKey();
            BattleCombatantInitialPlacement placement = Objects.requireNonNull(entry.getValue(), "placement");
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("placement map key must not be blank");
            }
            key = key.strip();
            if (!key.equals(placement.combatantId())) {
                throw new IllegalArgumentException("placement map key must match embedded combatantId");
            }
            if (copy.put(key, placement) != null) {
                throw new IllegalArgumentException("duplicate combatant placement");
            }
        }
        placementsByCombatant = Map.copyOf(copy);
    }

    public static BattleInitialPlacementSnapshot from(
            BattleAuthoritySnapshot battle,
            Map<String, BattleGridCoordinate> anchorsByCombatant
    ) {
        Objects.requireNonNull(battle, "battle");
        if (battle.arena() == null) {
            throw new IllegalArgumentException("battle reservation must freeze an arena before initial placement");
        }
        if (anchorsByCombatant == null) {
            throw new IllegalArgumentException("anchorsByCombatant is required");
        }

        Set<String> rosterIds = battle.roster().stream()
                .map(BattlePokemonSnapshot::pokemonId)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> suppliedIds = anchorsByCombatant.keySet().stream()
                .map(id -> {
                    if (id == null || id.isBlank()) {
                        throw new IllegalArgumentException("combatant placement key must not be blank");
                    }
                    return id.strip();
                })
                .collect(Collectors.toUnmodifiableSet());
        if (!suppliedIds.equals(rosterIds)) {
            throw new IllegalArgumentException("initial placements must exactly cover the authoritative roster");
        }

        LinkedHashMap<String, BattleCombatantInitialPlacement> placements = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : battle.roster()) {
            BattleGridCoordinate anchor = anchorsByCombatant.get(pokemon.pokemonId());
            if (anchor == null) {
                throw new IllegalArgumentException("missing initial placement for combatant " + pokemon.pokemonId());
            }
            placements.put(
                    pokemon.pokemonId(),
                    new BattleCombatantInitialPlacement(pokemon.pokemonId(), anchor));
        }
        return new BattleInitialPlacementSnapshot(battle.reservationId(), placements);
    }

    public WorldBlockCoordinate worldAnchor(BattleAuthoritySnapshot battle, String combatantId) {
        Objects.requireNonNull(battle, "battle");
        if (!reservationId.equals(battle.reservationId())) {
            throw new IllegalArgumentException("placement snapshot belongs to a different battle reservation");
        }
        if (battle.arena() == null) {
            throw new IllegalArgumentException("battle reservation has no frozen arena");
        }
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        BattleCombatantInitialPlacement placement = placementsByCombatant.get(combatantId.strip());
        if (placement == null) {
            throw new IllegalArgumentException("combatant is not present in the initial placement snapshot");
        }
        return BattleGridTransform.from(battle.arena()).toWorld(placement.anchor());
    }
}
