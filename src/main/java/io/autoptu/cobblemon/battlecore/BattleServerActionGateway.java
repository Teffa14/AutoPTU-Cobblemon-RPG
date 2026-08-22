package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Server-side handoff from minimal client intent to one exact core-produced legal choice.
 *
 * The request is first checked only for reservation/identity/loadout forgery. The current
 * legal action space is then fetched from the authoritative core, matched exactly, and the
 * selected choice is passed through unchanged for execution.
 */
public final class BattleServerActionGateway {
    private BattleServerActionGateway() {}

    public static BattleCoreLegalChoice execute(
            BattleRuntimePreparationEnvelope preparation,
            BattleClientActionRequest request,
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        Objects.requireNonNull(preparation, "preparation");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(legalChoiceSource, "legalChoiceSource");
        Objects.requireNonNull(executor, "executor");

        BattleClientActionRequest accepted = BattleClientActionRequestGate.accept(preparation, request);
        BattleCoreLegalChoiceSet legalChoices = Objects.requireNonNull(
                legalChoiceSource.legalChoices(accepted.reservationId(), accepted.actorId()),
                "authoritative legal choice source returned null");
        BattleCoreLegalChoice selected = BattleClientLegalChoiceMatcher.select(
                preparation, accepted, legalChoices);
        executor.execute(accepted.reservationId(), selected);
        return selected;
    }
}
