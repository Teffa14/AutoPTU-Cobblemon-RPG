package io.autoptu.cobblemon.battlecore;

/**
 * Adapter-neutral server packet boundary.
 *
 * Networking supplies the authenticated principal from server connection context. The client packet
 * is decoded into minimal intent and then routed through the existing authoritative request handler.
 */
public final class BattleServerActionPacketHandler {
    private BattleServerActionPacketHandler() {}

    public static BattleCoreLegalChoice handle(
            String authenticatedPrincipalId,
            BattleClientActionPacket packet,
            BattleAuthoritativePreparationSource preparationSource,
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        BattleClientActionRequest request = BattleClientActionPacketDecoder.decode(packet);
        return BattleServerActionRequestHandler.handle(
                authenticatedPrincipalId,
                request,
                preparationSource,
                legalChoiceSource,
                executor);
    }
}
