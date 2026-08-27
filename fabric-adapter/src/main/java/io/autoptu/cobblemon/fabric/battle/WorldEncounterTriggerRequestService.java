package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Server-owned queue for world encounter requests.
 *
 * Normal visible-wild requests arrive with a canonical encounter ID and external presentation actor
 * ID that were assigned before the player interacts. The service never inspects Cobblemon state.
 */
public final class WorldEncounterTriggerRequestService {
    public record Request(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
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
     * Legacy/server fallback for callers that do not already own a visible encounter identity.
     * Normal wild gameplay should use requestBoundEncounter.
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
                zoneId,
                contextId,
                dimensionId,
                blockX,
                blockY,
                blockZ,
                serverTick
        );
    }

    /**
     * Creates a request for the exact visible wild actor the player engaged.
     * The encounter ID and actor ID come from AutoPTU's server-owned world binding.
     */
    public synchronized Decision requestBoundEncounter(
            String canonicalEncounterId,
            String canonicalPlayerId,
            String externalWildActorId,
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
