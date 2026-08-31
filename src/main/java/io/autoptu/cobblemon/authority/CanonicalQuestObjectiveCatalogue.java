package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server-authored quest objective bindings. Event keys describe RPG world observations only. */
public final class CanonicalQuestObjectiveCatalogue {
    public static final CanonicalQuestObjectiveCatalogue DEFAULT = new CanonicalQuestObjectiveCatalogue(List.of(
            new Objective("cedar-field-notes", "observe-lookout", "cedar_meadow:lookout_watching", "Observe the meadow lookout notice your approach."),
            new Objective("cedar-field-notes", "observe-feeding-group", "cedar_meadow:feeders_alarmed", "Observe the feeding group react to the lookout alarm.")
    ));

    private final List<Objective> objectives;
    private final Map<String, List<Objective>> byQuest;

    public CanonicalQuestObjectiveCatalogue(List<Objective> objectives) {
        Objects.requireNonNull(objectives, "objectives");
        LinkedHashMap<String, java.util.ArrayList<Objective>> mutable = new LinkedHashMap<>();
        java.util.HashSet<String> identities = new java.util.HashSet<>();
        for (Objective objective : objectives) {
            Objects.requireNonNull(objective, "objective");
            String identity = objective.questId() + "\u0000" + objective.objectiveId();
            if (!identities.add(identity)) throw new IllegalArgumentException("duplicate quest objective: " + objective.questId() + "/" + objective.objectiveId());
            mutable.computeIfAbsent(objective.questId(), ignored -> new java.util.ArrayList<>()).add(objective);
        }
        LinkedHashMap<String, List<Objective>> frozen = new LinkedHashMap<>();
        mutable.forEach((questId, values) -> frozen.put(questId, List.copyOf(values)));
        this.objectives = List.copyOf(objectives);
        this.byQuest = Map.copyOf(frozen);
    }

    public List<Objective> objectivesForQuest(String questId) {
        if (questId == null || questId.isBlank()) return List.of();
        return byQuest.getOrDefault(questId.trim(), List.of());
    }

    public List<Objective> objectivesForEvent(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) return List.of();
        String normalized = eventKey.trim();
        return objectives.stream().filter(objective -> objective.eventKey().equals(normalized)).toList();
    }

    public record Objective(String questId, String objectiveId, String eventKey, String description) {
        public Objective {
            questId = requireText(questId, "questId");
            objectiveId = requireText(objectiveId, "objectiveId");
            eventKey = requireText(eventKey, "eventKey");
            description = requireText(description, "description");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
