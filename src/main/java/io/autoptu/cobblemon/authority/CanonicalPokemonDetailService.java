package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Optional;

/** Resolves one party slot into a player-visible canonical Pokemon detail view. */
public final class CanonicalPokemonDetailService {
    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalPokemonDetailService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public Optional<CanonicalPokemonDetail> findPokemon(String authenticatedPlayerId, int slot) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank() || slot < 1) return Optional.empty();
        String playerId = authenticatedPlayerId.strip();
        Optional<CanonicalPlayerEncounterProfile> profileResult = partyRepository.findProfile(playerId);
        if (profileResult.isEmpty()) return Optional.empty();

        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) {
            throw new IllegalStateException("party repository returned mismatched player identity");
        }
        if (slot > profile.pokemonIds().size()) return Optional.empty();

        String pokemonId = profile.pokemonIds().get(slot - 1);
        CanonicalPokemonState pokemon = pokemonRepository.findPokemon(pokemonId)
                .orElseThrow(() -> new IllegalStateException("party references missing canonical Pokemon: " + pokemonId));
        if (!pokemon.ownerPlayerId().equals(playerId)) {
            throw new IllegalStateException("party references Pokemon owned by another player: " + pokemonId);
        }

        return Optional.of(new CanonicalPokemonDetail(
                slot,
                pokemon.pokemonId(),
                pokemon.speciesId(),
                pokemon.level(),
                pokemon.health(),
                pokemon.statuses().stream().toList(),
                pokemon.combatStats(),
                pokemon.moveLoadout(),
                pokemon.baseMovement(),
                pokemon.battleTraits(),
                pokemon.accuracyEvasion(),
                pokemon.injuryState(),
                pokemon.heldItemInstanceId() != null,
                pokemon.capabilities().stream().toList(),
                pokemon.revision()
        ));
    }
}
