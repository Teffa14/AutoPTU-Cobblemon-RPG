package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned preflight for canonical party mutations requested by Minecraft surfaces.
 *
 * The client/UI may select displayed slots, but it never supplies trusted ownership, party
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
        CanonicalPartySummary.Member member = memberAt(current, request.partySlot());
        if (member == null) {
            return Decision.denied("party slot is not occupied");
        }
        if (!matchesPokemon(member, request.expectedPokemonId())) {
            return Decision.denied("party Pokemon identity changed on the server");
        }

        if (request.mutation() == Mutation.REORDER) {
            if (request.targetPartySlot() < 1 || request.targetPartySlot() == request.partySlot()) {
                return Decision.denied("reorder target slot is invalid");
            }
            CanonicalPartySummary.Member target = memberAt(current, request.targetPartySlot());
            if (target == null) {
                return Decision.denied("reorder target slot is not occupied");
            }
            if (!matchesPokemon(target, request.expectedTargetPokemonId())) {
                return Decision.denied("reorder target Pokemon identity changed on the server");
            }
            return Decision.allowed(
                    request.mutation(),
                    request.partySlot(),
                    member.pokemonId(),
                    request.targetPartySlot(),
                    target.pokemonId(),
                    current.partyRevision()
            );
        }

        return Decision.allowed(
                request.mutation(),
                request.partySlot(),
                member.pokemonId(),
                -1,
                null,
                current.partyRevision()
        );
    }

    private static CanonicalPartySummary.Member memberAt(CanonicalPartySummary party, int slot) {
        if (slot < 1) return null;
        return party.members().stream()
                .filter(candidate -> candidate.slot() == slot)
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesPokemon(CanonicalPartySummary.Member member, String expectedPokemonId) {
        return expectedPokemonId != null
                && !expectedPokemonId.isBlank()
                && member.pokemonId().equals(expectedPokemonId.trim());
    }

    public enum Mutation {
        SET_LEAD,
        REORDER
    }

    public record Request(
            String playerId,
            boolean canonicalTrainerExists,
            Mutation mutation,
            int partySlot,
            String expectedPokemonId,
            int targetPartySlot,
            String expectedTargetPokemonId,
            long expectedPartyRevision,
            CanonicalPartySummary currentParty
    ) {
        public Request(
                String playerId,
                boolean canonicalTrainerExists,
                Mutation mutation,
                int partySlot,
                String expectedPokemonId,
                long expectedPartyRevision,
                CanonicalPartySummary currentParty
        ) {
            this(
                    playerId,
                    canonicalTrainerExists,
                    mutation,
                    partySlot,
                    expectedPokemonId,
                    -1,
                    null,
                    expectedPartyRevision,
                    currentParty
            );
        }
    }

    public record Decision(
            boolean allowed,
            Mutation mutation,
            int partySlot,
            String pokemonId,
            int targetPartySlot,
            String targetPokemonId,
            long partyRevision,
            String reason
    ) {
        public static Decision allowed(
                Mutation mutation,
                int partySlot,
                String pokemonId,
                int targetPartySlot,
                String targetPokemonId,
                long partyRevision
        ) {
            return new Decision(
                    true,
                    mutation,
                    partySlot,
                    pokemonId,
                    targetPartySlot,
                    targetPokemonId,
                    partyRevision,
                    "allowed"
            );
        }

        public static Decision denied(String reason) {
            return new Decision(false, null, -1, null, -1, null, -1, reason);
        }
    }
}
