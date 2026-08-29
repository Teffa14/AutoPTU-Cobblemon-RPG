package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Server-authoritative mutation boundary for selecting one already-accepted quest to track. */
public final class CanonicalQuestTrackingService {
    private static final int MAX_STALE_RETRIES = 16;

    private final CanonicalQuestCatalogue catalogue;
    private final FileCanonicalQuestJournalRepository repository;

    public CanonicalQuestTrackingService(
            CanonicalQuestCatalogue catalogue,
            FileCanonicalQuestJournalRepository repository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public TrackingResult track(String authenticatedPlayerId, String questId) {
        String playerId = requireId(authenticatedPlayerId, "authenticatedPlayerId");
        String requestedQuestId = requireId(questId, "questId");
        var quest = catalogue.quest(requestedQuestId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical questId: " + requestedQuestId));

        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            var current = repository.findOrCreate(playerId);
            if (!current.entries().containsKey(quest.questId())) {
                throw new IllegalArgumentException("quest is not in this Trainer's journal: " + quest.questId());
            }
            if (quest.questId().equals(current.trackedQuestId())) {
                return new TrackingResult(quest.questId(), current.revision(), false);
            }
            var result = repository.track(playerId, quest.questId(), current.revision());
            if (result.status() == FileCanonicalQuestJournalRepository.TrackStatus.TRACKED) {
                return new TrackingResult(quest.questId(), result.journal().revision(), true);
            }
            if (result.status() == FileCanonicalQuestJournalRepository.TrackStatus.ALREADY_TRACKED) {
                return new TrackingResult(quest.questId(), result.journal().revision(), false);
            }
        }
        throw new IllegalStateException("quest tracking retry exhausted because canonical journal kept changing");
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public record TrackingResult(String questId, long journalRevision, boolean changed) {
        public TrackingResult {
            questId = requireId(questId, "questId");
            if (journalRevision < 0) throw new IllegalArgumentException("journalRevision must not be negative");
        }
    }
}
