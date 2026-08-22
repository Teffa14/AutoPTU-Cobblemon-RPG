package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Adapter-neutral identity supplied by a world integration before canonical battle resolution.
 *
 * Only opaque identifiers cross this boundary. No Pokemon stats, HP, moves, abilities, items,
 * legality or outcomes may be inferred from these values.
 */
public record EncounterParticipantRef(
        Kind kind,
        String presentationEntityId,
        String ownerId
) {
    public enum Kind {
        POKEMON,
        TRAINER
    }

    public EncounterParticipantRef {
        kind = Objects.requireNonNull(kind, "kind");
        presentationEntityId = requireToken(presentationEntityId, "presentationEntityId");
        ownerId = normalizeOptional(ownerId);
    }

    public static EncounterParticipantRef pokemon(String presentationEntityId, String ownerId) {
        return new EncounterParticipantRef(Kind.POKEMON, presentationEntityId, ownerId);
    }

    public static EncounterParticipantRef trainer(String presentationEntityId, String ownerId) {
        return new EncounterParticipantRef(Kind.TRAINER, presentationEntityId, ownerId);
    }

    private static String requireToken(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
