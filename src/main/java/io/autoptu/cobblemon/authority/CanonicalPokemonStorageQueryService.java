package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Builds a read-only box view from server-owned durable state only. */
public final class CanonicalPokemonStorageQueryService {
    private final VersionedCanonicalPokemonStorageRepository storageRepository;
    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalPokemonStorageQueryService(
            VersionedCanonicalPokemonStorageRepository storageRepository,
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.storageRepository = Objects.requireNonNull(storageRepository, "storageRepository");
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public CanonicalPokemonStorageSummary inspect(String authenticatedPlayerId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()) {
            throw new IllegalArgumentException("authenticatedPlayerId is required");
        }
        String playerId = authenticatedPlayerId.strip();
        CanonicalPokemonStorageState storage = storageRepository.findOrCreate(playerId);
        Set<String> activeParty = partyRepository.findProfile(playerId)
                .map(profile -> new HashSet<>(profile.pokemonIds()))
                .orElseGet(HashSet::new);
        ArrayList<CanonicalPokemonStorageSummary.Member> members = new ArrayList<>();
        for (int index = 0; index < storage.pokemonIds().size(); index++) {
            String pokemonId = storage.pokemonIds().get(index);
            if (activeParty.contains(pokemonId)) {
                throw new IllegalStateException("canonical Pokemon cannot exist in party and storage simultaneously: " + pokemonId);
            }
            CanonicalPokemonState pokemon = pokemonRepository.findPokemon(pokemonId)
                    .orElseThrow(() -> new IllegalStateException("storage references missing canonical Pokemon: " + pokemonId));
            if (!pokemon.ownerPlayerId().equals(playerId)) {
                throw new IllegalStateException("storage references Pokemon owned by another player: " + pokemonId);
            }
            members.add(new CanonicalPokemonStorageSummary.Member(
                    index + 1,
                    pokemon.pokemonId(),
                    pokemon.speciesId(),
                    pokemon.level(),
                    pokemon.revision()
            ));
        }
        return new CanonicalPokemonStorageSummary(playerId, members, storage.revision());
    }
}
