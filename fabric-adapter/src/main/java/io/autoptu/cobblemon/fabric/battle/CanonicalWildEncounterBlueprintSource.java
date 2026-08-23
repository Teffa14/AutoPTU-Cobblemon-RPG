package io.autoptu.cobblemon.fabric.battle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-owned source for already-decided canonical WILD encounter blueprints.
 *
 * Implementations may read RPG/encounter state, but this boundary accepts only the canonical encounter
 * identifier. Cobblemon actor/Pokemon UUIDs and entity state are intentionally absent so presentation
 * data cannot influence species, level, stats, HP, moves, abilities, items or other PTU values.
 */
@FunctionalInterface
public interface CanonicalWildEncounterBlueprintSource {
    record CanonicalWildEncounterBlueprint(
            String canonicalEncounterId,
            int side,
            List<ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint> pokemon
    ) {
        public CanonicalWildEncounterBlueprint {
            if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) {
                throw new IllegalArgumentException("canonicalEncounterId is required");
            }
            canonicalEncounterId = canonicalEncounterId.strip();
            if (side < 0) throw new IllegalArgumentException("side must be >= 0");
            if (pokemon == null || pokemon.isEmpty()) {
                throw new IllegalArgumentException("pokemon must not be empty");
            }
            if (pokemon.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("pokemon must not contain null entries");
            }
            pokemon = List.copyOf(pokemon);
        }
    }

    Optional<CanonicalWildEncounterBlueprint> resolve(String canonicalEncounterId);
}
