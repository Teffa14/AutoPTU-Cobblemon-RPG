package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    private WorldEncounterTriggerRequestRepository repository;
    private long sequence;

    public WorldEncounterTriggerRequestService() {
        this(new InMemoryRepository());
    }

    public WorldEncounterTriggerRequestService(WorldEncounterTriggerRequestRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Rebinds this long-lived server facade to the repository for the current world save.
     * This is intended for Minecraft server lifecycle wiring before players can create requests.
     */
    public synchronized void useRepository(WorldEncounterTriggerRequestRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

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
        Optional<Request> existing = repository.findPending(canonicalPlayerId);
        if (existing.isPresent()) return new Decision(Outcome.ALREADY_PENDING, existing.get());

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
        if (!repository.saveIfAbsent(created)) {
            Request raced = repository.findPending(canonicalPlayerId)
                    .orElseThrow(() -> new IllegalStateException("active encounter request disappeared during create"));
            return new Decision(Outcome.ALREADY_PENDING, raced);
        }
        return new Decision(Outcome.CREATED, created);
    }

    public synchronized Optional<Request> pendingForPlayer(String canonicalPlayerId) {
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
        return repository.findPending(canonicalPlayerId.strip());
    }

    public synchronized boolean clearForPlayer(String canonicalPlayerId) {
        return canonicalPlayerId != null
                && !canonicalPlayerId.isBlank()
                && repository.clear(canonicalPlayerId.strip());
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static final class InMemoryRepository implements WorldEncounterTriggerRequestRepository {
        private final Map<String, Request> pendingByPlayerId = new LinkedHashMap<>();

        @Override
        public Optional<Request> findPending(String canonicalPlayerId) {
            if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
            return Optional.ofNullable(pendingByPlayerId.get(canonicalPlayerId.strip()));
        }

        @Override
        public boolean saveIfAbsent(Request request) {
            Objects.requireNonNull(request, "request");
            return pendingByPlayerId.putIfAbsent(request.canonicalPlayerId(), request) == null;
        }

        @Override
        public boolean clear(String canonicalPlayerId) {
            return canonicalPlayerId != null
                    && !canonicalPlayerId.isBlank()
                    && pendingByPlayerId.remove(canonicalPlayerId.strip()) != null;
        }
    }
}
