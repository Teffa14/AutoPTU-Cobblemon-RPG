package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Routes a validated entity-bound presentation stream to an adapter consumer in authoritative order.
 *
 * This class performs no timing, animation policy, entity lookup, PTU calculation, or world mutation.
 * Those concerns belong to the adapter implementation after this identity-safe boundary.
 */
public final class BattleEntityBoundPresentationDispatcher {
    private BattleEntityBoundPresentationDispatcher() {}

    public static void dispatch(
            BattleEntityBoundPresentationStream stream,
            BattleEntityBoundPresentationConsumer consumer
    ) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(consumer, "consumer");

        for (EntityBoundPresentationOutput output : stream.outputs()) {
            if (output instanceof EntityBoundMoveAnimation animation) {
                consumer.animateMove(stream.reservationId(), animation);
            } else if (output instanceof EntityBoundBattleHealthProjection health) {
                consumer.projectHealth(stream.reservationId(), health);
            } else if (output instanceof EntityBoundBattleWorldRelocation relocation) {
                consumer.relocateEntity(stream.reservationId(), relocation);
            } else if (output instanceof EntityBoundBattlePresentationCommand cue) {
                consumer.showCombatantCue(stream.reservationId(), cue);
            } else {
                throw new IllegalStateException("unsupported presentation output: " + output.getClass().getName());
            }
        }
    }
}
