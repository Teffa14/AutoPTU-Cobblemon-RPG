package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Creates an immutable, multi-side roster reservation from server-owned canonical identities.
 * It does not accept platform entities, platform stats, client ownership claims, trainer state or items.
 */
public final class BattleEncounterRosterReservationService {
    private final CanonicalBattleEncounterRepository canonicalRepository;
    private final BattleEncounterRosterRepository reservationRepository;
    private final Supplier<String> reservationIds;
    private final LongSupplier rngSeeds;

    public BattleEncounterRosterReservationService(
            CanonicalBattleEncounterRepository canonicalRepository,
            BattleEncounterRosterRepository reservationRepository,
            Supplier<String> reservationIds,
            LongSupplier rngSeeds
    ) {
        if (canonicalRepository == null || reservationRepository == null) {
            throw new IllegalArgumentException("repositories must not be null");
        }
        if (reservationIds == null || rngSeeds == null) {
            throw new IllegalArgumentException("server suppliers must not be null");
        }
        this.canonicalRepository = canonicalRepository;
        this.reservationRepository = reservationRepository;
        this.reservationIds = reservationIds;
        this.rngSeeds = rngSeeds;
    }

    public BattleEncounterRosterReservationDecision reserve(List<BattleEncounterParticipantRequest> requests) {
        return reserve(requests, null);
    }

    BattleEncounterRosterReservationDecision reserve(
            List<BattleEncounterParticipantRequest> requests,
            BattleReservationAuthority reservationAuthority
    ) {
        if (requests == null || requests.isEmpty()) {
            return BattleEncounterRosterReservationDecision.deny("invalid_participants");
        }

        Set<String> participantIds = new HashSet<>();
        Set<String> combatantIds = new HashSet<>();
        ArrayList<BattleEncounterParticipantSnapshot> participants = new ArrayList<>();

        try {
            for (BattleEncounterParticipantRequest request : requests) {
                if (request == null) return BattleEncounterRosterReservationDecision.deny("invalid_participant");
                if (!participantIds.add(request.participantId())) {
                    return BattleEncounterRosterReservationDecision.deny("duplicate_participant:" + request.participantId());
                }

                String teamId = "battle-side-" + request.side();
                ArrayList<BattleCombatantAuthoritySnapshot> combatants = new ArrayList<>();
                for (String combatantId : request.combatantIds()) {
                    if (!combatantIds.add(combatantId)) {
                        return BattleEncounterRosterReservationDecision.deny("duplicate_combatant:" + combatantId);
                    }
                    CanonicalBattlePokemonView canonical = canonicalRepository.findCombatant(
                            request.participantKind(), request.participantId(), combatantId).orElse(null);
                    if (canonical == null) {
                        return BattleEncounterRosterReservationDecision.deny("unknown_or_unauthorized_combatant:" + combatantId);
                    }
                    if (!canonical.pokemonId().equals(combatantId)) {
                        return BattleEncounterRosterReservationDecision.deny("canonical_identity_mismatch:" + combatantId);
                    }
                    combatants.add(BattleCombatantAuthoritySnapshot.from(
                            canonical,
                            request.participantId(),
                            teamId,
                            request.participantKind()
                    ));
                }
                participants.add(new BattleEncounterParticipantSnapshot(
                        request.side(),
                        request.participantId(),
                        teamId,
                        request.participantKind(),
                        combatants
                ));
            }

            BattleReservationAuthority authority = reservationAuthority == null
                    ? issueReservationAuthority()
                    : reservationAuthority;
            BattleEncounterRosterReservation reservation = new BattleEncounterRosterReservation(
                    authority.reservationId(),
                    authority.rngSeed(),
                    participants
            );
            if (!reservationRepository.tryReserve(reservation)) {
                return BattleEncounterRosterReservationDecision.deny("state_changed_or_combatants_reserved");
            }
            return BattleEncounterRosterReservationDecision.allow(reservation);
        } catch (IllegalArgumentException ex) {
            return BattleEncounterRosterReservationDecision.deny("invalid_encounter:" + ex.getMessage());
        }
    }

    private BattleReservationAuthority issueReservationAuthority() {
        String reservationId = reservationIds.get();
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalStateException("reservation id supplier returned blank id");
        }
        return new BattleReservationAuthority(reservationId, rngSeeds.getAsLong());
    }

    public BattleEncounterRosterReservationDecision release(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return BattleEncounterRosterReservationDecision.deny("invalid_reservation_id");
        }
        BattleEncounterRosterReservation reservation = reservationRepository.findReservation(reservationId).orElse(null);
        if (reservation == null) {
            return BattleEncounterRosterReservationDecision.deny("unknown_encounter_reservation");
        }
        if (!reservationRepository.release(reservationId)) {
            return BattleEncounterRosterReservationDecision.deny("encounter_reservation_conflict");
        }
        return BattleEncounterRosterReservationDecision.allow(reservation);
    }
}
