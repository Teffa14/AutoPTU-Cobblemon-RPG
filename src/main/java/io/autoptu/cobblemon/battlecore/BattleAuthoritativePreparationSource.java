package io.autoptu.cobblemon.battlecore;

/**
 * Resolves the current battle preparation from server-owned identity and reservation state.
 * The authenticated principal comes from the server connection/context, never from the client payload.
 */
@FunctionalInterface
public interface BattleAuthoritativePreparationSource {
    BattleRuntimePreparationEnvelope preparation(
            String authenticatedPrincipalId,
            String reservationId,
            String actorId
    );
}
