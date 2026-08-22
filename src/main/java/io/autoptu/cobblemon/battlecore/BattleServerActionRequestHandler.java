package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Server networking/application boundary for minimal client battle intent.
 *
 * The caller supplies an authenticated server principal separately from the client payload.
 * Current canonical battle preparation is resolved from server-owned state before the existing
 * legal-choice gateway validates and executes the exact core-produced choice.
 */
public final class BattleServerActionRequestHandler {
    private BattleServerActionRequestHandler() {}

    public static BattleCoreLegalChoice handle(
            String authenticatedPrincipalId,
            BattleClientActionRequest request,
            BattleAuthoritativePreparationSource preparationSource,
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        authenticatedPrincipalId = normalize(authenticatedPrincipalId, "authenticatedPrincipalId");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(preparationSource, "preparationSource");
        Objects.requireNonNull(legalChoiceSource, "legalChoiceSource");
        Objects.requireNonNull(executor, "executor");

        BattleRuntimePreparationEnvelope preparation = Objects.requireNonNull(
                preparationSource.preparation(
                        authenticatedPrincipalId,
                        request.reservationId(),
                        request.actorId()),
                "authoritative preparation source returned null");

        return BattleServerActionGateway.execute(preparation, request, legalChoiceSource, executor);
    }

    private static String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
