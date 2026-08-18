package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleOutcomeCommitService {
    private final BattleSnapshotRepository snapshotRepository;
    private final BattleOutcomeRepository outcomeRepository;

    public BattleOutcomeCommitService(
            BattleSnapshotRepository snapshotRepository,
            BattleOutcomeRepository outcomeRepository
    ) {
        if (snapshotRepository == null || outcomeRepository == null) {
            throw new IllegalArgumentException("repositories must not be null");
        }
        this.snapshotRepository = snapshotRepository;
        this.outcomeRepository = outcomeRepository;
    }

    /**
     * Server-internal boundary for a result already produced by the trusted
     * battle engine. Client packets must never call this method directly.
     */
    public BattleOutcomeDecision commitEngineOutcome(
            String playerId,
            String reservationId,
            String engineTranscriptDigest,
            Map<String, Integer> consumedItemQuantities
    ) {
        if (playerId == null || playerId.isBlank()
                || reservationId == null || reservationId.isBlank()
                || engineTranscriptDigest == null || engineTranscriptDigest.isBlank()) {
            return BattleOutcomeDecision.deny("invalid_request");
        }

        Map<String, Integer> requestedConsumptions = normalizeConsumptions(consumedItemQuantities);
        if (requestedConsumptions == null) {
            return BattleOutcomeDecision.deny("invalid_consumption_request");
        }

        BattleOutcomeCommit existing = outcomeRepository.findCommittedOutcome(reservationId).orElse(null);
        if (existing != null) {
            return matchesRequest(existing, playerId, engineTranscriptDigest, requestedConsumptions)
                    ? BattleOutcomeDecision.idempotent(existing)
                    : BattleOutcomeDecision.deny("outcome_already_committed_with_different_payload");
        }

        BattleAuthoritySnapshot snapshot = snapshotRepository.findSnapshot(reservationId).orElse(null);
        if (snapshot == null) {
            return BattleOutcomeDecision.deny("unknown_battle_reservation");
        }
        if (!snapshot.playerId().equals(playerId)) {
            return BattleOutcomeDecision.deny("battle_reservation_not_owned");
        }

        Map<String, BattleItemSnapshot> reservedItems = new LinkedHashMap<>();
        for (BattleItemSnapshot item : snapshot.items()) {
            reservedItems.put(item.itemInstanceId(), item);
        }

        List<BattleItemConsumption> consumptions = new ArrayList<>();
        requestedConsumptions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    BattleItemSnapshot reserved = reservedItems.get(entry.getKey());
                    if (reserved != null) {
                        consumptions.add(new BattleItemConsumption(
                                reserved.itemInstanceId(),
                                reserved.templateId(),
                                entry.getValue(),
                                reserved.revision()));
                    }
                });

        for (Map.Entry<String, Integer> entry : requestedConsumptions.entrySet()) {
            BattleItemSnapshot reserved = reservedItems.get(entry.getKey());
            if (reserved == null) {
                return BattleOutcomeDecision.deny("item_not_reserved_for_battle:" + entry.getKey());
            }
            if (reserved.heldItem()) {
                return BattleOutcomeDecision.deny("held_item_cannot_be_consumed:" + entry.getKey());
            }
            if (entry.getValue() > reserved.reservedQuantity()) {
                return BattleOutcomeDecision.deny("consumption_exceeds_reservation:" + entry.getKey());
            }
        }

        Map<String, Long> pokemonRevisions = new LinkedHashMap<>();
        snapshot.roster().stream()
                .sorted(Comparator.comparing(BattlePokemonSnapshot::pokemonId))
                .forEach(pokemon -> pokemonRevisions.put(pokemon.pokemonId(), pokemon.revision()));

        BattleOutcomeCommit outcome = new BattleOutcomeCommit(
                reservationId,
                playerId,
                engineTranscriptDigest,
                snapshot.trainer().revision(),
                pokemonRevisions,
                consumptions);

        if (outcomeRepository.tryCommitOutcome(snapshot, outcome)) {
            return BattleOutcomeDecision.committed(outcome);
        }

        BattleOutcomeCommit raced = outcomeRepository.findCommittedOutcome(reservationId).orElse(null);
        if (raced != null && raced.equals(outcome)) {
            return BattleOutcomeDecision.idempotent(raced);
        }
        return BattleOutcomeDecision.deny("state_changed_or_outcome_conflict");
    }

    private static Map<String, Integer> normalizeConsumptions(Map<String, Integer> requested) {
        if (requested == null || requested.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            String itemId = entry.getKey();
            Integer quantity = entry.getValue();
            if (itemId == null || itemId.isBlank() || quantity == null || quantity <= 0) {
                return null;
            }
            normalized.put(itemId, quantity);
        }
        return Map.copyOf(normalized);
    }

    private static boolean matchesRequest(
            BattleOutcomeCommit existing,
            String playerId,
            String engineTranscriptDigest,
            Map<String, Integer> requestedConsumptions
    ) {
        if (!existing.playerId().equals(playerId)
                || !existing.engineTranscriptDigest().equals(engineTranscriptDigest)) {
            return false;
        }
        Map<String, Integer> committedConsumptions = new LinkedHashMap<>();
        for (BattleItemConsumption consumption : existing.consumedItems()) {
            committedConsumptions.put(consumption.itemInstanceId(), consumption.quantity());
        }
        return committedConsumptions.equals(requestedConsumptions);
    }
}
