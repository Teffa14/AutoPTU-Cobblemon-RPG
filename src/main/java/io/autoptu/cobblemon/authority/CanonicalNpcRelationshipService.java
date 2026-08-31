package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned RPG relationship boundary for authored NPCs.
 *
 * <p>The first physical contact may establish that a Trainer has met an NPC. This service does not
 * infer reputation gains, dialogue unlocks, quest rewards, PTU Features, or battle effects.</p>
 */
public final class CanonicalNpcRelationshipService {
    private static final int MAX_CAS_ATTEMPTS = 4;
    private final CanonicalNpcDialogueCatalogue catalogue;
    private final FileCanonicalNpcRelationshipRepository repository;

    public CanonicalNpcRelationshipService(
            CanonicalNpcDialogueCatalogue catalogue,
            FileCanonicalNpcRelationshipRepository repository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Records a server-observed physical contact with a known authored NPC exactly once. */
    public ContactResult observeContact(String playerId, String npcId) {
        var dialogue = catalogue.dialogue(npcId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical npcId: " + npcId));
        for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
            var current = repository.findOrCreate(playerId, dialogue.npcId());
            if (current.met()) return new ContactResult(false, snapshot(current));
            var replacement = new FileCanonicalNpcRelationshipRepository.RelationshipState(
                    current.playerId(), current.npcId(), true, current.reputation(), current.revision() + 1);
            if (repository.replaceIfRevision(replacement, current.revision())) {
                return new ContactResult(true, snapshot(replacement));
            }
        }
        throw new IllegalStateException("canonical NPC relationship update conflicted repeatedly");
    }

    public Snapshot inspect(String playerId, String npcId) {
        var dialogue = catalogue.dialogue(npcId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical npcId: " + npcId));
        return snapshot(repository.findOrCreate(playerId, dialogue.npcId()));
    }

    private static Snapshot snapshot(FileCanonicalNpcRelationshipRepository.RelationshipState state) {
        return new Snapshot(state.playerId(), state.npcId(), state.met(), state.reputation(), state.revision());
    }

    public record Snapshot(String playerId, String npcId, boolean met, int reputation, long revision) { }
    public record ContactResult(boolean newlyMet, Snapshot relationship) { }
}
