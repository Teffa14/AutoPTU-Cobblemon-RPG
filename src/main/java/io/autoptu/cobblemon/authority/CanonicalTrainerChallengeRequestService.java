package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Validates a physical Trainer challenge request from durable RPG state without starting a PTU battle. */
public final class CanonicalTrainerChallengeRequestService {
    private final CanonicalTrainerChallengeCatalogue catalogue;
    private final CanonicalStateRepository playerRepository;
    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;

    public CanonicalTrainerChallengeRequestService(
            CanonicalTrainerChallengeCatalogue catalogue,
            CanonicalStateRepository playerRepository,
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
    }

    public Decision request(String authenticatedPlayerId, String npcId, String challengeId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()
                || npcId == null || npcId.isBlank()
                || challengeId == null || challengeId.isBlank()) {
            return Decision.rejected("invalid challenge request");
        }
        String playerId = authenticatedPlayerId.strip();
        String requestedNpcId = npcId.strip();
        var challenge = catalogue.challenge(challengeId).orElse(null);
        if (challenge == null) return Decision.rejected("unknown authored challenge");
        if (!challenge.npcId().equals(requestedNpcId)) return Decision.rejected("challenge does not belong to this NPC");
        if (playerRepository.findPlayer(playerId).isEmpty()) return Decision.rejected("canonical Trainer state is not loaded");
        var profile = partyRepository.findProfile(playerId).orElse(null);
        if (profile == null || !profile.playerId().equals(playerId) || profile.pokemonIds().isEmpty()) {
            return Decision.rejected("a canonical party is required before challenging a Trainer");
        }
        return Decision.accepted(challenge, new TrainerChallengeRequest(
                playerId,
                challenge.challengeId(),
                challenge.npcId(),
                profile.pokemonIds(),
                profile.revision()
        ));
    }

    public record Decision(boolean accepted, String detail, CanonicalTrainerChallengeCatalogue.Challenge challenge, TrainerChallengeRequest request) {
        public Decision {
            detail = detail == null ? "" : detail;
        }

        static Decision accepted(CanonicalTrainerChallengeCatalogue.Challenge challenge, TrainerChallengeRequest request) {
            return new Decision(true, "challenge request accepted", challenge, request);
        }

        static Decision rejected(String detail) {
            return new Decision(false, detail, null, null);
        }
    }
}
