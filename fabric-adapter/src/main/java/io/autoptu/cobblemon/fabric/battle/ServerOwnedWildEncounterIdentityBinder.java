package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Binds opaque Cobblemon WILD identities to a server-owned canonical roster that was provisioned
 * before the battle-start claim. Cobblemon contributes identity/correlation keys only.
 */
public final class ServerOwnedWildEncounterIdentityBinder {
    public record CanonicalWildRoster(
            String canonicalParticipantId,
            List<String> canonicalPokemonIds
    ) {
        public CanonicalWildRoster {
            canonicalParticipantId = requireId(canonicalParticipantId, "canonicalParticipantId");
            if (canonicalPokemonIds == null || canonicalPokemonIds.isEmpty()) {
                throw new IllegalArgumentException("canonicalPokemonIds must not be empty");
            }
            canonicalPokemonIds = canonicalPokemonIds.stream()
                    .map(id -> requireId(id, "canonicalPokemonId"))
                    .toList();
            if (canonicalPokemonIds.stream().distinct().count() != canonicalPokemonIds.size()) {
                throw new IllegalArgumentException("canonicalPokemonIds must be unique");
            }
        }
    }

    @FunctionalInterface
    public interface CanonicalWildRosterSource {
        Optional<CanonicalWildRoster> resolve(
                String cobblemonBattleId,
                int side,
                String externalWildActorId
        );
    }

    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final CanonicalWildRosterSource rosterSource;

    public ServerOwnedWildEncounterIdentityBinder(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            CanonicalWildRosterSource rosterSource
    ) {
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
        this.rosterSource = Objects.requireNonNull(rosterSource, "rosterSource");
    }

    public boolean bind(
            String cobblemonBattleId,
            CobblemonBattleStartInterceptor.ParticipantIdentity externalWild
    ) {
        if (cobblemonBattleId == null || cobblemonBattleId.isBlank() || externalWild == null) return false;
        if (externalWild.kind() != CobblemonBattleStartInterceptor.ParticipantKind.WILD) return false;

        Optional<CanonicalWildRoster> resolved = rosterSource.resolve(
                cobblemonBattleId.strip(),
                externalWild.side(),
                externalWild.actorId()
        );
        if (resolved.isEmpty()) return false;

        CanonicalWildRoster canonical = resolved.get();
        if (canonical.canonicalPokemonIds().size() != externalWild.pokemonIds().size()) return false;

        Map<String, String> mappings = new LinkedHashMap<>();
        for (int index = 0; index < externalWild.pokemonIds().size(); index++) {
            mappings.put(externalWild.pokemonIds().get(index), canonical.canonicalPokemonIds().get(index));
        }

        try {
            identityRegistry.registerOrReplace(
                    CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                    externalWild.actorId(),
                    canonical.canonicalParticipantId(),
                    mappings
            );
            return true;
        } catch (IllegalArgumentException | IllegalStateException rejected) {
            return false;
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
