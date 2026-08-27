package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WorldEncounterTriggerRequestService {
    public record Request(String canonicalEncounterId, String canonicalPlayerId, String zoneId, String contextId,
                          String dimensionId, int blockX, int blockY, int blockZ, long serverTick) {}

    public enum Outcome { CREATED, ALREADY_PENDING }

    public record Decision(Outcome outcome, Request request) {}

    private final Map<String, Request> pendingByPlayerId = new LinkedHashMap<>();
    private long sequence;

    public synchronized Decision request(String canonicalPlayerId, String zoneId, String contextId,
                                         String dimensionId, int blockX, int blockY, int blockZ, long serverTick) {
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) throw new IllegalArgumentException("canonicalPlayerId is required");
        Request existing = pendingByPlayerId.get(canonicalPlayerId);
        if (existing != null) return new Decision(Outcome.ALREADY_PENDING, existing);
        Request created = new Request("world-encounter:" + canonicalPlayerId + ":" + (++sequence), canonicalPlayerId,
                zoneId, contextId, dimensionId, blockX, blockY, blockZ, serverTick);
        pendingByPlayerId.put(canonicalPlayerId, created);
        return new Decision(Outcome.CREATED, created);
    }

    public synchronized Optional<Request> pendingForPlayer(String canonicalPlayerId) {
        return Optional.ofNullable(pendingByPlayerId.get(canonicalPlayerId));
    }

    public synchronized boolean clearForPlayer(String canonicalPlayerId) {
        return pendingByPlayerId.remove(canonicalPlayerId) != null;
    }
}
