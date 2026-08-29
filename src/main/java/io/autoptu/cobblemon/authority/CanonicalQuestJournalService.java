package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Server-authoritative quest acceptance boundary. */
public final class CanonicalQuestJournalService {
    private final CanonicalQuestCatalogue catalogue;
    private final FileCanonicalQuestJournalRepository repository;

    public CanonicalQuestJournalService(CanonicalQuestCatalogue catalogue, FileCanonicalQuestJournalRepository repository) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public FileCanonicalQuestJournalRepository.JournalState inspect(String playerId) {
        return repository.findOrCreate(playerId);
    }

    public AcceptQuestResult accept(String playerId, String npcId, String questId) {
        var quest = catalogue.quest(questId).orElseThrow(() -> new IllegalArgumentException("unknown canonical questId: " + questId));
        if (!quest.giverNpcId().equals(npcId)) throw new IllegalArgumentException("quest is not offered by this NPC");
        for (int attempt = 0; attempt < 4; attempt++) {
            var current = repository.findOrCreate(playerId);
            var result = repository.accept(playerId, quest.questId(), current.revision());
            if (result.status() == FileCanonicalQuestJournalRepository.AcceptStatus.STALE_REVISION) continue;
            return new AcceptQuestResult(quest, result);
        }
        throw new IllegalStateException("canonical quest journal changed repeatedly during acceptance");
    }

    public record AcceptQuestResult(CanonicalQuestCatalogue.Quest quest, FileCanonicalQuestJournalRepository.AcceptResult commit) {
        public boolean newlyAccepted() {
            return commit.status() == FileCanonicalQuestJournalRepository.AcceptStatus.ACCEPTED;
        }
    }
}
