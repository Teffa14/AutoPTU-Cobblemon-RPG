package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncounterClaimBoundaryTest {
    @Test
    void participantIdentityIsOpaqueAndNormalized() {
        EncounterParticipantRef ref = EncounterParticipantRef.pokemon("  entity-42  ", "  player-7  ");

        assertEquals(EncounterParticipantRef.Kind.POKEMON, ref.kind());
        assertEquals("entity-42", ref.presentationEntityId());
        assertEquals("player-7", ref.ownerId());
    }

    @Test
    void claimRequestDefensivelyCopiesParticipants() {
        List<EncounterParticipantRef> mutable = new ArrayList<>();
        mutable.add(EncounterParticipantRef.pokemon("pokemon-a", null));

        EncounterClaimRequest request = new EncounterClaimRequest(" battle-a ", mutable);
        mutable.add(EncounterParticipantRef.pokemon("pokemon-b", null));

        assertEquals("battle-a", request.externalBattleId());
        assertEquals(1, request.participants().size());
        assertThrows(UnsupportedOperationException.class,
                () -> request.participants().add(EncounterParticipantRef.pokemon("pokemon-c", null)));
    }

    @Test
    void claimResultCannotPretendAReservationExistsAfterRejection() {
        CanonicalEncounterClaimService.ClaimResult rejected =
                CanonicalEncounterClaimService.ClaimResult.rejected("unresolved_participant");

        assertFalse(rejected.claimed());
        assertNull(rejected.reservationId());
        assertEquals("unresolved_participant", rejected.rejectionCode());
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalEncounterClaimService.ClaimResult(false, "fake", null));
    }

    @Test
    void claimedResultRequiresServerReservationId() {
        CanonicalEncounterClaimService.ClaimResult claimed =
                CanonicalEncounterClaimService.ClaimResult.claimed(" reservation-99 ");

        assertTrue(claimed.claimed());
        assertEquals("reservation-99", claimed.reservationId());
        assertNull(claimed.rejectionCode());
        assertThrows(IllegalArgumentException.class,
                () -> CanonicalEncounterClaimService.ClaimResult.claimed("  "));
    }
}
