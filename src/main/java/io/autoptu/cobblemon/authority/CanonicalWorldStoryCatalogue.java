package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Server-authored world story nodes and explicit consequence flags. */
public final class CanonicalWorldStoryCatalogue {
    public static final CanonicalWorldStoryCatalogue DEFAULT = new CanonicalWorldStoryCatalogue(List.of(
            new StoryNode(
                    "cedar-meadow-approach",
                    "Cedar Meadow approach",
                    List.of(
                            new Choice("observe-first", "Observe before approaching", Set.of("cedar_meadow_observe_first")),
                            new Choice("engage-directly", "Approach the meadow directly", Set.of("cedar_meadow_engage_directly"))
                    )
            )
    ));

    private final Map<String, StoryNode> nodes;

    public CanonicalWorldStoryCatalogue(List<StoryNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        Map<String, StoryNode> indexed = new LinkedHashMap<>();
        for (StoryNode node : nodes) {
            Objects.requireNonNull(node, "node");
            if (indexed.putIfAbsent(node.nodeId(), node) != null) {
                throw new IllegalArgumentException("duplicate story nodeId: " + node.nodeId());
            }
        }
        this.nodes = Map.copyOf(indexed);
    }

    public Optional<StoryNode> node(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return Optional.empty();
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public record StoryNode(String nodeId, String displayName, List<Choice> choices) {
        public StoryNode {
            nodeId = requireId(nodeId, "nodeId");
            displayName = requireId(displayName, "displayName");
            choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
            if (choices.isEmpty()) throw new IllegalArgumentException("story node choices cannot be empty");
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            for (Choice choice : choices) {
                Objects.requireNonNull(choice, "choice");
                if (!ids.add(choice.choiceId())) throw new IllegalArgumentException("duplicate story choiceId: " + choice.choiceId());
            }
        }

        public Optional<Choice> choice(String choiceId) {
            if (choiceId == null || choiceId.isBlank()) return Optional.empty();
            return choices.stream().filter(choice -> choice.choiceId().equals(choiceId)).findFirst();
        }
    }

    public record Choice(String choiceId, String label, Set<String> consequenceFlags) {
        public Choice {
            choiceId = requireId(choiceId, "choiceId");
            label = requireId(label, "label");
            consequenceFlags = Set.copyOf(Objects.requireNonNull(consequenceFlags, "consequenceFlags"));
            if (consequenceFlags.isEmpty()) throw new IllegalArgumentException("story choice must have an authored consequence flag");
            for (String flag : consequenceFlags) requireId(flag, "consequenceFlag");
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
