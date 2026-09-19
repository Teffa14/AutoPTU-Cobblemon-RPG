package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.battle.PersistentWorldEncounterPartyHandoffService;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;

import java.util.Objects;

/**
 * Server-owned lifecycle boundary for returning a reserved visible WILD actor to normal world play.
 *
 * <p>The caller must first own the authoritative decision that the encounter reservation may be
 * released. This service only coordinates the already-authoritative handoff release, durable pending
 * request cleanup, and Minecraft presentation/interactivity. It does not infer battle completion,
 * outcomes, HP, faint, capture, legality, RNG, or any other PTU rule from the Cobblemon actor.</p>
 */
public final class VisibleWildReservationReleaseService {
    public enum Outcome {
        RELEASED_AND_REACTIVATED,
        RELEASED_PRESENTATION_NOT_BOUND,
        RESERVATION_NOT_FOUND,
        REQUEST_MISMATCH
    }

    private final PersistentWorldEncounterPartyHandoffService handoffs;
    private final WorldEncounterTriggerRequestService requests;

    public VisibleWildReservationReleaseService(
            PersistentWorldEncounterPartyHandoffService handoffs,
            WorldEncounterTriggerRequestService requests
    ) {
        this.handoffs = Objects.requireNonNull(handoffs, "handoffs");
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    public synchronized Outcome release(String canonicalEncounterId) {
        String encounterId = requireId(canonicalEncounterId);
        var reservation = handoffs.findByEncounterId(encounterId).orElse(null);
        if (reservation == null) return Outcome.RESERVATION_NOT_FOUND;

        var pending = requests.pendingForPlayer(reservation.canonicalPlayerId()).orElse(null);
        if (pending == null || !encounterId.equals(pending.canonicalEncounterId())) {
            return Outcome.REQUEST_MISMATCH;
        }

        if (!handoffs.release(encounterId)) return Outcome.RESERVATION_NOT_FOUND;
        requests.clearForPlayer(reservation.canonicalPlayerId());

        var entityUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (entityUuid.isEmpty()) return Outcome.RELEASED_PRESENTATION_NOT_BOUND;

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
