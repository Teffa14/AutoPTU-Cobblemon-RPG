package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Server-owned queue for world encounter requests.
 *
 * Normal visible-wild requests arrive with canonical encounter identity, canonical visible species,
 * and the external presentation actor ID that AutoPTU assigned before player interaction. Cobblemon
 * Pokemon state never enters this service.
 */
public final class WorldEncounterTriggerRequestService {
    public record Request(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
            String canonicalWildSpeciesId,
            String zoneId,
            String contextId,
            String dimensionId,
            int blockX,
            int blockY,
            int blockZ,
            long serverTick
    ) {}

    public enum Outcome { CREATED, ALREADY_PENDING }

    public record Decision(Outcome outcome, Request request) {}

    private final Map<String, Request> pendingByPlayerId = new LinkedHashMap<>();
    private long sequence;

    /**
     * Legacy/server fallback for callers without a visible actor. Normal wild gameplay must use
     * requestBoundEncounter so the exact visible species and actor identity are preserved.
     */
    public synchronized Decision request(
            String canonicalPlayerId,
            String zoneId,
            String contextId,
            String dimensionId,
            int blockX,
            int blockY,
            int blockZ,
            long serverTick
    ) {
        String playerId = requireId(canonicalPlayerId, "canonicalPlayerId");
        return create(
                "world-encounter:" + playerId + ":" + (++sequence),
                playerId,
                null,
                null,
                zoneId,
                contextId,
                dimensionId,
                blockX,
                blockY,
                blockZ,
                serverTick
        );
    }

    /** Creates a request for the exact AutoPTU-owned visible wild actor the player engaged. */
    public synchronized Decision requestBoundEncounter(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
            String canonicalWildSpeciesId,
            String zoneId,
            String contextId,
            String dimensionId,
            int blockX,
            int blockY,
            int blockZ,
            long serverTick
    ) {
        return create(
                requireId(canonicalEncounterId, "canonicalEncounterId"),
                requireId(canonicalPlayerId, "canonicalPlayerId"),
                requireId(externalWildActorId, "externalWildActorId"),
                requireId(canonicalWildSpeciesId, "canonicalWildSpeciesId"),
                zoneId,
                contextId,
                dimensionId,
                blockX,
                blockY,
                blockZ,
                serverTick
        );
    }

    private Decision create(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
            String canonicalWildSpeciesId,
            String zoneId,
            String contextId,
            String dimensionId,
            int blockX,
            int blockY,
            int blockZ,
            long serverTick
    ) {
        Request existing = pendingByPlayerId.get(canonicalPlayerId);
        if (existing != null) return new Decision(Outcome.ALREADY_PENDING, existing);

        Request created = new Request(
                canonicalEncounterId,
                canonicalPlayerId,
                externalWildActorId,
                canonicalWildSpeciesId,
                requireId(zoneId, "zoneId"),
                requireId(contextId, "contextId"),
                requireId(dimensionId, "dimensionId"),
                blockX,
                blockY,
                blockZ,
                serverTick
        );
        pendingByPlayerId.put(canonicalPlayerId, created);
        return new Decision(Outcome.CREATED, created);
    }

    public synchronized Optional<Request> pendingForPlayer(String canonicalPlayerId) {
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
        return Optional.ofNullable(pendingByPlayerId.get(canonicalPlayerId.strip()));
    }

    public synchronized boolean clearForPlayer(String canonicalPlayerId) {
        return canonicalPlayerId != null
                && !canonicalPlayerId.isBlank()
                && pendingByPlayerId.remove(canonicalPlayerId.strip()) != null;
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
