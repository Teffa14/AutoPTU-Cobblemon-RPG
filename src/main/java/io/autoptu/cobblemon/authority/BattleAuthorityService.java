package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class BattleAuthorityService {
    private final CanonicalStateRepository stateRepository;
    private final CanonicalAssetRepository assetRepository;
    private final BattleSnapshotRepository snapshotRepository;
    private final Supplier<String> reservationIds;
    private final LongSupplier rngSeeds;

    public BattleAuthorityService(
            CanonicalStateRepository stateRepository,
            CanonicalAssetRepository assetRepository,
            BattleSnapshotRepository snapshotRepository,
            Supplier<String> reservationIds,
            LongSupplier rngSeeds
    ) {
        if (stateRepository == null || assetRepository == null || snapshotRepository == null) {
            throw new IllegalArgumentException("repositories must not be null");
        }
        if (reservationIds == null || rngSeeds == null) {
            throw new IllegalArgumentException("server suppliers must not be null");
        }
        this.stateRepository = stateRepository;
        this.assetRepository = assetRepository;
        this.snapshotRepository = snapshotRepository;
        this.reservationIds = reservationIds;
        this.rngSeeds = rngSeeds;
    }

    public BattleSnapshotDecision reserveBattle(
            String playerId,
            List<String> pokemonIds,
            Map<String, Integer> consumableQuantities
    ) {
        return reserveBattleInternal(playerId, pokemonIds, consumableQuantities, null);
    }

    public BattleSnapshotDecision reserveBattleInArena(
            String playerId,
            List<String> pokemonIds,
            Map<String, Integer> consumableQuantities,
            BattleArenaSnapshot arena
    ) {
        if (arena == null) {
            return BattleSnapshotDecision.deny("invalid_battle_arena");
        }
        return reserveBattleInternal(playerId, pokemonIds, consumableQuantities, arena);
    }

    private BattleSnapshotDecision reserveBattleInternal(
            String playerId,
            List<String> pokemonIds,
            Map<String, Integer> consumableQuantities,
            BattleArenaSnapshot arena
    ) {
        if (playerId == null || playerId.isBlank() || pokemonIds == null || pokemonIds.isEmpty()) {
            return BattleSnapshotDecision.deny("invalid_request");
        }

        Set<String> requestedPokemon = new HashSet<>();
        for (String pokemonId : pokemonIds) {
            if (pokemonId == null || pokemonId.isBlank() || !requestedPokemon.add(pokemonId)) {
                return BattleSnapshotDecision.deny("invalid_roster");
            }
        }

        CanonicalPlayerState player = stateRepository.findPlayer(playerId).orElse(null);
        if (player == null) {
            return BattleSnapshotDecision.deny("unknown_player");
        }

        List<BattlePokemonSnapshot> roster = new ArrayList<>();
        LinkedHashMap<String, BattleItemSnapshot> items = new LinkedHashMap<>();
        for (String pokemonId : pokemonIds) {
            CanonicalPokemonState pokemon = assetRepository.findPokemon(pokemonId).orElse(null);
            if (pokemon == null) {
                return BattleSnapshotDecision.deny("unknown_pokemon:" + pokemonId);
            }
            if (!pokemon.ownerPlayerId().equals(playerId)) {
                return BattleSnapshotDecision.deny("pokemon_not_owned:" + pokemonId);
            }
            roster.add(BattlePokemonSnapshot.from(pokemon));

            if (pokemon.heldItemInstanceId() != null) {
                CanonicalItemInstance held = assetRepository.findItem(pokemon.heldItemInstanceId()).orElse(null);
                if (held == null) {
                    return BattleSnapshotDecision.deny("unknown_held_item:" + pokemon.heldItemInstanceId());
                }
                if (!held.ownerPlayerId().equals(playerId)) {
                    return BattleSnapshotDecision.deny("held_item_not_owned:" + held.itemInstanceId());
                }
                if (held.quantity() < 1) {
                    return BattleSnapshotDecision.deny("held_item_unavailable:" + held.itemInstanceId());
                }
                BattleItemSnapshot previous = items.putIfAbsent(
                        held.itemInstanceId(),
                        new BattleItemSnapshot(
                                held.itemInstanceId(), held.ownerPlayerId(), held.templateId(), 1, held.revision(), true));
                if (previous != null) {
                    return BattleSnapshotDecision.deny("duplicate_held_item:" + held.itemInstanceId());
                }
            }
        }

        Map<String, Integer> requestedConsumables = consumableQuantities == null
                ? Map.of()
                : Map.copyOf(consumableQuantities);
        for (Map.Entry<String, Integer> entry : requestedConsumables.entrySet()) {
            String itemId = entry.getKey();
            Integer quantity = entry.getValue();
            if (itemId == null || itemId.isBlank() || quantity == null || quantity <= 0) {
                return BattleSnapshotDecision.deny("invalid_consumable_request");
            }
            if (items.containsKey(itemId)) {
                return BattleSnapshotDecision.deny("item_role_conflict:" + itemId);
            }
            CanonicalItemInstance item = assetRepository.findItem(itemId).orElse(null);
            if (item == null) {
                return BattleSnapshotDecision.deny("unknown_item:" + itemId);
            }
            if (!item.ownerPlayerId().equals(playerId)) {
                return BattleSnapshotDecision.deny("item_not_owned:" + itemId);
            }
            if (item.quantity() < quantity) {
                return BattleSnapshotDecision.deny("insufficient_quantity:" + itemId);
            }
            items.put(itemId, new BattleItemSnapshot(
                    item.itemInstanceId(), item.ownerPlayerId(), item.templateId(), quantity, item.revision(), false));
        }

        String reservationId = reservationIds.get();
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException("reservation id supplier returned blank id");
        }

        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                reservationId,
                playerId,
                BattleTrainerSnapshot.from(player),
                roster,
                List.copyOf(items.values()),
                rngSeeds.getAsLong(),
                arena);

        if (!snapshotRepository.tryReserveSnapshot(snapshot)) {
            return BattleSnapshotDecision.deny("state_changed_or_assets_reserved");
        }
        return BattleSnapshotDecision.allow(snapshot);
    }

    public BattleSnapshotDecision releaseBattle(String playerId, String reservationId) {
        if (playerId == null || playerId.isBlank() || reservationId == null || reservationId.isBlank()) {
            return BattleSnapshotDecision.deny("invalid_request");
        }
        BattleAuthoritySnapshot snapshot = snapshotRepository.findSnapshot(reservationId).orElse(null);
        if (snapshot == null) {
            return BattleSnapshotDecision.deny("unknown_battle_reservation");
        }
        if (!snapshot.playerId().equals(playerId)) {
            return BattleSnapshotDecision.deny("battle_reservation_not_owned");
        }
        if (!snapshotRepository.releaseSnapshot(reservationId, playerId)) {
            return BattleSnapshotDecision.deny("battle_reservation_conflict");
        }
        return BattleSnapshotDecision.allow(snapshot);
    }
}
