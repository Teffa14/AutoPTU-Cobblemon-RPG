package io.autoptu.cobblemon.authority;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BattleOutcomeCommit(
        String reservationId,
        String playerId,
        String engineTranscriptDigest,
        long trainerRevision,
        Map<String, Long> pokemonRevisions,
        List<BattleItemConsumption> consumedItems
) {
    public BattleOutcomeCommit {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        if (engineTranscriptDigest == null || engineTranscriptDigest.isBlank()) {
            throw new IllegalArgumentException("engineTranscriptDigest must not be blank");
        }
        if (trainerRevision < 0) {
            throw new IllegalArgumentException("trainerRevision must be >= 0");
        }
        pokemonRevisions = pokemonRevisions == null ? Map.of() : Map.copyOf(pokemonRevisions);
        consumedItems = consumedItems == null ? List.of() : List.copyOf(consumedItems);
        pokemonRevisions.forEach((pokemonId, revision) -> {
            if (pokemonId == null || pokemonId.isBlank() || revision == null || revision < 0) {
                throw new IllegalArgumentException("invalid Pokémon revision");
            }
        });
        Set<String> consumedIds = new HashSet<>();
        for (BattleItemConsumption consumption : consumedItems) {
            if (!consumedIds.add(consumption.itemInstanceId())) {
                throw new IllegalArgumentException("duplicate consumed item");
            }
        }
    }
}
