package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalStateRepository;
import io.autoptu.cobblemon.authority.VersionedCanonicalPlayerEncounterProfileRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Production bridge from a durable visible-world encounter request to the immutable battle handoff.
 *
 * The request already belongs to an authenticated Minecraft player. This service re-resolves the
 * canonical Trainer and encounter profile from server-owned persistence at handoff time and freezes
 * only those values plus the exact AutoPTU-owned WILD blueprint. Cobblemon entity state is never an
 * input, and this layer does not decide any PTU battle legality, RNG, damage, status or outcome.
 */
public final class PersistentWorldEncounterPartyHandoffService {
    public enum Outcome {
        CREATED,
        ALREADY_RESERVED,
        PLAYER_MISSING,
        ENCOUNTER_PROFILE_MISSING,
        PLAYER_CONTEXT_MISMATCH,
        WILD_BLUEPRINT_MISSING,
        WILD_BLUEPRINT_MISMATCH
    }

    public record Decision(Outcome outcome, WorldEncounterPartyHandoffService.Reservation reservation) {
        public boolean ready() {
            return outcome == Outcome.CREATED || outcome == Outcome.ALREADY_RESERVED;
        }
    }

    private final CanonicalStateRepository playerRepository;
    private final VersionedCanonicalPlayerEncounterProfileRepository profileRepository;
    private final WorldEncounterPartyHandoffService handoffService;

    public PersistentWorldEncounterPartyHandoffService(
            CanonicalStateRepository playerRepository,
            VersionedCanonicalPlayerEncounterProfileRepository profileRepository,
            CanonicalWildEncounterBlueprintSource wildBlueprintSource
    ) {
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.handoffService = new WorldEncounterPartyHandoffService(
                Objects.requireNonNull(wildBlueprintSource, "wildBlueprintSource"));
    }

    public synchronized Decision reserve(WorldEncounterTriggerRequestService.Request request) {
        Objects.requireNonNull(request, "request");
        String playerId = request.canonicalPlayerId();
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            return new Decision(Outcome.PLAYER_MISSING, null);
        }

        Optional<CanonicalPlayerEncounterProfile> profileResult = profileRepository.findProfile(playerId);
        if (profileResult.isEmpty()) {
            return new Decision(Outcome.ENCOUNTER_PROFILE_MISSING, null);
        }
        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) {
            return new Decision(Outcome.PLAYER_CONTEXT_MISMATCH, null);
        }

        var context = new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                playerId,
                profile.pokemonIds(),
                profile.consumableQuantities(),
                profile.arena()
        );
        WorldEncounterPartyHandoffService.Decision decision = handoffService.reserve(request, context);
        return new Decision(map(decision.outcome()), decision.reservation());
    }

    public synchronized Optional<WorldEncounterPartyHandoffService.Reservation> findByPlayerId(String playerId) {
        return handoffService.findByPlayerId(playerId);
    }

    public synchronized boolean release(String encounterId) {
        return handoffService.release(encounterId);
    }

    private static Outcome map(WorldEncounterPartyHandoffService.Outcome outcome) {
        return switch (outcome) {
            case CREATED -> Outcome.CREATED;
            case ALREADY_RESERVED -> Outcome.ALREADY_RESERVED;
            case PLAYER_CONTEXT_MISMATCH -> Outcome.PLAYER_CONTEXT_MISMATCH;
            case WILD_BLUEPRINT_MISSING -> Outcome.WILD_BLUEPRINT_MISSING;
            case WILD_BLUEPRINT_MISMATCH -> Outcome.WILD_BLUEPRINT_MISMATCH;
        };
    }
}
