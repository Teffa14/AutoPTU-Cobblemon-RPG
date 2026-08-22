package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;

/**
 * Performs only reservation/identity/loadout anti-forgery checks before a client request
 * is handed to AutoPTU-Java. Passing this gate does not mean the requested action is legal.
 */
public final class BattleClientActionRequestGate {
    private BattleClientActionRequestGate() {}

    public static BattleClientActionRequest accept(
            BattleRuntimePreparationEnvelope preparation,
            BattleClientActionRequest request
    ) {
        Objects.requireNonNull(preparation, "preparation");
        Objects.requireNonNull(request, "request");
        if (!preparation.reservationId().equals(request.reservationId())) {
            throw new IllegalArgumentException("client action request belongs to a different battle reservation");
        }
        if (!preparation.combatants().containsKey(request.actorId())) {
            throw new IllegalArgumentException("client action actor is outside the authoritative roster");
        }

        if (request instanceof BattleClientActionRequest.Move move) {
            List<AuthoritativeMoveMetadata> canonicalMoves = preparation.movesByCombatant().get(move.actorId());
            boolean ownsMove = canonicalMoves.stream().anyMatch(metadata -> metadata.moveId().equals(move.moveId()));
            if (!ownsMove) {
                throw new IllegalArgumentException("requested move is outside the actor canonical loadout");
            }
            if (move.target().mode() == BattleClientActionRequest.Target.Mode.COMBATANT
                    && !preparation.combatants().containsKey(move.target().combatantId())) {
                throw new IllegalArgumentException("requested combatant target is outside the authoritative roster");
            }
        }

        return request;
    }
}
