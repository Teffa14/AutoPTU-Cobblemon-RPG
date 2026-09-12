package io.autoptu.cobblemon.authority;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Read-only operator projection of one server-owned canonical Pokemon aggregate. */
public final class CanonicalPokemonAdminInspectionService {
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalPokemonAdminInspectionService(VersionedCanonicalPokemonRepository pokemonRepository) {
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public Optional<Inspection> inspect(String pokemonId) {
        if (pokemonId == null || pokemonId.isBlank()) return Optional.empty();
        return pokemonRepository.findPokemon(pokemonId.strip()).map(CanonicalPokemonAdminInspectionService::project);
    }

    private static Inspection project(CanonicalPokemonState state) {
        return new Inspection(
                state.pokemonId(),
                state.ownerPlayerId(),
                state.speciesId(),
                state.level(),
                state.health(),
                state.statuses().stream().sorted().toList(),
                state.combatStats(),
                state.moveLoadout(),
                state.baseMovement(),
                state.battleTraits(),
                state.accuracyEvasion(),
                state.injuryState(),
                state.heldItemInstanceId(),
                state.capabilities().stream().sorted(Comparator.naturalOrder()).toList(),
                state.revision());
    }

    public record Inspection(
            String pokemonId,
            String ownerPlayerId,
            String speciesId,
            int level,
            CanonicalHealth health,
            List<String> statuses,
            CanonicalCombatStats combatStats,
            CanonicalMoveLoadout moveLoadout,
            CanonicalBaseMovement baseMovement,
            CanonicalBattleTraits battleTraits,
            CanonicalAccuracyEvasion accuracyEvasion,
            CanonicalInjuryState injuryState,
            String heldItemInstanceId,
            List<String> capabilities,
            long revision
    ) {
        public Inspection {
            statuses = List.copyOf(statuses == null ? List.of() : statuses);
            capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        }
    }
}
