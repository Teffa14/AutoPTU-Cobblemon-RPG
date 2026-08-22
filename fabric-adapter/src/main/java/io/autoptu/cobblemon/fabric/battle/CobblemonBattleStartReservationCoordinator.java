package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservation;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservationDecision;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservationService;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Converts an intercepted Cobblemon identity-only signal into one canonical multi-side roster
 * reservation. Cobblemon cancellation should occur only when this coordinator returns a claim.
 */
public final class CobblemonBattleStartReservationCoordinator {
    public record ClaimAttempt(
            boolean claimed,
            String rejectionCode,
            BattleEncounterRosterReservation reservation
    ) {
        public ClaimAttempt {
            if (claimed) {
                if (reservation == null) throw new IllegalArgumentException("claimed attempt requires reservation");
                rejectionCode = null;
            } else {
                if (reservation != null) throw new IllegalArgumentException("rejected attempt cannot contain reservation");
                if (rejectionCode == null || rejectionCode.isBlank()) {
                    throw new IllegalArgumentException("rejected attempt requires rejectionCode");
                }
                rejectionCode = rejectionCode.strip();
            }
        }

        public static ClaimAttempt claimed(BattleEncounterRosterReservation reservation) {
            return new ClaimAttempt(true, null, reservation);
        }

        public static ClaimAttempt rejected(String code) {
            return new ClaimAttempt(false, code, null);
        }
    }

    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final BattleEncounterRosterReservationService reservationService;

    public CobblemonBattleStartReservationCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            BattleEncounterRosterReservationService reservationService
    ) {
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
    }

    public ClaimAttempt tryReserve(CobblemonBattleStartInterceptor.BattleStartSignal signal) {
        Objects.requireNonNull(signal, "signal");
        ArrayList<BattleEncounterParticipantRequest> requests = new ArrayList<>();
        for (CobblemonBattleStartInterceptor.ParticipantIdentity participant : signal.participants()) {
            BattleEncounterParticipantRequest resolved = identityRegistry.resolve(participant).orElse(null);
            if (resolved == null) {
                return ClaimAttempt.rejected("unresolved_participant:" + participant.actorId());
            }
            requests.add(resolved);
        }

        BattleEncounterRosterReservationDecision decision = reservationService.reserve(requests);
        if (!decision.allowed()) {
            return ClaimAttempt.rejected(decision.reason());
        }
        return ClaimAttempt.claimed(decision.reservation());
    }
}
