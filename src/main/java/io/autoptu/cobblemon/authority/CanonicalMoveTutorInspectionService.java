package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only server authority for move tutor/relearner surfaces.
 *
 * This boundary may inspect an owned canonical party Pokemon and its already-persisted move
 * loadout. It deliberately does not calculate learnability, forgotten moves, tutor eligibility,
 * costs or move mutations; those require an authoritative upstream PTU contract.
 */
public final class CanonicalMoveTutorInspectionService {
    private final CanonicalPokemonDetailService pokemonDetailService;

    public CanonicalMoveTutorInspectionService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.pokemonDetailService = new CanonicalPokemonDetailService(
                Objects.requireNonNull(partyRepository, "partyRepository"),
                Objects.requireNonNull(pokemonRepository, "pokemonRepository")
        );
    }

    public Optional<Inspection> inspect(String authenticatedPlayerId, int partySlot) {
        return pokemonDetailService.findPokemon(authenticatedPlayerId, partySlot)
                .map(detail -> new Inspection(
                        detail.slot(),
                        detail.pokemonId(),
                        detail.speciesId(),
                        detail.level(),
                        detail.moveLoadout() == null ? List.of() : detail.moveLoadout().moveIds(),
                        detail.moveLoadout() != null,
                        detail.revision()
                ));
    }

    public record Inspection(
            int partySlot,
            String pokemonId,
            String speciesId,
            int level,
            List<String> currentMoveIds,
            boolean moveLoadoutAvailable,
            long pokemonRevision
    ) {
        public Inspection {
            currentMoveIds = List.copyOf(Objects.requireNonNull(currentMoveIds, "currentMoveIds"));
        }
    }
}
