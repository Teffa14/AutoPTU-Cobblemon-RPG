package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Creates a player's first canonical Pokemon and persistent encounter-party binding exactly once. */
public final class CanonicalStarterSelectionService {
    private static final Set<String> STARTERS = Set.of("bulbasaur", "charmander", "squirtle");

    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalStarterSelectionService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public CanonicalStarterSelectionDecision choose(
            String authenticatedPlayerId,
            String requestedSpeciesId,
            BattleArenaSnapshot serverOwnedArena
    ) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank() || serverOwnedArena == null) {
            return decision(CanonicalStarterSelectionDecision.Outcome.INVALID_REQUEST, "", "", "player and arena are required");
        }
        String playerId = authenticatedPlayerId.strip();
        String speciesId = requestedSpeciesId == null ? "" : requestedSpeciesId.strip().toLowerCase(Locale.ROOT);
        if (!STARTERS.contains(speciesId)) {
            return decision(CanonicalStarterSelectionDecision.Outcome.INVALID_STARTER, "", speciesId,
                    "starter must be bulbasaur, charmander, or squirtle");
        }

        Optional<CanonicalPlayerEncounterProfile> existingParty = partyRepository.findProfile(playerId);
        if (existingParty.isPresent()) {
            CanonicalPlayerEncounterProfile profile = existingParty.get();
            String existingPokemonId = profile.pokemonIds().isEmpty() ? "" : profile.pokemonIds().get(0);
            String existingSpecies = pokemonRepository.findPokemon(existingPokemonId)
                    .map(CanonicalPokemonState::speciesId)
                    .orElse("");
            return decision(CanonicalStarterSelectionDecision.Outcome.ALREADY_CHOSEN,
                    existingPokemonId, existingSpecies, "player already has a canonical party");
        }

        String pokemonId = playerId + ":starter";
        CanonicalPokemonState proposed = new CanonicalPokemonState(
                pokemonId,
                playerId,
                speciesId,
                5,
                Set.of(),
                0L
        );

        pokemonRepository.createPokemonIfAbsent(proposed);
        CanonicalPokemonState persisted = pokemonRepository.findPokemon(pokemonId).orElse(null);
        if (persisted == null
                || !persisted.ownerPlayerId().equals(playerId)
                || !persisted.speciesId().equals(speciesId)) {
            return decision(CanonicalStarterSelectionDecision.Outcome.CONFLICT, pokemonId, speciesId,
                    "canonical starter identity already belongs to a different selection");
        }

        CanonicalPlayerEncounterProfile profile = new CanonicalPlayerEncounterProfile(
                playerId,
                List.of(pokemonId),
                Map.of(),
                serverOwnedArena,
                0L
        );
        if (!partyRepository.createProfileIfAbsent(profile)) {
            Optional<CanonicalPlayerEncounterProfile> winner = partyRepository.findProfile(playerId);
            if (winner.isPresent() && winner.get().pokemonIds().contains(pokemonId)) {
                return decision(CanonicalStarterSelectionDecision.Outcome.CHOSEN, pokemonId, speciesId, "");
            }
            return decision(CanonicalStarterSelectionDecision.Outcome.CONFLICT, pokemonId, speciesId,
                    "another canonical party write won the starter race");
        }
        return decision(CanonicalStarterSelectionDecision.Outcome.CHOSEN, pokemonId, speciesId, "");
    }

    private static CanonicalStarterSelectionDecision decision(
            CanonicalStarterSelectionDecision.Outcome outcome,
            String pokemonId,
            String speciesId,
            String detail
    ) {
        return new CanonicalStarterSelectionDecision(outcome, pokemonId, speciesId, detail);
    }
}
