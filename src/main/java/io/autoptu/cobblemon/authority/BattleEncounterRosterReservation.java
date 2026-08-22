package io.autoptu.cobblemon.authority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable reservation of canonical combatant rosters on both encounter sides.
 * Trainer/item authority composes separately and is not implied by this roster-only reservation.
 */
public record BattleEncounterRosterReservation(
        String reservationId,
        long rngSeed,
        List<BattleEncounterParticipantSnapshot> participants
) {
    public BattleEncounterRosterReservation {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        participants = participants == null ? List.of() : List.copyOf(participants);
        if (participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");

        boolean side1 = false;
        boolean side2 = false;
        Set<String> participantIds = new HashSet<>();
        Set<String> combatantIds = new HashSet<>();
        Set<String> side1Teams = new HashSet<>();
        Set<String> side2Teams = new HashSet<>();

        for (BattleEncounterParticipantSnapshot participant : participants) {
            if (participant == null) throw new IllegalArgumentException("participant must not be null");
            if (!participantIds.add(participant.participantId())) {
                throw new IllegalArgumentException("participant cannot appear more than once in encounter");
            }
            if (participant.side() == 1) {
                side1 = true;
                side1Teams.add(participant.teamId());
            } else {
                side2 = true;
                side2Teams.add(participant.teamId());
            }
            for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                if (!combatantIds.add(combatant.combatantId())) {
                    throw new IllegalArgumentException("combatant cannot appear on multiple encounter participants");
                }
            }
        }
        if (!side1 || !side2) throw new IllegalArgumentException("encounter must contain both sides");
        Set<String> overlappingTeams = new HashSet<>(side1Teams);
        overlappingTeams.retainAll(side2Teams);
        if (!overlappingTeams.isEmpty()) {
            throw new IllegalArgumentException("opposing sides must not share battle team identity");
        }
    }
}
