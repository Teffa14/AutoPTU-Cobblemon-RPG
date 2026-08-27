package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Restores persistent HP for the server-owned encounter party outside battle.
 *
 * This service does not cure statuses, injuries, held-item state or any battle-scoped condition.
 * Those mechanics remain owned by explicit upstream/server-authoritative rules. The client supplies
 * only an authenticated player identity; party membership, ownership, HP and revisions are loaded
 * from canonical repositories.
 */
public final class CanonicalPartyHealingService {
    private static final int MAX_CAS_ATTEMPTS = 3;

    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    public CanonicalPartyHealingService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public CanonicalPartyHealingDecision healParty(String authenticatedPlayerId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()) {
            return new CanonicalPartyHealingDecision(
                    CanonicalPartyHealingDecision.Outcome.INVALID_REQUEST,
                    0,
                    0,
                    List.of(),
                    "authenticated player identity is required"
            );
        }
        String playerId = authenticatedPlayerId.strip();
        Optional<CanonicalPlayerEncounterProfile> profileResult = partyRepository.findProfile(playerId);
        if (profileResult.isEmpty()) {
            return new CanonicalPartyHealingDecision(
                    CanonicalPartyHealingDecision.Outcome.NO_PARTY,
                    0,
                    0,
                    List.of(),
                    "no canonical party is configured for this player"
            );
        }

        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) {
            return new CanonicalPartyHealingDecision(
                    CanonicalPartyHealingDecision.Outcome.INVALID_REQUEST,
                    0,
                    0,
                    profile.pokemonIds(),
                    "party repository returned a mismatched player identity"
            );
        }

        int healed = 0;
        int alreadyFull = 0;
        ArrayList<String> failed = new ArrayList<>();
        for (String pokemonId : profile.pokemonIds()) {
            HealOneResult result = healOne(playerId, pokemonId);
            switch (result) {
                case HEALED -> healed++;
                case ALREADY_FULL -> alreadyFull++;
                case FAILED -> failed.add(pokemonId);
            }
        }

        CanonicalPartyHealingDecision.Outcome outcome = failed.isEmpty()
                ? CanonicalPartyHealingDecision.Outcome.APPLIED
                : CanonicalPartyHealingDecision.Outcome.PARTIAL;
        return new CanonicalPartyHealingDecision(
                outcome,
                healed,
                alreadyFull,
                failed,
                failed.isEmpty() ? "" : "one or more canonical Pokemon could not be healed safely"
        );
    }

    private HealOneResult healOne(String playerId, String pokemonId) {
        for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
            Optional<CanonicalPokemonState> pokemonResult = pokemonRepository.findPokemon(pokemonId);
            if (pokemonResult.isEmpty()) return HealOneResult.FAILED;
            CanonicalPokemonState current = pokemonResult.get();
            if (!current.pokemonId().equals(pokemonId) || !current.ownerPlayerId().equals(playerId)) {
                return HealOneResult.FAILED;
            }
            CanonicalHealth health = current.health();
            if (health == null) return HealOneResult.FAILED;
            if (health.currentHp() == health.maxHp()) return HealOneResult.ALREADY_FULL;

            CanonicalPokemonState replacement = new CanonicalPokemonState(
                    current.pokemonId(),
                    current.ownerPlayerId(),
                    current.speciesId(),
                    current.level(),
                    current.capabilities(),
                    current.statuses(),
                    current.statusState(),
                    current.combatStats(),
                    new CanonicalHealth(health.maxHp(), health.maxHp()),
                    current.moveLoadout(),
                    current.baseMovement(),
                    current.battleTraits(),
                    current.accuracyEvasion(),
                    current.injuryState(),
                    current.heldItemInstanceId(),
                    current.revision() + 1
            );
            if (pokemonRepository.replacePokemonIfRevision(pokemonId, current.revision(), replacement)) {
                return HealOneResult.HEALED;
            }
        }
        return HealOneResult.FAILED;
    }

    private enum HealOneResult {
        HEALED,
        ALREADY_FULL,
        FAILED
    }
}
