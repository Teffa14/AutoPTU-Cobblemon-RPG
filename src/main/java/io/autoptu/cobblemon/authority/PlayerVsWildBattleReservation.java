package io.autoptu.cobblemon.authority;

import java.util.List;

/**
 * One server-authoritative player-versus-wild reservation spanning player assets and encounter affiliation.
 */
public record PlayerVsWildBattleReservation(
        BattleAuthoritySnapshot playerAuthority,
        BattleEncounterRosterReservation encounterAuthority
) {
    public PlayerVsWildBattleReservation {
        if (playerAuthority == null || encounterAuthority == null) {
            throw new IllegalArgumentException("both authority reservations are required");
        }
        if (!playerAuthority.reservationId().equals(encounterAuthority.reservationId())) {
            throw new IllegalArgumentException("reservation identities must match");
        }
        if (playerAuthority.rngSeed() != encounterAuthority.rngSeed()) {
            throw new IllegalArgumentException("reservation RNG seeds must match");
        }

        BattleEncounterParticipantSnapshot playerParticipant = encounterAuthority.participants().stream()
                .filter(participant -> participant.participantKind() == BattleParticipantKind.PLAYER)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("encounter must include a player participant"));
        if (!playerAuthority.playerId().equals(playerParticipant.participantId())) {
            throw new IllegalArgumentException("player participant must match player authority");
        }

        List<String> playerAssetRoster = playerAuthority.roster().stream()
                .map(BattlePokemonSnapshot::pokemonId)
                .toList();
        List<String> encounterPlayerRoster = playerParticipant.combatants().stream()
                .map(BattleCombatantAuthoritySnapshot::combatantId)
                .toList();
        if (!playerAssetRoster.equals(encounterPlayerRoster)) {
            throw new IllegalArgumentException("player rosters must match exactly");
        }
    }

    public String reservationId() {
        return playerAuthority.reservationId();
    }

    public long rngSeed() {
        return playerAuthority.rngSeed();
    }
}
