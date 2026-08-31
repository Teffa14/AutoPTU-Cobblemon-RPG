package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authored Gym/League registration catalogue. Registration is RPG state, not battle legality. */
public final class CanonicalLeagueChallengeCatalogue {
    public static final CanonicalLeagueChallengeCatalogue DEFAULT = new CanonicalLeagueChallengeCatalogue(List.of(
            new Challenge("cedar-gym-entry", Kind.GYM, "Cedar Gym Entry", "Register for the Cedar Gym challenge circuit."),
            new Challenge("ouros-league-open", Kind.LEAGUE, "Ouros League Open", "Register for the Ouros League open circuit.")
    ));

    private final Map<String, Challenge> challenges;

    public CanonicalLeagueChallengeCatalogue(List<Challenge> authoredChallenges) {
        LinkedHashMap<String, Challenge> copy = new LinkedHashMap<>();
        for (Challenge challenge : authoredChallenges == null ? List.<Challenge>of() : authoredChallenges) {
            Challenge previous = copy.put(challenge.challengeId(), challenge);
            if (previous != null) throw new IllegalArgumentException("duplicate league challenge id " + challenge.challengeId());
        }
        challenges = Map.copyOf(copy);
    }

    public Optional<Challenge> challenge(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) return Optional.empty();
        return Optional.ofNullable(challenges.get(challengeId.strip()));
    }

    public List<Challenge> challenges() {
        return List.copyOf(challenges.values());
    }

    public enum Kind { GYM, LEAGUE }

    public record Challenge(String challengeId, Kind kind, String displayName, String description) {
        public Challenge {
            challengeId = requireText(challengeId, "challengeId");
            if (kind == null) throw new IllegalArgumentException("kind is required");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
