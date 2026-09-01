package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-owned dialogue content for physical Ouros NPC presentation actors. */
public final class CanonicalNpcDialogueCatalogue {
    public static final CanonicalNpcDialogueCatalogue DEFAULT = new CanonicalNpcDialogueCatalogue(List.of(
            new Dialogue(
                    "cedar-ranger",
                    "Cedar Ranger",
                    "Welcome to Cedar Meadow. The Pokemon here react to how you approach them, so watch the field before you commit.",
                    List.of(
                            new Option("meadow", "What should I watch for?", "The lookout notices movement first. If it raises the alarm, the feeding group will move for cover."),
                            new Option("field-notes", "Anything I can help with?", "Start a field journal for me. Observe the meadow lookout and feeding group, then come back to collect the field stipend.", "cedar-field-notes"),
                            new Option("facilities", "Where can I prepare?", "Use the healing machine, PC, Mart and crafting stations you find in the world. Their state belongs to your canonical Trainer."),
                            Option.challenge("field-spar", "Challenge Cedar Ranger", "Let's see whether your party is ready for a proper field spar.", "cedar-ranger-field-spar"),
                            new Option("battles", "How do battles work here?", "When a battle starts, AutoPTU decides legal actions and outcomes. The Minecraft world only presents the authoritative result.")
                    )
            ),
            new Dialogue(
                    "ouros.npc.ivo_serrat",
                    "Ivo Serrat",
                    "Three uneven deliveries and half the market already has one perfect explanation. I would rather know what actually happened.",
                    List.of(
                            new Option("shortfall", "What is wrong with the deliveries?", "They are smaller and less predictable than last season. That is the observation. Crop failure, road trouble and hoarding are still claims."),
                            new Option("thin-season", "I can compare the evidence.", "Start with Loma Clara and Mirador. Production records and field observations answer different questions, and I want both kept separate.", "marea-market-shortfall"),
                            new Option("work", "What do you do here?", "I buy for the communal kitchen, coordinate suppliers and keep substitutions honest. If a dish changes, I would rather say it changed than pretend nobody notices."),
                            new Option("pepa", "And Pepa?", "Pepa is my Greedent partner. She has habits and a job around the kitchen, but species alone does not give her a magical storage bonus.")
                    )
            ),
            new Dialogue(
                    "ouros.npc.mara_veyra",
                    "Mara Veyra",
                    "If you need a conclusion, bring me evidence. If you need a route checked, I can give you a route.",
                    List.of(
                            new Option("office", "What does the Field Office handle?", "Route reports, wildlife incidents, missing-person searches and practical coordination. We are not a police force and we do not own every Pokemon we help."),
                            new Option("route-check", "I can inspect the Sendero.", "Good. Check the ordinary road and the seasonal crossing separately. A blocked cart in one place does not make the whole route impassable.", "marea-route-field-check"),
                            new Option("delivery", "Is the road causing the shortfall?", "Maybe. We have reports, not a verdict. I will not turn correlation into a cause because the market is impatient."),
                            new Option("kite", "Where is Kite?", "Kite is my Corviknight partner. Travel and observation are part of our history; battle state still belongs to AutoPTU when a battle actually exists.")
                    )
            ),
            new Dialogue(
                    "ouros.npc.nerea_sol",
                    "Dr. Nerea Sol",
                    "The station has measurements, gaps, revisions and a great many people who prefer to remember only the first of those.",
                    List.of(
                            new Option("station", "What does Mirador measure?", "Weather observations, ecological transects and route-adjacent field notes. Each record has a time, method and limits."),
                            new Option("observations", "I can compare the observations.", "Walk the transect trailhead, then read the preserved copies at Tideglass. Current conditions and historical records should inform each other without becoming the same thing.", "marea-mirador-observations"),
                            new Option("cause", "Did weather cause the delivery problem?", "That is a hypothesis. We have relevant weather observations, but relevance is not causation."),
                            new Option("lumen", "What does Lumen do?", "Lumen is my Heliolisk field partner. I do not treat his species as a weather instrument or an electrical generator by default.")
                    )
            ),
            new Dialogue(
                    "ouros.npc.taro_min",
                    "Taro Min",
                    "A useful archive preserves the wrong recollection beside the right date instead of quietly editing the town into agreement.",
                    List.of(
                            new Option("archive", "What is kept here?", "Route surveys, market records, oral-history deposits and copies of field observations. The record tells you what was recorded, not automatically what was true."),
                            new Option("comparison", "Give me something to compare.", "Visit the cooperative storehouse and Mirador. Bring current context back to the old record instead of asking the old record to answer a new season by itself.", "marea-tideglass-comparison"),
                            new Option("testimony", "Why keep contradictory testimony?", "Because disagreement is evidence about memory, perspective and public belief. Deleting one version would destroy information."),
                            new Option("margin", "Does Margin help verify stories?", "Margin is my Noctowl companion, not a truth detector. Observation and truth still need provenance.")
                    )
            ),
            new Dialogue(
                    "ouros.npc.sela_orrin",
                    "Sela Orrin",
                    "A rematch is useful only if something changed. Otherwise we are just repeating ourselves with better posture.",
                    List.of(
                            new Option("yard", "What is the Battle Yard?", "A local training and battle institution. It is not a Gym just because people fight here, and I do not hand out invented badges."),
                            new Option("measure-change", "How do I start here?", "See the yard, review your Trainer record, then come back. If we spar later, AutoPTU will own every legal action and result.", "marea-battle-yard-introduction"),
                            new Option("delivery", "Can battling help the delivery problem?", "It can help if a real field situation requires a battle. Winning a match does not explain missing produce."),
                            new Option("rook", "Tell me about Rook.", "Rook is my Falinks partner. His effective battle level can grow with serious challengers, but his identity and our history do not reroll every encounter.")
                    )
            )
    ));

