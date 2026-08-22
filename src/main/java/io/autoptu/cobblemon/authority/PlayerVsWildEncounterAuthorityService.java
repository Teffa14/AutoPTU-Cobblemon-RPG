package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Composes the existing player asset reservation with the owner-neutral encounter roster reservation.
 *
 * This service accepts canonical IDs only. Cobblemon/Minecraft identities must already have been mapped
 * to canonical IDs before this boundary is called.
 */
public final class PlayerVsWildEncounterAuthorityService {
    private final BattleAuthorityService playerAuthority;
    private final BattleEncounterRosterReservationService encounterAuthority;
    private final Supplier<String> reservationIds;
    private final LongSupplier rngSeeds;

    public PlayerVsWildEncounterAuthorityService(
            BattleAuthorityService playerAuthority,
            BattleEncounterRosterReservationService encounterAuthority,
            Supplier<String> reservationIds,
            LongSupplier rngSeeds
    ) {
        if (playerAuthority == null || encounterAuthority == null) {
            throw new IllegalArgumentException("authority services must not be null");
        }
        if (reservationIds == null || rngSeeds == null) {
            throw new IllegalArgumentException("server suppliers must not be null");
        }
        this.playerAuthority = playerAuthority;
        this.encounterAuthority = encounterAuthority;
        this.reservationIds = reservationIds;
        this.rngSeeds = rngSeeds;
    }

    public PlayerVsWildBattleReservationDecision reserve(
            String playerId,
            List<String> playerPokemonIds,
            Map<String, Integer> consumableQuantities,
            BattleArenaSnapshot arena,
            List<BattleEncounterParticipantRequest> participants
    ) {
        String topologyError = validateTopology(playerId, playerPokemonIds, arena, participants);
        if (topologyError != null) {
            return PlayerVsWildBattleReservationDecision.deny(topologyError);
        }

        BattleReservationAuthority sharedAuthority = issueReservationAuthority();
        BattleSnapshotDecision playerDecision = playerAuthority.reserveBattleInArena(
                playerId,
                playerPokemonIds,
                consumableQuantities,
                arena,
                sharedAuthority
        );
        if (!playerDecision.allowed()) {
            return PlayerVsWildBattleReservationDecision.deny("player_authority:" + playerDecision.reason());
        }

        BattleEncounterRosterReservationDecision encounterDecision = encounterAuthority.reserve(
                participants,
                sharedAuthority
        );
        if (!encounterDecision.allowed()) {
            BattleSnapshotDecision released = playerAuthority.releaseBattle(playerId, sharedAuthority.reservationId());
            if (!released.allowed()) {
                throw new IllegalStateException(
                        "failed to compensate player reservation after encounter denial: " + released.reason());
            }
            return PlayerVsWildBattleReservationDecision.deny("encounter_authority:" + encounterDecision.reason());
        }

        try {
            return PlayerVsWildBattleReservationDecision.allow(new PlayerVsWildBattleReservation(
                    playerDecision.snapshot(),
                    encounterDecision.reservation()
            ));
        } catch (IllegalArgumentException invalidComposition) {
            BattleEncounterRosterReservationDecision encounterRelease = encounterAuthority.release(
                    sharedAuthority.reservationId());
            BattleSnapshotDecision playerRelease = playerAuthority.releaseBattle(
                    playerId,
                    sharedAuthority.reservationId());
            if (!encounterRelease.allowed() || !playerRelease.allowed()) {
                throw new IllegalStateException("failed to compensate invalid composed reservation", invalidComposition);
            }
            return PlayerVsWildBattleReservationDecision.deny(
                    "invalid_composition:" + invalidComposition.getMessage());
        }
    }

    private String validateTopology(
            String playerId,
            List<String> playerPokemonIds,
            BattleArenaSnapshot arena,
            List<BattleEncounterParticipantRequest> participants
    ) {
        if (playerId == null || playerId.isBlank()
                || playerPokemonIds == null || playerPokemonIds.isEmpty()) {
            return "invalid_request";
        }
        if (arena == null) {
            return "invalid_battle_arena";
        }
        if (participants == null || participants.size() != 2) {
            return "unsupported_player_vs_wild_topology";
        }

        BattleEncounterParticipantRequest playerParticipant = null;
        BattleEncounterParticipantRequest wildParticipant = null;
        for (BattleEncounterParticipantRequest participant : participants) {
            if (participant == null) return "invalid_participant";
            if (participant.participantKind() == BattleParticipantKind.PLAYER) {
                if (playerParticipant != null) return "unsupported_player_vs_wild_topology";
                playerParticipant = participant;
            } else if (participant.participantKind() == BattleParticipantKind.WILD) {
                if (wildParticipant != null) return "unsupported_player_vs_wild_topology";
                wildParticipant = participant;
            } else {
                return "unsupported_player_vs_wild_topology";
            }
        }
        if (playerParticipant == null || wildParticipant == null) {
            return "unsupported_player_vs_wild_topology";
        }
        if (!playerId.equals(playerParticipant.participantId())) {
            return "player_participant_mismatch";
        }
        if (!List.copyOf(playerPokemonIds).equals(playerParticipant.combatantIds())) {
            return "player_roster_mismatch";
        }
        if (playerParticipant.side() == wildParticipant.side()) {
            return "participants_must_be_opposed";
        }
        return null;
    }

    private BattleReservationAuthority issueReservationAuthority() {
        String reservationId = reservationIds.get();
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException("reservation id supplier returned blank id");
        }
        return new BattleReservationAuthority(reservationId, rngSeeds.getAsLong());
    }
}
