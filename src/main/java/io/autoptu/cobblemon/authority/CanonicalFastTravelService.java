package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned authorization boundary for Minecraft fast travel.
 *
 * Minecraft owns the physical travel point and teleport implementation. This service only decides
 * whether a request may use an observed point and a server-authored destination. It owns no PTU
 * legality, battle state, RNG, rewards or client-authored coordinates.
 */
public final class CanonicalFastTravelService {
    private final double maxDistanceSquared;

    public CanonicalFastTravelService(double maxDistanceSquared) {
        if (maxDistanceSquared <= 0.0D) throw new IllegalArgumentException("maxDistanceSquared must be positive");
        this.maxDistanceSquared = maxDistanceSquared;
    }

    public Decision canTravel(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.playerId() == null || request.playerId().isBlank()) {
            return Decision.denied("canonical player id is required");
        }
        if (!request.canonicalTrainerExists()) {
            return Decision.denied("canonical Trainer is not provisioned");
        }
        if (request.sourcePointId() == null || request.sourcePointId().isBlank()) {
            return Decision.denied("fast-travel source is required");
        }
        if (!request.sourcePointObserved()) {
            return Decision.denied("fast-travel source no longer exists");
        }
        if (request.distanceSquared() > maxDistanceSquared) {
            return Decision.denied("fast-travel source is out of interaction range");
        }
        if (request.destinationId() == null || request.destinationId().isBlank()) {
            return Decision.denied("destination is required");
        }
        if (!request.destinationAuthored()) {
            return Decision.denied("destination is not server-authored");
        }
        if (!request.destinationAvailable()) {
            return Decision.denied("destination is not currently available");
        }
        return Decision.allowed(request.sourcePointId(), request.destinationId());
    }

    public record Request(
            String playerId,
            boolean canonicalTrainerExists,
            String sourcePointId,
            boolean sourcePointObserved,
            double distanceSquared,
            String destinationId,
            boolean destinationAuthored,
            boolean destinationAvailable
    ) {}

    public record Decision(boolean allowed, String sourcePointId, String destinationId, String reason) {
        public static Decision allowed(String sourcePointId, String destinationId) {
            return new Decision(true, sourcePointId, destinationId, "allowed");
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, null, reason);
        }
    }
}
