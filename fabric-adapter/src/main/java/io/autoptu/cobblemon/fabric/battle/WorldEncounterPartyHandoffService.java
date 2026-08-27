package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-owned handoff between a world encounter request and later battle startup.
 *
 * This service freezes only already-canonical RPG/world facts: the authenticated player's active
 * canonical party plus the exact server-authored WILD blueprint selected for the visible encounter.
 * It does not calculate PTU legality, battle RNG, damage, statuses, action economy or outcomes.
 */
public final class WorldEncounterPartyHandoffService {
    public record Reservation(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
            List<String> canonicalPlayerPokemonIds,
            Map<String, Integer> consumableQuantities,
            CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint wildBlueprint,
            String zoneId,
            String contextId,
            String dimensionId,
            int blockX,
            int blockY,
            int blockZ,
            long serverTick
    ) {
        public Reservation {
            canonicalEncounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
            canonicalPlayerId = requireId(canonicalPlayerId, "canonicalPlayerId");
            externalWildActorId = requireId(externalWildActorId, "externalWildActorId");
            if (canonicalPlayerPokemonIds == null || canonicalPlayerPokemonIds.isEmpty()) {
                throw new IllegalArgumentException("canonicalPlayerPokemonIds must not be empty");
            }
            canonicalPlayerPokemonIds = List.copyOf(canonicalPlayerPokemonIds);
            consumableQuantities = consumableQuantities == null
                    ? Map.of()
                    : Map.copyOf(new LinkedHashMap<>(consumableQuantities));
            wildBlueprint = Objects.requireNonNull(wildBlueprint, "wildBlueprint");
            zoneId = requireId(zoneId, "zoneId");
            contextId = requireId(contextId, "contextId");
            dimensionId = requireId(dimensionId, "dimensionId");
        }
    }

    public enum Outcome {
        CREATED,
        ALREADY_RESERVED,
        PLAYER_CONTEXT_MISMATCH,
        WILD_BLUEPRINT_MISSING,
        WILD_BLUEPRINT_MISMATCH
    }

    public record Decision(Outcome outcome, Reservation reservation) {
        public boolean created() {
            return outcome == Outcome.CREATED;
        }
    }

    private final CanonicalWildEncounterBlueprintSource wildBlueprintSource;
    private final Map<String, Reservation> byEncounterId = new LinkedHashMap<>();
    private final Map<String, Reservation> byPlayerId = new LinkedHashMap<>();

    public WorldEncounterPartyHandoffService(CanonicalWildEncounterBlueprintSource wildBlueprintSource) {
        this.wildBlueprintSource = Objects.requireNonNull(wildBlueprintSource, "wildBlueprintSource");
    }

    /**
     * Freezes the exact canonical party/context that existed when the world request was accepted.
     * A later party edit cannot mutate this reservation because all collections and blueprint
     * collections are immutable copies.
     */
    public synchronized Decision reserve(
            WorldEncounterTriggerRequestService.Request request,
            CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext playerContext
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(playerContext, "playerContext");

        Reservation encounterExisting = byEncounterId.get(request.canonicalEncounterId());
        if (encounterExisting != null) return new Decision(Outcome.ALREADY_RESERVED, encounterExisting);
        Reservation playerExisting = byPlayerId.get(request.canonicalPlayerId());
        if (playerExisting != null) return new Decision(Outcome.ALREADY_RESERVED, playerExisting);

        if (!request.canonicalPlayerId().equals(playerContext.canonicalPlayerId())) {
            return new Decision(Outcome.PLAYER_CONTEXT_MISMATCH, null);
        }
        if (request.externalWildActorId() == null || request.externalWildActorId().isBlank()) {
            return new Decision(Outcome.WILD_BLUEPRINT_MISMATCH, null);
        }

        Optional<CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint> blueprintResult =
                wildBlueprintSource.resolve(request.canonicalEncounterId());
        if (blueprintResult.isEmpty()) {
            return new Decision(Outcome.WILD_BLUEPRINT_MISSING, null);
        }
        CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint blueprint = blueprintResult.get();
        if (!request.canonicalEncounterId().equals(blueprint.canonicalEncounterId())) {
            return new Decision(Outcome.WILD_BLUEPRINT_MISMATCH, null);
        }

        Reservation reservation = new Reservation(
                request.canonicalEncounterId(),
                request.canonicalPlayerId(),
                request.externalWildActorId(),
                playerContext.canonicalPokemonIds(),
                playerContext.consumableQuantities(),
                blueprint,
                request.zoneId(),
                request.contextId(),
                request.dimensionId(),
                request.blockX(),
                request.blockY(),
                request.blockZ(),
                request.serverTick()
        );
        byEncounterId.put(reservation.canonicalEncounterId(), reservation);
        byPlayerId.put(reservation.canonicalPlayerId(), reservation);
        return new Decision(Outcome.CREATED, reservation);
    }

    public synchronized Optional<Reservation> findByEncounterId(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byEncounterId.get(canonicalEncounterId.strip()));
    }

    public synchronized Optional<Reservation> findByPlayerId(String canonicalPlayerId) {
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byPlayerId.get(canonicalPlayerId.strip()));
    }

    public synchronized boolean release(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return false;
        Reservation removed = byEncounterId.remove(canonicalEncounterId.strip());
        if (removed == null) return false;
        byPlayerId.remove(removed.canonicalPlayerId(), removed);
        return true;
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
