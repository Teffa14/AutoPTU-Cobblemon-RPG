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
                            new Option(
                                    "meadow",
                                    "What should I watch for?",
                                    "The lookout notices movement first. If it raises the alarm, the feeding group will move for cover."
                            ),
                            new Option(
                                    "facilities",
                                    "Where can I prepare?",
                                    "Use the healing machine, PC, Mart and crafting stations you find in the world. Their state belongs to your canonical Trainer."
                            ),
                            new Option(
                                    "battles",
                                    "How do battles work here?",
                                    "When a battle starts, AutoPTU decides legal actions and outcomes. The Minecraft world only presents the authoritative result."
                            )
                    )
            )
    ));

    private final Map<String, Dialogue> dialogues;

    public CanonicalNpcDialogueCatalogue(List<Dialogue> dialogues) {
        Objects.requireNonNull(dialogues, "dialogues");
        Map<String, Dialogue> indexed = new LinkedHashMap<>();
        for (Dialogue dialogue : dialogues) {
            Objects.requireNonNull(dialogue, "dialogue");
            if (indexed.putIfAbsent(dialogue.npcId(), dialogue) != null) {
                throw new IllegalArgumentException("duplicate npcId: " + dialogue.npcId());
            }
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

    public record Option(String optionId, String label, String response) {
        public Option {
            optionId = requireText(optionId, "optionId");
            label = requireText(label, "label");
            response = requireText(response, "response");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
