package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-owned catalogue for world Trainer challenge identities. It contains no PTU battle rules. */
public final class CanonicalTrainerChallengeCatalogue {
    public static final CanonicalTrainerChallengeCatalogue DEFAULT = new CanonicalTrainerChallengeCatalogue(List.of(
            new Challenge(
                    "cedar-ranger-field-spar",
                    "cedar-ranger",
                    "Cedar Ranger Field Spar",
                    "Challenge Cedar Ranger to a field spar. AutoPTU must authorize and resolve any battle that follows."
            ),
            new Challenge(
                    "cedar-gym-trial-registration",
                    "cedar-league-desk",
                    "Cedar Gym Trial",
                    "Register for the Cedar Gym trial. AutoPTU must authorize the opponent, battle start and every battle outcome."
            )
    ));

    private final Map<String, Challenge> challenges;

    public CanonicalTrainerChallengeCatalogue(List<Challenge> challenges) {
        Objects.requireNonNull(challenges, "challenges");
        Map<String, Challenge> indexed = new LinkedHashMap<>();
        for (Challenge challenge : challenges) {
            Objects.requireNonNull(challenge, "challenge");
            if (indexed.putIfAbsent(challenge.challengeId(), challenge) != null) {
                throw new IllegalArgumentException("duplicate challengeId: " + challenge.challengeId());
            }
        }
        this.challenges = Map.copyOf(indexed);
    }

    public Optional<Challenge> challenge(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) return Optional.empty();
        return Optional.ofNullable(challenges.get(challengeId.strip()));
    }

    public record Challenge(String challengeId, String npcId, String displayName, String requestText) {
        public Challenge {
            challengeId = requireText(challengeId, "challengeId");
            npcId = requireText(npcId, "npcId");
            displayName = requireText(displayName, "displayName");
            requestText = requireText(requestText, "requestText");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
