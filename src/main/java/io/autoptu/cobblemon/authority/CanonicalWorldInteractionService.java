package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned authorization boundary for authored Minecraft world interactions.
 *
 * This service validates identity, authored object identity, observed object type and range only.
 * It deliberately owns no PTU legality, RNG, rewards, battle state or progression inference.
 */
public final class CanonicalWorldInteractionService {
    private final double maxDistanceSquared;

    public CanonicalWorldInteractionService(double maxDistanceSquared) {
        if (maxDistanceSquared <= 0.0D) throw new IllegalArgumentException("maxDistanceSquared must be positive");
        this.maxDistanceSquared = maxDistanceSquared;
    }

    public Decision canInteract(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.playerId() == null || request.playerId().isBlank()) return Decision.denied("canonical player id is required");
        if (!request.canonicalTrainerExists()) return Decision.denied("canonical Trainer is not provisioned");
        if (request.objectId() == null || request.objectId().isBlank()) return Decision.denied("authored object id is required");
        if (request.expectedKind() == null || request.observedKind() == null) return Decision.denied("interaction kind is required");
        if (request.expectedKind() != request.observedKind()) return Decision.denied("world object no longer matches authored kind");
        if (request.distanceSquared() > maxDistanceSquared) return Decision.denied("world object is out of interaction range");
        return Decision.allowed(request.objectId(), request.expectedKind());
    }

    public enum Kind {
        CHEST,
        SWITCH,
        DOOR,
        TERMINAL,
        SHRINE
    }

    public record Request(
            String playerId,
            boolean canonicalTrainerExists,
            String objectId,
            Kind expectedKind,
            Kind observedKind,
            double distanceSquared
    ) {}

    public record Decision(boolean allowed, String objectId, Kind kind, String reason) {
        public static Decision allowed(String objectId, Kind kind) {
            return new Decision(true, objectId, kind, "allowed");
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, null, reason);
        }
    }
}
