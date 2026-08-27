package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Server-owned catalogue of starter species exposed to Minecraft clients. */
public final class CanonicalStarterCatalogue {
    private static final List<StarterOption> CONFIGURED = List.of(
            new StarterOption("bulbasaur", "Bulbasaur"),
            new StarterOption("charmander", "Charmander"),
            new StarterOption("squirtle", "Squirtle")
    );

    public List<StarterOption> configuredStarters() {
        return CONFIGURED;
    }

    public Optional<StarterOption> findConfigured(String requestedSpeciesId) {
        if (requestedSpeciesId == null || requestedSpeciesId.isBlank()) {
            return Optional.empty();
        }
        String normalized = requestedSpeciesId.strip().toLowerCase(Locale.ROOT);
        return CONFIGURED.stream().filter(option -> option.speciesId().equals(normalized)).findFirst();
    }

    public record StarterOption(String speciesId, String displayName) {
        public StarterOption {
            Objects.requireNonNull(speciesId, "speciesId");
            Objects.requireNonNull(displayName, "displayName");
            if (speciesId.isBlank() || displayName.isBlank()) {
                throw new IllegalArgumentException("starter option fields must not be blank");
            }
        }
    }
}
