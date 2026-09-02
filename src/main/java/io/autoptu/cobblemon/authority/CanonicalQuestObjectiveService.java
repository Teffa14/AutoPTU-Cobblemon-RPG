package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Advances authored quest objectives from server-observed RPG events only. */
public final class CanonicalQuestObjectiveService {
    private final CanonicalQuestObjectiveCatalogue objectives;
    private final FileCanonicalQuestJournalRepository journals;
    private final FileCanonicalQuestObjectiveRepository progress;

    public CanonicalQuestObjectiveService(
            CanonicalQuestObjectiveCatalogue objectives,
            FileCanonicalQuestJournalRepository journals,
            FileCanonicalQuestObjectiveRepository progress
    ) {
        this.objectives = Objects.requireNonNull(objectives, "objectives");
        this.journals = Objects.requireNonNull(journals, "journals");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public EventResult observe(String playerId, String eventKey) {
        var journal = journals.findOrCreate(playerId);
        List<ObjectiveUpdate> updates = new ArrayList<>();
        for (var objective : objectives.objectivesForEvent(eventKey)) {
            if (!journal.entries().containsKey(objective.questId())) continue;
            for (int attempt = 0; attempt < 4; attempt++) {
                var current = progress.findOrCreate(playerId);
                if (!prerequisitesComplete(current, objective)) break;
                var result = progress.complete(playerId, objective.questId(), objective.objectiveId(), current.revision());
                if (result.status() == FileCanonicalQuestObjectiveRepository.CompleteStatus.STALE_REVISION) continue;
                boolean newlyCompleted = result.status() == FileCanonicalQuestObjectiveRepository.CompleteStatus.COMPLETED;
                var snapshot = inspectQuest(playerId, objective.questId());
                updates.add(new ObjectiveUpdate(objective, newlyCompleted, snapshot));
                break;
            }
        }
        return new EventResult(List.copyOf(updates));
    }

    private static boolean prerequisitesComplete(
            FileCanonicalQuestObjectiveRepository.State current,
            CanonicalQuestObjectiveCatalogue.Objective objective
    ) {
        return objective.requiredObjectiveIds().stream().allMatch(requiredObjectiveId ->
                current.completedObjectives().contains(
                        FileCanonicalQuestObjectiveRepository.key(objective.questId(), requiredObjectiveId)
                )
        );
    }

    public QuestProgress inspectQuest(String playerId, String questId) {
        var configured = objectives.objectivesForQuest(questId);
        var state = progress.findOrCreate(playerId);
        List<ObjectiveProgress> values = configured.stream()
                .map(objective -> new ObjectiveProgress(
                        objective,
                        state.completedObjectives().contains(FileCanonicalQuestObjectiveRepository.key(objective.questId(), objective.objectiveId()))
                ))
                .toList();
        long completed = values.stream().filter(ObjectiveProgress::completed).count();
        return new QuestProgress(questId, values, completed, values.size(), !values.isEmpty() && completed == values.size(), state.revision());
    }

    public record EventResult(List<ObjectiveUpdate> updates) {
        public EventResult { updates = List.copyOf(updates); }
        public boolean changed() { return updates.stream().anyMatch(ObjectiveUpdate::newlyCompleted); }
    }
    public record ObjectiveUpdate(CanonicalQuestObjectiveCatalogue.Objective objective, boolean newlyCompleted, QuestProgress questProgress) {}
    public record ObjectiveProgress(CanonicalQuestObjectiveCatalogue.Objective objective, boolean completed) {}
    public record QuestProgress(String questId, List<ObjectiveProgress> objectives, long completedCount, long totalCount, boolean complete, long revision) {
        public QuestProgress { objectives = List.copyOf(objectives); }
    }
}
