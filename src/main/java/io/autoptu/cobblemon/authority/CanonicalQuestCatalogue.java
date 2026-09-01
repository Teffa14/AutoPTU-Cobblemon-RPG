package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-owned quest definitions. Quest objectives/rewards/prerequisites are RPG data only and carry no PTU legality. */
public final class CanonicalQuestCatalogue {
    public static final CanonicalQuestCatalogue DEFAULT = new CanonicalQuestCatalogue(List.of(
            new Quest(
                    "cedar-field-notes",
                    "cedar-ranger",
                    "Cedar Field Notes",
                    "The Cedar Ranger asked you to begin a field journal for Cedar Meadow.",
                    "Return after observing the meadow lookout and feeding group."
            ),
            new Quest(
                    "cedar-observer-brief",
                    "cedar-ranger",
                    "The Patient Approach",
                    "The Cedar Ranger has a follow-up brief for Trainers who chose to observe the meadow before approaching.",
                    "Compare what you noticed before contact with what changed after you entered the meadow.",
                    List.of(),
                    List.of("cedar_meadow_observe_first")
            ),
            new Quest(
                    "marea-market-shortfall",
                    "ouros.npc.ivo_serrat",
                    "The Thin Delivery Season",
                    "Ivo Serrat wants evidence before Puerto Bruma turns several uneven deliveries into one convenient story.",
                    "Visit Loma Clara and Estacion Mirador, then compare what each place can actually establish."
            ),
            new Quest(
                    "marea-route-field-check",
                    "ouros.npc.mara_veyra",
                    "Reading the Sendero",
                    "Mara Veyra wants a direct route observation separated from rumor about the deliveries.",
                    "Inspect Sendero del Vidrio and its seasonal crossing.",
                    List.of("marea-market-shortfall")
            ),
            new Quest(
                    "marea-mirador-observations",
                    "ouros.npc.nerea_sol",
                    "What the Station Can Actually Say",
                    "Nerea Sol needs a field-aware reader to distinguish station observations from conclusions people attach to them.",
                    "Visit the Mirador transect trailhead and Tideglass Archive so current observations and preserved records can be compared."
            ),
            new Quest(
                    "marea-tideglass-comparison",
                    "ouros.npc.taro_min",
                    "The Record Is Not the Cause",
                    "Taro Min wants a current observation paired with an older source instead of letting one archive entry become the answer.",
                    "Visit the Loma Clara cooperative storehouse and Estacion Mirador before returning to the archive."
            ),
            new Quest(
                    "marea-battle-yard-introduction",
                    "ouros.npc.sela_orrin",
                    "Measure the Change",
                    "Sela Orrin wants the Battle Yard to remember progression through repeatable evidence, not reputation alone.",
                    "Visit the Bruma Battle Yard and review your current Trainer record before requesting an audited spar."
            )
    ));

    private final Map<String, Quest> quests;

    public CanonicalQuestCatalogue(List<Quest> quests) {
        Objects.requireNonNull(quests, "quests");
        Map<String, Quest> indexed = new LinkedHashMap<>();
        for (Quest quest : quests) {
            Objects.requireNonNull(quest, "quest");
            if (indexed.putIfAbsent(quest.questId(), quest) != null) throw new IllegalArgumentException("duplicate questId: " + quest.questId());
        }
        for (Quest quest : indexed.values()) {
            for (String prerequisiteQuestId : quest.requiredAcceptedQuestIds()) {
                if (quest.questId().equals(prerequisiteQuestId)) throw new IllegalArgumentException("quest cannot require itself: " + quest.questId());
                if (!indexed.containsKey(prerequisiteQuestId)) throw new IllegalArgumentException("unknown prerequisite questId: " + prerequisiteQuestId);
            }
        }
        this.quests = Map.copyOf(indexed);
    }

    public Optional<Quest> quest(String questId) {
        if (questId == null || questId.isBlank()) return Optional.empty();
        return Optional.ofNullable(quests.get(questId));
    }

    public record Quest(String questId, String giverNpcId, String title, String summary, String objectiveText,
                        List<String> requiredAcceptedQuestIds, List<String> requiredStoryFlags) {
        public Quest(String questId, String giverNpcId, String title, String summary, String objectiveText) {
            this(questId, giverNpcId, title, summary, objectiveText, List.of(), List.of());
        }
        public Quest(String questId, String giverNpcId, String title, String summary, String objectiveText,
                     List<String> requiredAcceptedQuestIds) {
            this(questId, giverNpcId, title, summary, objectiveText, requiredAcceptedQuestIds, List.of());
        }
        public Quest {
            questId = requireText(questId, "questId"); giverNpcId = requireText(giverNpcId, "giverNpcId");
            title = requireText(title, "title"); summary = requireText(summary, "summary"); objectiveText = requireText(objectiveText, "objectiveText");
            requiredAcceptedQuestIds = validateUnique(requiredAcceptedQuestIds, "requiredAcceptedQuestIds", "duplicate prerequisite questId: ");
            requiredStoryFlags = validateUnique(requiredStoryFlags, "requiredStoryFlags", "duplicate prerequisite story flag: ");
        }
    }

    private static List<String> validateUnique(List<String> values, String field, String duplicatePrefix) {
        List<String> copy = List.copyOf(Objects.requireNonNull(values, field));
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (String value : copy) {
            String id = requireText(value, field);
            if (!ids.add(id)) throw new IllegalArgumentException(duplicatePrefix + id);
        }
        return copy;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
