package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Builds a read-only party view exclusively from durable canonical repositories. */
public final class CanonicalPartyQueryService {
    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalPartyQueryService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public Optional<CanonicalPartySummary> findParty(String authenticatedPlayerId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()) return Optional.empty();
        String playerId = authenticatedPlayerId.strip();
        Optional<CanonicalPlayerEncounterProfile> profileResult = partyRepository.findProfile(playerId);
        if (profileResult.isEmpty()) return Optional.empty();

        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) {
            throw new IllegalStateException("party repository returned mismatched player identity");
        }

        ArrayList<CanonicalPartySummary.Member> members = new ArrayList<>();
        for (int index = 0; index < profile.pokemonIds().size(); index++) {
            String pokemonId = profile.pokemonIds().get(index);
            CanonicalPokemonState pokemon = pokemonRepository.findPokemon(pokemonId)
                    .orElseThrow(() -> new IllegalStateException("party references missing canonical Pokemon: " + pokemonId));
            if (!pokemon.ownerPlayerId().equals(playerId)) {
                throw new IllegalStateException("party references Pokemon owned by another player: " + pokemonId);
            }
            CanonicalHealth health = pokemon.health();
            List<String> statuses = pokemon.statuses().stream().sorted(Comparator.naturalOrder()).toList();
            members.add(new CanonicalPartySummary.Member(
                    index + 1,
                    pokemon.pokemonId(),
                    pokemon.speciesId(),
                    pokemon.level(),
                    health == null ? null : health.currentHp(),
                    health == null ? null : health.maxHp(),
                    statuses,
                    pokemon.revision()
            ));
        }
        return Optional.of(new CanonicalPartySummary(playerId, members, profile.revision()));
    }
}
