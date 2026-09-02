package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server-authored quest objective bindings. Event keys describe RPG world observations only. */
public final class CanonicalQuestObjectiveCatalogue {
    public static final String AUTHORED_QUEST_OBJECT_INSPECTED_EVENT = "rpg:authored_quest_object_inspected";
    private static final String NPC_TALK_EVENT_PREFIX = "rpg:npc_talked:";

    public static final CanonicalQuestObjectiveCatalogue DEFAULT = new CanonicalQuestObjectiveCatalogue(List.of(
            new Objective("cedar-field-notes", "observe-lookout", "cedar_meadow:lookout_watching", "Observe the meadow lookout notice your approach."),
            new Objective("cedar-field-notes", "observe-feeding-group", "cedar_meadow:feeders_alarmed", "Observe the feeding group react to the lookout alarm."),
            new Objective("cedar-field-notes", "inspect-field-notes", AUTHORED_QUEST_OBJECT_INSPECTED_EVENT, "Inspect the Ranger's authored field-notes lectern."),
            new Objective("cedar-observer-brief", "review-field-notes", AUTHORED_QUEST_OBJECT_INSPECTED_EVENT, "Revisit the Ranger's field-notes lectern after your first approach."),

            new Objective("marea-market-shortfall", "visit-loma-clara", "location:ouros.marea.loma_clara", "Visit Loma Clara and see the production side of the district."),
            new Objective("marea-market-shortfall", "visit-mirador", "location:ouros.marea.estacion_mirador", "Visit Estacion Mirador and inspect what observations are actually available."),

            new Objective("marea-route-field-check", "inspect-sendero", "location:ouros.marea.sendero_vidrio", "Inspect the ordinary condition of Sendero del Vidrio."),
            new Objective("marea-route-field-check", "inspect-crossing", "location:ouros.marea.sendero_crossing", "Inspect the seasonal crossing instead of inferring route condition from town reports."),

            new Objective("marea-mirador-observations", "visit-transect", "location:ouros.marea.mirador_transect", "Visit the Mirador transect trailhead."),
            new Objective("marea-mirador-observations", "visit-tideglass", "location:ouros.marea.tideglass_archive", "Visit Tideglass Archive to compare current observations with preserved records."),
            new Objective("marea-mirador-observations", "consult-taro", npcTalkedEvent("ouros.npc.taro_min"), "Talk with Taro Min at Tideglass Archive about what the preserved record can and cannot establish."),

            new Objective("marea-tideglass-comparison", "visit-storehouse", "location:ouros.marea.loma_storehouse", "Visit the Loma Clara cooperative storehouse."),
            new Objective("marea-tideglass-comparison", "visit-mirador", "location:ouros.marea.estacion_mirador", "Visit Estacion Mirador before treating archive records as a current explanation."),

            new Objective("marea-battle-yard-introduction", "visit-yard", "location:ouros.marea.bruma_battle_yard", "Visit the Bruma Battle Yard."),
            new Objective("marea-battle-yard-introduction", "review-trainer-record", "rpg:trainer_record_reviewed", "Review your current canonical Trainer record before a future audited spar.")
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

    public static String npcTalkedEvent(String npcId) {
        return NPC_TALK_EVENT_PREFIX + requireText(npcId, "npcId");
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
