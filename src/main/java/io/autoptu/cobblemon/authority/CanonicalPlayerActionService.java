package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned preflight for Minecraft-facing RPG actions.
 *
 * This boundary validates authenticated canonical player state and a server-observed action context.
 * It does not decide PTU legality, action economy, RNG, damage, item effects, capture, battle state,
 * progression rewards or any other rule owned by AutoPTU-Java.
 */
public final class CanonicalPlayerActionService {

    public Decision canPerform(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.playerId() == null || request.playerId().isBlank()) {
            return Decision.denied("canonical player id is required");
        }
        if (!request.canonicalTrainerExists()) {
            return Decision.denied("canonical Trainer is not provisioned");
        }
        if (request.action() == null) {
            return Decision.denied("action kind is required");
        }
        if (request.contextId() == null || request.contextId().isBlank()) {
            return Decision.denied("server action context is required");
        }
        if (!request.serverObservedContext()) {
            return Decision.denied("action context was not observed by the server");
        }
        return Decision.allowed(request.action(), request.contextId());
    }

    public record Request(
            String playerId,
            boolean canonicalTrainerExists,
            ActionKind action,
            String contextId,
            boolean serverObservedContext
    ) {}

    public record Decision(boolean allowed, ActionKind action, String contextId, String reason) {
        public static Decision allowed(ActionKind action, String contextId) {
            return new Decision(true, action, contextId, "allowed");
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, null, reason);
        }
    }
}
