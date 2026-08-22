package io.autoptu.cobblemon.authority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BattleEncounterParticipantSnapshot(
        int side,
        String participantId,
        String teamId,
        BattleParticipantKind participantKind,
        List<BattleCombatantAuthoritySnapshot> combatants
) {
    public BattleEncounterParticipantSnapshot {
        if (side != 1 && side != 2) throw new IllegalArgumentException("side must be 1 or 2");
        participantId = requireId(participantId, "participantId");
        teamId = requireId(teamId, "teamId");
        if (participantKind == null) throw new IllegalArgumentException("participantKind is required");
        combatants = combatants == null ? List.of() : List.copyOf(combatants);
        if (combatants.isEmpty()) throw new IllegalArgumentException("combatants must not be empty");

        Set<String> ids = new HashSet<>();
        for (BattleCombatantAuthoritySnapshot combatant : combatants) {
            if (combatant == null) throw new IllegalArgumentException("combatant must not be null");
            if (!combatant.participantId().equals(participantId)
                    || !combatant.teamId().equals(teamId)
                    || combatant.participantKind() != participantKind) {
                throw new IllegalArgumentException("combatant affiliation must match participant snapshot");
            }
            if (!ids.add(combatant.combatantId())) {
                throw new IllegalArgumentException("duplicate combatant in participant snapshot");
            }
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
