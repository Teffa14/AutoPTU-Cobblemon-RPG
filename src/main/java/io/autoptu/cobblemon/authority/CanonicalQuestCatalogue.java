package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-owned quest definitions. Quest objectives/rewards are data only and carry no PTU legality. */
public final class CanonicalQuestCatalogue {
    public static final CanonicalQuestCatalogue DEFAULT = new CanonicalQuestCatalogue(List.of(
            new Quest(
                    "cedar-field-notes",
                    "cedar-ranger",
                    "Cedar Field Notes",
                    "The Cedar Ranger asked you to begin a field journal for Cedar Meadow.",
                    "Return after observing the meadow lookout and feeding group."
            )
    ));

    private final Map<String, Quest> quests;

    public CanonicalQuestCatalogue(List<Quest> quests) {
        Objects.requireNonNull(quests, "quests");
        Map<String, Quest> indexed = new LinkedHashMap<>();
        for (Quest quest : quests) {
            Objects.requireNonNull(quest, "quest");
            if (indexed.putIfAbsent(quest.questId(), quest) != null) {
                throw new IllegalArgumentException("duplicate questId: " + quest.questId());
            }
        }
        this.quests = Map.copyOf(indexed);
    }

    public Optional<Quest> quest(String questId) {
        if (questId == null || questId.isBlank()) return Optional.empty();
        return Optional.ofNullable(quests.get(questId));
    }

    public record Quest(String questId, String giverNpcId, String title, String summary, String objectiveText) {
        public Quest {
            questId = requireText(questId, "questId");
            giverNpcId = requireText(giverNpcId, "giverNpcId");
            title = requireText(title, "title");
            summary = requireText(summary, "summary");
            objectiveText = requireText(objectiveText, "objectiveText");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
