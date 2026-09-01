package io.autoptu.cobblemon.authority;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-owned questline graph metadata.
 *
 * Questlines organize authored world content. They do not decide PTU legality, battle outcomes,
 * rewards, or client presentation. A quest may participate in multiple narrative families through
 * its containing questlines without duplicating the underlying world event.
 */
public final class CanonicalQuestlineCatalogue {
    public static final CanonicalQuestlineCatalogue DEFAULT = new CanonicalQuestlineCatalogue(List.of(
            new Questline(
                    "ouros.marea.thin_delivery_season",
                    "The Thin Delivery Season",
                    Set.of(QuestlineType.REGION, QuestlineType.SETTLEMENT),
                    List.of(
                            "marea-market-shortfall",
                            "marea-route-field-check",
                            "marea-mirador-observations",
                            "marea-tideglass-comparison"
                    ),
                    List.of(
                            "ouros.marea.puerto_bruma",
                            "ouros.marea.sendero_vidrio",
                            "ouros.marea.loma_clara",
                            "ouros.marea.estacion_mirador"
                    ),
                    List.of(
                            "ouros.npc.ivo_serrat",
                            "ouros.npc.mara_veyra",
                            "ouros.npc.nerea_sol",
                            "ouros.npc.taro_min"
                    ),
                    List.of(
                            "ouros.faction.marea_field_office",
                            "ouros.faction.loma_cooperative",
                            "ouros.faction.tideglass_archive"
                    ),
                    List.of()
            ),
            new Questline(
                    "ouros.marea.route_evidence",
                    "Reading the Sendero",
                    Set.of(QuestlineType.CLASS, QuestlineType.FACTION, QuestlineType.EXPLORATION),
                    List.of("marea-route-field-check"),
                    List.of("ouros.marea.sendero_vidrio"),
                    List.of("ouros.npc.mara_veyra"),
                    List.of("ouros.faction.marea_field_office"),
                    List.of("ouros.marea.thin_delivery_season")
            ),
            new Questline(
                    "ouros.marea.mirador_evidence",
                    "What the Station Can Actually Say",
                    Set.of(QuestlineType.CLASS, QuestlineType.EXPLORATION, QuestlineType.CHARACTER),
                    List.of("marea-mirador-observations"),
                    List.of("ouros.marea.estacion_mirador"),
                    List.of("ouros.npc.nerea_sol"),
                    List.of(),
                    List.of("ouros.marea.thin_delivery_season")
            ),
            new Questline(
                    "ouros.marea.tideglass_comparison",
                    "The Record Is Not the Cause",
                    Set.of(QuestlineType.CLASS, QuestlineType.FACTION, QuestlineType.ITEM),
                    List.of("marea-tideglass-comparison"),
                    List.of("ouros.marea.puerto_bruma", "ouros.marea.loma_clara"),
                    List.of("ouros.npc.taro_min"),
                    List.of("ouros.faction.tideglass_archive"),
                    List.of("ouros.marea.thin_delivery_season")
            ),
            new Questline(
                    "ouros.marea.bruma_yard_circuit",
                    "Bruma Yard: Measure the Change",
                    Set.of(QuestlineType.COMPETITIVE, QuestlineType.RIVAL, QuestlineType.RELATIONSHIP),
                    List.of("marea-battle-yard-introduction"),
                    List.of("ouros.marea.bruma_battle_yard"),
                    List.of("ouros.npc.sela_orrin", "ouros.npc.jace_orrin"),
                    List.of("ouros.faction.bruma_battle_yard"),
                    List.of()
            )
    ));

    private final Map<String, Questline> questlines;

    public CanonicalQuestlineCatalogue(List<Questline> questlines) {
        Objects.requireNonNull(questlines, "questlines");
        LinkedHashMap<String, Questline> indexed = new LinkedHashMap<>();
        for (Questline questline : questlines) {
            Objects.requireNonNull(questline, "questline");
            if (indexed.putIfAbsent(questline.questlineId(), questline) != null) {
                throw new IllegalArgumentException("duplicate questlineId: " + questline.questlineId());
            }
        }
        this.questlines = Map.copyOf(indexed);
    }

    public Optional<Questline> questline(String questlineId) {
        if (questlineId == null || questlineId.isBlank()) return Optional.empty();
        return Optional.ofNullable(questlines.get(questlineId.strip()));
    }

    public List<Questline> questlines() {
        return List.copyOf(questlines.values());
    }

    public List<Questline> questlinesForQuest(String questId) {
        if (questId == null || questId.isBlank()) return List.of();
        String normalized = questId.strip();
        return questlines.values().stream().filter(value -> value.questIds().contains(normalized)).toList();
    }

    public enum QuestlineType {
        MAIN,
        CLASS,
        SECONDARY,
        REGION,
        FACTION,
        POKEMON,
        DUNGEON,
        EQUIPMENT,
        ITEM,
        RELATIONSHIP,
        RIVAL,
        SERVER_EVENT,
        CHARACTER,
        EXPLORATION,
        COMPETITIVE,
        SETTLEMENT
    }

    public record Questline(
            String questlineId,
            String title,
            Set<QuestlineType> types,
            List<String> questIds,
            List<String> locationIds,
            List<String> npcIds,
            List<String> factionIds,
            List<String> parentQuestlineIds
    ) {
        public Questline {
            questlineId = requireText(questlineId, "questlineId");
            title = requireText(title, "title");
            Objects.requireNonNull(types, "types");
            if (types.isEmpty()) throw new IllegalArgumentException("questline types cannot be empty");
            types = Set.copyOf(EnumSet.copyOf(types));
            questIds = copyTextList(questIds, "questIds");
            locationIds = copyTextList(locationIds, "locationIds");
            npcIds = copyTextList(npcIds, "npcIds");
            factionIds = copyTextList(factionIds, "factionIds");
            parentQuestlineIds = copyTextList(parentQuestlineIds, "parentQuestlineIds");
        }
    }

    private static List<String> copyTextList(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        return values.stream().map(value -> requireText(value, field)).toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
