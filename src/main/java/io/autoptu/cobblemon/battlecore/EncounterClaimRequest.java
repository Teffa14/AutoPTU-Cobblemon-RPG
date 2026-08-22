package io.autoptu.cobblemon.battlecore;

import java.util.List;

/** Immutable request handed from a world adapter to server-owned canonical encounter resolution. */
public record EncounterClaimRequest(
        String externalBattleId,
        List<EncounterParticipantRef> participants
) {
    public EncounterClaimRequest {
        if (externalBattleId == null || externalBattleId.isBlank()) {
            throw new IllegalArgumentException("externalBattleId is required");
        }
        externalBattleId = externalBattleId.strip();
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("participants are required");
        }
        participants = List.copyOf(participants);
        if (participants.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("participants cannot contain null");
        }
    }
}
