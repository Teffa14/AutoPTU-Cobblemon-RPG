package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattleItemSnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation-scoped canonical held-item state for the AutoPTU-Java runtime boundary.
 *
 * Held-item identity is resolved exclusively from the frozen battle reservation.
 * Minecraft/Cobblemon packets, entity equipment, display names, or client inventory
 * claims cannot introduce or replace an item here. Item effects remain core-owned.
 */
public record BattleCoreHeldItemBootstrapProjection(
        String reservationId,
        Map<String, BattleCombatantHeldItemProjection> heldItemsByCombatant
) {
    public BattleCoreHeldItemBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        LinkedHashMap<String, BattleCombatantHeldItemProjection> copy = new LinkedHashMap<>();
        if (heldItemsByCombatant != null) {
            for (Map.Entry<String, BattleCombatantHeldItemProjection> entry : heldItemsByCombatant.entrySet()) {
                String combatantId = entry.getKey();
                if (combatantId == null || combatantId.isBlank()) {
                    throw new IllegalArgumentException("held-item map key must not be blank");
                }
                combatantId = combatantId.strip();
                BattleCombatantHeldItemProjection value = Objects.requireNonNull(entry.getValue(), "held-item projection");
                if (!combatantId.equals(value.combatantId())) {
                    throw new IllegalArgumentException("held-item map key must match embedded combatantId");
                }
                if (copy.put(combatantId, value) != null) {
                    throw new IllegalArgumentException("duplicate held-item combatant");
                }
            }
        }
        heldItemsByCombatant = Map.copyOf(copy);
    }

    public static BattleCoreHeldItemBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        Map<String, BattleItemSnapshot> itemsById = new HashMap<>();
        for (BattleItemSnapshot item : snapshot.items()) {
            BattleItemSnapshot previous = itemsById.put(item.itemInstanceId(), item);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate item instance in battle snapshot");
            }
        }

        LinkedHashMap<String, BattleCombatantHeldItemProjection> heldItems = new LinkedHashMap<>();
        Set<String> assignedItemIds = new HashSet<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            String itemInstanceId = pokemon.heldItemInstanceId();
            if (itemInstanceId == null) {
                continue;
            }
            BattleItemSnapshot item = itemsById.get(itemInstanceId);
            if (item == null) {
                throw new IllegalArgumentException("held item must belong to the authoritative battle reservation");
            }
            if (!item.heldItem()) {
                throw new IllegalArgumentException("referenced item must be reserved as a held item");
            }
            if (!pokemon.ownerPlayerId().equals(item.ownerPlayerId())) {
                throw new IllegalArgumentException("held item and combatant must share canonical owner");
            }
            if (!assignedItemIds.add(itemInstanceId)) {
                throw new IllegalArgumentException("one held-item instance cannot be assigned to multiple combatants");
            }
            heldItems.put(pokemon.pokemonId(), new BattleCombatantHeldItemProjection(
                    pokemon.pokemonId(),
                    item.itemInstanceId(),
                    item.templateId()
            ));
        }

        return new BattleCoreHeldItemBootstrapProjection(snapshot.reservationId(), heldItems);
    }
}
