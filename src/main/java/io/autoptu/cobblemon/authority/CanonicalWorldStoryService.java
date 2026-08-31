package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Applies only server-authored world-story choices and their explicit consequence flags. */
public final class CanonicalWorldStoryService {
    private static final int MAX_CAS_ATTEMPTS = 8;
    private final CanonicalWorldStoryCatalogue catalogue;
    private final FileCanonicalWorldStoryRepository repository;

    public CanonicalWorldStoryService(CanonicalWorldStoryCatalogue catalogue, FileCanonicalWorldStoryRepository repository) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId) {
        return snapshot(repository.findOrCreate(playerId));
    }

    public ChoiceResult choose(String playerId, String nodeId, String choiceId) {
        var node = catalogue.node(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical story nodeId: " + nodeId));
        var choice = node.choice(choiceId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical story choiceId: " + choiceId));

        for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
            var current = repository.findOrCreate(playerId);
            String alreadySelected = current.selectedChoices().get(node.nodeId());
            if (alreadySelected != null) {
                if (!alreadySelected.equals(choice.choiceId())) {
                    throw new IllegalStateException("canonical story choice already committed for node: " + node.nodeId());
                }
                return new ChoiceResult(false, snapshot(current));
            }

            Map<String, String> selected = new LinkedHashMap<>(current.selectedChoices());
            selected.put(node.nodeId(), choice.choiceId());
            Set<String> flags = new LinkedHashSet<>(current.storyFlags());
            flags.addAll(choice.consequenceFlags());
            var replacement = new FileCanonicalWorldStoryRepository.StoryState(
                    current.playerId(), selected, flags, current.revision() + 1);
            if (repository.replaceIfRevision(replacement, current.revision())) {
                return new ChoiceResult(true, snapshot(replacement));
            }
        }
        throw new IllegalStateException("canonical world story state changed concurrently; retry request");
    }

    private static Snapshot snapshot(FileCanonicalWorldStoryRepository.StoryState state) {
        return new Snapshot(state.playerId(), state.selectedChoices(), state.storyFlags(), state.revision());
    }

    public record Snapshot(String playerId, Map<String, String> selectedChoices, Set<String> storyFlags, long revision) { }
    public record ChoiceResult(boolean newlyCommitted, Snapshot snapshot) { }
}
