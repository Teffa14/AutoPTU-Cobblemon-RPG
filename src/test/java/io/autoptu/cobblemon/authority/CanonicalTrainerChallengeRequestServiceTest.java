package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalTrainerChallengeRequestServiceTest {
    @Test
    void defaultCatalogueOwnsCedarRangerChallenge() {
        var challenge = CanonicalTrainerChallengeCatalogue.DEFAULT.challenge("cedar-ranger-field-spar").orElseThrow();
        assertEquals("cedar-ranger", challenge.npcId());
        assertEquals("Cedar Ranger Field Spar", challenge.displayName());
    }

    @Test
    void rejectsChallengeRequestedFromDifferentNpcBeforeReadingPlayerState() {
        CanonicalStateRepository players = playerId -> { throw new AssertionError("player repository should not be read"); };
        VersionedCanonicalPlayerEncounterProfileRepository parties = unusedPartyRepository();
        var service = new CanonicalTrainerChallengeRequestService(CanonicalTrainerChallengeCatalogue.DEFAULT, players, parties);

        var decision = service.request("player-one", "other-npc", "cedar-ranger-field-spar");

        assertFalse(decision.accepted());
        assertEquals("challenge does not belong to this NPC", decision.detail());
    }

    @Test
    void rejectsDuplicateAuthoredChallengeIds() {
        var challenge = new CanonicalTrainerChallengeCatalogue.Challenge("same", "npc", "One", "Request");
        assertThrows(IllegalArgumentException.class, () -> new CanonicalTrainerChallengeCatalogue(java.util.List.of(challenge, challenge)));
    }

    private static VersionedCanonicalPlayerEncounterProfileRepository unusedPartyRepository() {
        return new VersionedCanonicalPlayerEncounterProfileRepository() {
            @Override public Optional<CanonicalPlayerEncounterProfile> findProfile(String playerId) { throw new AssertionError("party repository should not be read"); }
            @Override public boolean createProfileIfAbsent(CanonicalPlayerEncounterProfile initialProfile) { throw new UnsupportedOperationException(); }
            @Override public boolean replaceProfileIfRevision(String playerId, long expectedRevision, CanonicalPlayerEncounterProfile replacement) { throw new UnsupportedOperationException(); }
        };
    }
}
