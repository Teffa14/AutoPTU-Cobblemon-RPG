package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.battle.PersistentWorldEncounterPartyHandoffService;

import java.util.Objects;

/**
 * Server-owned lifecycle boundary for returning a reserved visible WILD actor to normal world play.
 *
 * <p>The caller must first own the authoritative decision that the encounter reservation may be
 * released. This service only coordinates the already-authoritative handoff release with Minecraft
 * presentation/interactivity. It does not infer battle completion, outcomes, HP, faint, capture,
 * legality, RNG, or any other PTU rule from the Cobblemon actor.</p>
 */
public final class VisibleWildReservationReleaseService {
    public enum Outcome {
        RELEASED_AND_REACTIVATED,
        RESERVATION_NOT_FOUND,
        PRESENTATION_NOT_BOUND
    }

    private final PersistentWorldEncounterPartyHandoffService handoffs;

    public VisibleWildReservationReleaseService(PersistentWorldEncounterPartyHandoffService handoffs) {
        this.handoffs = Objects.requireNonNull(handoffs, "handoffs");
    }

    public synchronized Outcome release(String canonicalEncounterId) {
        String encounterId = requireId(canonicalEncounterId);
        if (!handoffs.release(encounterId)) return Outcome.RESERVATION_NOT_FOUND;

        var entityUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (entityUuid.isEmpty()) return Outcome.PRESENTATION_NOT_BOUND;

        VisibleWildPokemonEncounterRuntime.setInteractionActive(entityUuid.get(), true);
        return Outcome.RELEASED_AND_REACTIVATED;
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("canonicalEncounterId is required");
        }
        return value.strip();
    }
}
