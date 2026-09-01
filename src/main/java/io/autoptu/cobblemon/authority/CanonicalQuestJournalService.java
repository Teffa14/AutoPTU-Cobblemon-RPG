package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Server-authoritative quest acceptance boundary. */
public final class CanonicalQuestJournalService {
    private final CanonicalQuestCatalogue catalogue;
    private final FileCanonicalQuestJournalRepository repository;
    private final FileCanonicalWorldStoryRepository worldStoryRepository;
    private final FileCanonicalNpcRelationshipRepository relationshipRepository;

    public CanonicalQuestJournalService(CanonicalQuestCatalogue catalogue, FileCanonicalQuestJournalRepository repository) {
        this(catalogue, repository, null, null);
    }

    public CanonicalQuestJournalService(
            CanonicalQuestCatalogue catalogue,
            FileCanonicalQuestJournalRepository repository,
            FileCanonicalWorldStoryRepository worldStoryRepository
    ) {
        this(catalogue, repository, worldStoryRepository, null);
    }

    public CanonicalQuestJournalService(
            CanonicalQuestCatalogue catalogue,
            FileCanonicalQuestJournalRepository repository,
            FileCanonicalWorldStoryRepository worldStoryRepository,
            FileCanonicalNpcRelationshipRepository relationshipRepository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.worldStoryRepository = worldStoryRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public FileCanonicalQuestJournalRepository.JournalState inspect(String playerId) {
        return repository.findOrCreate(playerId);
    }

    public QuestEligibility eligibility(String playerId, String npcId, String questId) {
        var quest = requireOfferedQuest(npcId, questId);
        var journal = repository.findOrCreate(playerId);
        var story = storyState(playerId);
        return eligibility(playerId, quest, journal, story);
    }

    public AcceptQuestResult accept(String playerId, String npcId, String questId) {
        var quest = requireOfferedQuest(npcId, questId);
        for (int attempt = 0; attempt < 4; attempt++) {
            var current = repository.findOrCreate(playerId);
            if (current.entries().containsKey(quest.questId())) {
                var result = repository.accept(playerId, quest.questId(), current.revision());
                return new AcceptQuestResult(
                        quest,
                        AcceptQuestStatus.ALREADY_ACCEPTED,
                        result.journal(),
                        result,
                        List.of(),
                        List.of(),
                        List.of()
                );
            }

            var eligibility = eligibility(playerId, quest, current, storyState(playerId));
            if (!eligibility.eligible()) {
                return new AcceptQuestResult(
                        quest,
                        AcceptQuestStatus.BLOCKED_PREREQUISITES,
                        current,
                        null,
                        eligibility.missingAcceptedQuestIds(),
                        eligibility.missingStoryFlags(),
                        eligibility.missingMetNpcIds()
                );
            }

            var result = repository.accept(playerId, quest.questId(), current.revision());
            if (result.status() == FileCanonicalQuestJournalRepository.AcceptStatus.STALE_REVISION) continue;
            return new AcceptQuestResult(
                    quest,
                    result.status() == FileCanonicalQuestJournalRepository.AcceptStatus.ACCEPTED
                            ? AcceptQuestStatus.ACCEPTED
                            : AcceptQuestStatus.ALREADY_ACCEPTED,
                    result.journal(),
                    result,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        throw new IllegalStateException("canonical quest journal changed repeatedly during acceptance");
    }

    private CanonicalQuestCatalogue.Quest requireOfferedQuest(String npcId, String questId) {
        var quest = catalogue.quest(questId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical questId: " + questId));
        if (!quest.giverNpcId().equals(npcId)) throw new IllegalArgumentException("quest is not offered by this NPC");
        return quest;
    }

    private QuestEligibility eligibility(
            String playerId,
            CanonicalQuestCatalogue.Quest quest,
            FileCanonicalQuestJournalRepository.JournalState journal,
            StoryState story
    ) {
        if (journal.entries().containsKey(quest.questId())) {
            return new QuestEligibility(quest, true, List.of(), List.of(), List.of(), journal.revision(), story.revision());
        }
        List<String> missingQuests = quest.requiredAcceptedQuestIds().stream()
                .filter(requiredQuestId -> !journal.entries().containsKey(requiredQuestId))
                .toList();
        List<String> missingStoryFlags = quest.requiredStoryFlags().stream()
                .filter(requiredFlag -> !story.flags().contains(requiredFlag))
                .toList();
        List<String> missingMetNpcIds = quest.requiredMetNpcIds().stream()
                .filter(requiredNpcId -> !hasMet(playerId, requiredNpcId))
                .toList();
        return new QuestEligibility(
                quest,
                missingQuests.isEmpty() && missingStoryFlags.isEmpty() && missingMetNpcIds.isEmpty(),
                missingQuests,
                missingStoryFlags,
                missingMetNpcIds,
                journal.revision(),
                story.revision()
        );
    }

    private StoryState storyState(String playerId) {
        if (worldStoryRepository == null) return new StoryState(Set.of(), 0L);
        var story = worldStoryRepository.findOrCreate(playerId);
        return new StoryState(story.storyFlags(), story.revision());
    }

    private boolean hasMet(String playerId, String npcId) {
        if (relationshipRepository == null) return false;
        return relationshipRepository.find(playerId, npcId)
                .map(FileCanonicalNpcRelationshipRepository.RelationshipState::met)
                .orElse(false);
    }

    private record StoryState(Set<String> flags, long revision) {
        private StoryState {
            flags = Set.copyOf(Objects.requireNonNull(flags, "flags"));
            if (revision < 0) throw new IllegalArgumentException("story revision must not be negative");
        }
    }

    public record QuestEligibility(
            CanonicalQuestCatalogue.Quest quest,
            boolean eligible,
            List<String> missingAcceptedQuestIds,
            List<String> missingStoryFlags,
            List<String> missingMetNpcIds,
            long journalRevision,
            long storyRevision
    ) {
        public QuestEligibility {
            Objects.requireNonNull(quest, "quest");
            missingAcceptedQuestIds = List.copyOf(Objects.requireNonNull(missingAcceptedQuestIds, "missingAcceptedQuestIds"));
            missingStoryFlags = List.copyOf(Objects.requireNonNull(missingStoryFlags, "missingStoryFlags"));
            missingMetNpcIds = List.copyOf(Objects.requireNonNull(missingMetNpcIds, "missingMetNpcIds"));
            if (journalRevision < 0) throw new IllegalArgumentException("journalRevision must not be negative");
            if (storyRevision < 0) throw new IllegalArgumentException("storyRevision must not be negative");
            if (eligible && (!missingAcceptedQuestIds.isEmpty() || !missingStoryFlags.isEmpty() || !missingMetNpcIds.isEmpty())) {
                throw new IllegalArgumentException("eligible quest cannot have missing prerequisites");
            }
        }
    }

    public enum AcceptQuestStatus { ACCEPTED, ALREADY_ACCEPTED, BLOCKED_PREREQUISITES }

    public record AcceptQuestResult(
            CanonicalQuestCatalogue.Quest quest,
            AcceptQuestStatus status,
            FileCanonicalQuestJournalRepository.JournalState journal,
            FileCanonicalQuestJournalRepository.AcceptResult commit,
            List<String> missingAcceptedQuestIds,
            List<String> missingStoryFlags,
            List<String> missingMetNpcIds
    ) {
        public AcceptQuestResult {
            Objects.requireNonNull(quest, "quest");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(journal, "journal");
            missingAcceptedQuestIds = List.copyOf(Objects.requireNonNull(missingAcceptedQuestIds, "missingAcceptedQuestIds"));
            missingStoryFlags = List.copyOf(Objects.requireNonNull(missingStoryFlags, "missingStoryFlags"));
            missingMetNpcIds = List.copyOf(Objects.requireNonNull(missingMetNpcIds, "missingMetNpcIds"));
            if (status == AcceptQuestStatus.BLOCKED_PREREQUISITES
                    && missingAcceptedQuestIds.isEmpty()
                    && missingStoryFlags.isEmpty()
                    && missingMetNpcIds.isEmpty()) {
                throw new IllegalArgumentException("blocked quest requires at least one missing prerequisite");
            }
            if (status != AcceptQuestStatus.BLOCKED_PREREQUISITES && commit == null) {
                throw new IllegalArgumentException("accepted quest result requires repository commit evidence");
            }
        }

        public boolean newlyAccepted() {
            return status == AcceptQuestStatus.ACCEPTED;
        }

        public boolean blockedByPrerequisites() {
            return status == AcceptQuestStatus.BLOCKED_PREREQUISITES;
        }
    }
}
