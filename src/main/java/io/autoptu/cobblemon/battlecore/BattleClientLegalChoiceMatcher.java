package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;

/**
 * Selects an already-authoritative legal choice for a minimal client request.
 *
 * This matcher performs no PTU legality calculation. If AutoPTU-Java did not emit a
 * matching choice for the current action-space snapshot, the request fails closed.
 */
public final class BattleClientLegalChoiceMatcher {
    private BattleClientLegalChoiceMatcher() {}

    public static BattleCoreLegalChoice select(
            BattleRuntimePreparationEnvelope preparation,
            BattleClientActionRequest request,
            BattleCoreLegalChoiceSet legalChoices
    ) {
        Objects.requireNonNull(legalChoices, "legalChoices");
        BattleClientActionRequest accepted = BattleClientActionRequestGate.accept(preparation, request);
        if (!accepted.reservationId().equals(legalChoices.reservationId())) {
            throw new IllegalArgumentException("legal choices belong to a different battle reservation");
        }
        if (!accepted.actorId().equals(legalChoices.actorId())) {
            throw new IllegalArgumentException("legal choices belong to a different actor");
        }

        List<BattleCoreLegalChoice> matches = legalChoices.choices().stream()
                .filter(choice -> matches(accepted, choice))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("requested action is absent from the authoritative legal choice space");
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("requested action is ambiguous in the authoritative legal choice space");
        }
        return matches.getFirst();
    }

    private static boolean matches(BattleClientActionRequest request, BattleCoreLegalChoice choice) {
        if (request instanceof BattleClientActionRequest.Shift requestedShift
                && choice instanceof BattleCoreLegalChoice.Shift legalShift) {
            return requestedShift.actorId().equals(legalShift.actorId())
                    && requestedShift.destination().equals(legalShift.destination());
        }
        if (request instanceof BattleClientActionRequest.Move requestedMove
                && choice instanceof BattleCoreLegalChoice.Move legalMove) {
            if (!requestedMove.actorId().equals(legalMove.actorId())
                    || !requestedMove.moveId().equals(legalMove.moveId())
                    || requestedMove.target().mode() != legalMove.targetMode()) {
                return false;
            }
            return switch (requestedMove.target().mode()) {
                case COMBATANT -> requestedMove.target().combatantId().equals(legalMove.targetId());
                case TILE -> requestedMove.target().tile().equals(legalMove.targetAnchor());
                case SELF, FIELD -> true;
            };
        }
        return false;
    }
}
