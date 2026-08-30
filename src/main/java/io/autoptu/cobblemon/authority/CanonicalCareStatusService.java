package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only care projection built only from persistent server-owned party and Pokemon state.
 * It deliberately exposes stored HP, statuses and injuries without deriving recovery eligibility
 * or applying any PTU healing/status/injury rule in Minecraft.
 */
public final class CanonicalCareStatusService {
    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalCareStatusService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public Optional<Summary> findStatus(String authenticatedPlayerId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()) return Optional.empty();
        String playerId = authenticatedPlayerId.strip();
        Optional<CanonicalPlayerEncounterProfile> profileResult = partyRepository.findProfile(playerId);
        if (profileResult.isEmpty()) return Optional.empty();

        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) {
            throw new IllegalStateException("party repository returned mismatched player identity");
        }

        ArrayList<Member> members = new ArrayList<>();
        for (int index = 0; index < profile.pokemonIds().size(); index++) {
            String pokemonId = profile.pokemonIds().get(index);
            CanonicalPokemonState pokemon = pokemonRepository.findPokemon(pokemonId)
                    .orElseThrow(() -> new IllegalStateException("party references missing canonical Pokemon: " + pokemonId));
            if (!pokemon.ownerPlayerId().equals(playerId)) {
                throw new IllegalStateException("party references Pokemon owned by another player: " + pokemonId);
            }

            CanonicalHealth health = pokemon.health();
            CanonicalInjuryState injuryState = pokemon.injuryState();
            List<String> statuses = pokemon.statuses().stream().sorted(Comparator.naturalOrder()).toList();
            members.add(new Member(
                    index + 1,
                    pokemon.pokemonId(),
                    pokemon.speciesId(),
                    health == null ? null : health.currentHp(),
                    health == null ? null : health.maxHp(),
                    statuses,
                    injuryState == null ? null : injuryState.injuries(),
                    pokemon.revision()
            ));
        }
        return Optional.of(new Summary(playerId, List.copyOf(members), profile.revision()));
    }

    public record Summary(String playerId, List<Member> members, long partyRevision) {
        public Summary {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            members = members == null ? List.of() : List.copyOf(members);
            if (partyRevision < 0) throw new IllegalArgumentException("partyRevision must be >= 0");
        }
    }

    public record Member(
            int slot,
            String pokemonId,
            String speciesId,
            Integer currentHp,
            Integer maxHp,
            List<String> statuses,
            Integer injuries,
            long pokemonRevision
    ) {
        public Member {
            if (slot < 1) throw new IllegalArgumentException("slot must be >= 1");
            if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId must not be blank");
            if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId must not be blank");
            if ((currentHp == null) != (maxHp == null)) throw new IllegalArgumentException("HP values must both be present or absent");
            if (currentHp != null && (maxHp <= 0 || currentHp < 0 || currentHp > maxHp)) {
                throw new IllegalArgumentException("invalid HP projection");
            }
            statuses = statuses == null ? List.of() : List.copyOf(statuses);
            if (injuries != null && injuries < 0) throw new IllegalArgumentException("injuries must be >= 0 when available");
            if (pokemonRevision < 0) throw new IllegalArgumentException("pokemonRevision must be >= 0");
        }

        public boolean hasHealth() { return currentHp != null; }
        public boolean hasInjuryState() { return injuries != null; }
    }
}
