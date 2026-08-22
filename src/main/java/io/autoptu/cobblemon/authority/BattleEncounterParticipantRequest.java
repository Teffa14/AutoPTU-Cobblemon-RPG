package io.autoptu.cobblemon.authority;

import java.util.LinkedHashSet;
import java.util.List;

/** Canonical identity request used after any platform UUIDs have been resolved server-side. */
public record BattleEncounterParticipantRequest(
        int side,
        String participantId,
        BattleParticipantKind participantKind,
        List<String> combatantIds
) {
    public BattleEncounterParticipantRequest {
        if (side != 1 && side != 2) throw new IllegalArgumentException("side must be 1 or 2");
        participantId = requireId(participantId, "participantId");
        if (participantKind == null) throw new IllegalArgumentException("participantKind is required");
        if (combatantIds == null || combatantIds.isEmpty()) {
            throw new IllegalArgumentException("combatantIds must not be empty");
        }
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String combatantId : combatantIds) {
            if (!copy.add(requireId(combatantId, "combatantId"))) {
                throw new IllegalArgumentException("duplicate combatantId in participant request");
            }
        }
        combatantIds = List.copyOf(copy);
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
