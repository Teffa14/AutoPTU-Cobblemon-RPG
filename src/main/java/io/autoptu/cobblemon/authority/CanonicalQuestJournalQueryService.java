package io.autoptu.cobblemon.authority;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Read-only server-authoritative projection of one Trainer's durable quest journal. */
public final class CanonicalQuestJournalQueryService {
    private final CanonicalQuestCatalogue catalogue;
    private final FileCanonicalQuestJournalRepository repository;

    public CanonicalQuestJournalQueryService(CanonicalQuestCatalogue catalogue, FileCanonicalQuestJournalRepository repository) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public JournalSnapshot inspect(String playerId) {
        var journal = repository.findOrCreate(playerId);
        List<QuestSnapshot> quests = journal.entries().values().stream()
                .map(entry -> snapshot(entry, catalogue.quest(entry.questId())
                        .orElseThrow(() -> new IllegalStateException("quest journal references unknown canonical questId: " + entry.questId())),
                        entry.questId().equals(journal.trackedQuestId())))
                .sorted(Comparator.comparing(QuestSnapshot::questId))
                .toList();
        return new JournalSnapshot(journal.playerId(), journal.revision(), journal.trackedQuestId(), quests);
    }

    public QuestSnapshot inspectQuest(String playerId, String questId) {
        var quest = catalogue.quest(questId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical questId: " + questId));
        var journal = repository.findOrCreate(playerId);
        var entry = journal.entries().get(quest.questId());
        if (entry == null) throw new IllegalArgumentException("quest is not in this Trainer's journal: " + quest.questId());
        return snapshot(entry, quest, quest.questId().equals(journal.trackedQuestId()));
    }

    private static QuestSnapshot snapshot(
            FileCanonicalQuestJournalRepository.QuestEntry entry,
            CanonicalQuestCatalogue.Quest quest,
            boolean tracked
    ) {
        return new QuestSnapshot(
                quest.questId(),
                quest.title(),
                quest.summary(),
                quest.objectiveText(),
                entry.state().name(),
                entry.acceptedRevision(),
                tracked
        );
    }

    public record JournalSnapshot(String playerId, long revision, String trackedQuestId, List<QuestSnapshot> quests) {
        public JournalSnapshot {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
            if (trackedQuestId != null && trackedQuestId.isBlank()) throw new IllegalArgumentException("trackedQuestId must not be blank");
            quests = List.copyOf(Objects.requireNonNull(quests, "quests"));
        }
    }

    public record QuestSnapshot(
            String questId,
            String title,
            String summary,
            String objectiveText,
            String state,
            long acceptedRevision,
            boolean tracked
    ) {}
}
