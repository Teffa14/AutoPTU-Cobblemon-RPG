package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned preflight for canonical party mutations requested by Minecraft surfaces.
 *
 * The client/UI may select a displayed slot, but it never supplies trusted ownership, party
 * membership, revision or Pokemon identity. Callers must re-resolve the current canonical party
 * and pass that server-owned snapshot here before invoking a durable mutation service.
 */
public final class CanonicalPartyManagementService {

    public Decision canManage(Request request) {
        Objects.requireNonNull(request, "request");
        if (request.playerId() == null || request.playerId().isBlank()) {
            return Decision.denied("canonical player id is required");
        }
        if (!request.canonicalTrainerExists()) {
            return Decision.denied("canonical Trainer is not provisioned");
        }
        if (request.mutation() == null) {
            return Decision.denied("party mutation is required");
        }
        CanonicalPartySummary current = request.currentParty();
        if (current == null || current.members().isEmpty()) {
            return Decision.denied("canonical party is unavailable");
        }
        String playerId = request.playerId().trim();
        if (!current.playerId().equals(playerId)) {
            return Decision.denied("party owner does not match authenticated player");
        }
        if (request.expectedPartyRevision() < 0 || current.partyRevision() != request.expectedPartyRevision()) {
            return Decision.denied("party revision changed on the server");
        }
        if (request.partySlot() < 1) {
            return Decision.denied("party slot is invalid");
        }
        CanonicalPartySummary.Member member = current.members().stream()
                .filter(candidate -> candidate.slot() == request.partySlot())
                .findFirst()
                .orElse(null);
        if (member == null) {
            return Decision.denied("party slot is not occupied");
        }
        if (request.expectedPokemonId() == null || request.expectedPokemonId().isBlank()
                || !member.pokemonId().equals(request.expectedPokemonId().trim())) {
            return Decision.denied("party Pokemon identity changed on the server");
        }
        return Decision.allowed(request.mutation(), request.partySlot(), member.pokemonId(), current.partyRevision());
    }

    public enum Mutation {
        SET_LEAD
    }

    public record Request(
            String playerId,
            boolean canonicalTrainerExists,
            Mutation mutation,
            int partySlot,
            String expectedPokemonId,
            long expectedPartyRevision,
            CanonicalPartySummary currentParty
    ) {}

    public record Decision(
            boolean allowed,
            Mutation mutation,
            int partySlot,
            String pokemonId,
            long partyRevision,
            String reason
    ) {
        public static Decision allowed(Mutation mutation, int partySlot, String pokemonId, long partyRevision) {
            return new Decision(true, mutation, partySlot, pokemonId, partyRevision, "allowed");
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, -1, null, -1, reason);
        }
    }
}
