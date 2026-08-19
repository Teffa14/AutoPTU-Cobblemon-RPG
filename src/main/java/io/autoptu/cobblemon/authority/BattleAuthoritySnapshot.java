package io.autoptu.cobblemon.authority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BattleAuthoritySnapshot(
        String reservationId,
        String playerId,
        BattleTrainerSnapshot trainer,
        List<BattlePokemonSnapshot> roster,
        List<BattleItemSnapshot> items,
        long rngSeed,
        BattleArenaSnapshot arena
) {
    public BattleAuthoritySnapshot(
            String reservationId,
            String playerId,
            BattleTrainerSnapshot trainer,
            List<BattlePokemonSnapshot> roster,
            List<BattleItemSnapshot> items,
            long rngSeed
    ) {
        this(reservationId, playerId, trainer, roster, items, rngSeed, null);
    }

    public BattleAuthoritySnapshot {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        if (trainer == null || !trainer.playerId().equals(playerId)) {
            throw new IllegalArgumentException("trainer must belong to playerId");
        }
        roster = roster == null ? List.of() : List.copyOf(roster);
        items = items == null ? List.of() : List.copyOf(items);
        if (roster.isEmpty()) {
            throw new IllegalArgumentException("roster must not be empty");
        }

        Set<String> pokemonIds = new HashSet<>();
        for (BattlePokemonSnapshot pokemon : roster) {
            if (!pokemon.ownerPlayerId().equals(playerId)) {
                throw new IllegalArgumentException("all Pokémon must belong to playerId");
            }
            if (!pokemonIds.add(pokemon.pokemonId())) {
                throw new IllegalArgumentException("duplicate Pokémon in roster");
            }
        }

        Set<String> itemIds = new HashSet<>();
        for (BattleItemSnapshot item : items) {
            if (!item.ownerPlayerId().equals(playerId)) {
                throw new IllegalArgumentException("all items must belong to playerId");
            }
            if (!itemIds.add(item.itemInstanceId())) {
                throw new IllegalArgumentException("duplicate item in snapshot");
            }
        }
    }
}
