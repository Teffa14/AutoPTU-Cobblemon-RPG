package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authored registration metadata layered over canonical Trainer challenge identities. */
public final class CanonicalLeagueChallengeCatalogue {
    public static final CanonicalLeagueChallengeCatalogue DEFAULT = new CanonicalLeagueChallengeCatalogue(List.of(
            new RegistrationDefinition("cedar-gym-trial-registration", Kind.GYM)
    ));

    private final Map<String, RegistrationDefinition> registrations;

    public CanonicalLeagueChallengeCatalogue(List<RegistrationDefinition> authoredRegistrations) {
        LinkedHashMap<String, RegistrationDefinition> copy = new LinkedHashMap<>();
        for (RegistrationDefinition registration : authoredRegistrations == null ? List.<RegistrationDefinition>of() : authoredRegistrations) {
            RegistrationDefinition previous = copy.put(registration.challengeId(), registration);
            if (previous != null) throw new IllegalArgumentException("duplicate league registration challenge id " + registration.challengeId());
        }
        registrations = Map.copyOf(copy);
    }

    public Optional<RegistrationDefinition> registration(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) return Optional.empty();
        return Optional.ofNullable(registrations.get(challengeId.strip()));
    }

    public List<RegistrationDefinition> registrations() {
        return List.copyOf(registrations.values());
    }

    public enum Kind { GYM, LEAGUE }

    public record RegistrationDefinition(String challengeId, Kind kind) {
        public RegistrationDefinition {
            challengeId = requireText(challengeId, "challengeId");
            if (kind == null) throw new IllegalArgumentException("kind is required");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