    private final Map<String, Dialogue> dialogues;

    public CanonicalNpcDialogueCatalogue(List<Dialogue> dialogues) {
        Objects.requireNonNull(dialogues, "dialogues");
        Map<String, Dialogue> indexed = new LinkedHashMap<>();
        for (Dialogue dialogue : dialogues) {
            Objects.requireNonNull(dialogue, "dialogue");
            if (indexed.putIfAbsent(dialogue.npcId(), dialogue) != null) throw new IllegalArgumentException("duplicate npcId: " + dialogue.npcId());
        }
        this.dialogues = Map.copyOf(indexed);
    }

    public Optional<Dialogue> dialogue(String npcId) {
        if (npcId == null || npcId.isBlank()) return Optional.empty();
        return Optional.ofNullable(dialogues.get(npcId));
    }

    public record Dialogue(String npcId, String displayName, String openingLine, List<Option> options) {
        public Dialogue {
            npcId = requireText(npcId, "npcId");
            displayName = requireText(displayName, "displayName");
            openingLine = requireText(openingLine, "openingLine");
            options = List.copyOf(Objects.requireNonNull(options, "options"));
            if (options.isEmpty()) throw new IllegalArgumentException("dialogue options cannot be empty");
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            for (Option option : options) {
                Objects.requireNonNull(option, "option");
                if (!ids.add(option.optionId())) throw new IllegalArgumentException("duplicate optionId: " + option.optionId());
            }
        }

        public Optional<Option> option(String optionId) {
            if (optionId == null || optionId.isBlank()) return Optional.empty();
            return options.stream().filter(option -> option.optionId().equals(optionId)).findFirst();
        }
    }

    public record Option(String optionId, String label, String response, String questId, String challengeId) {
        public Option(String optionId, String label, String response) { this(optionId, label, response, null, null); }
        public Option(String optionId, String label, String response, String questId) { this(optionId, label, response, questId, null); }
        public static Option challenge(String optionId, String label, String response, String challengeId) {
            return new Option(optionId, label, response, null, challengeId);
        }
        public Option {
            optionId = requireText(optionId, "optionId");
            label = requireText(label, "label");
            response = requireText(response, "response");
            if (questId != null) questId = requireText(questId, "questId");
            if (challengeId != null) challengeId = requireText(challengeId, "challengeId");
            if (questId != null && challengeId != null) throw new IllegalArgumentException("dialogue option cannot be both quest and challenge action");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
