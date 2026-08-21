package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * First concrete adapter-facing consumer for the validated entity-bound presentation stream.
 *
 * This class only unwraps already-authoritative presentation values and forwards them to a platform
 * gateway. It does not look up PTU state, select targets, calculate damage, decide movement legality,
 * alter initiative, resolve statuses, execute hooks, or mutate canonical persistence.
 */
public final class GatewayBackedBattleEntityBoundPresentationConsumer
        implements BattleEntityBoundPresentationConsumer {
    private final PresentationEntityGateway gateway;

    public GatewayBackedBattleEntityBoundPresentationConsumer(PresentationEntityGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public void animateMove(String reservationId, EntityBoundMoveAnimation animation) {
        Objects.requireNonNull(animation, "animation");
        gateway.animateMove(
                requireReservationId(reservationId),
                animation.attackerPresentationEntityId(),
                animation.targetPresentationEntityId(),
                animation.moveId()
        );
    }

    @Override
    public void projectHealth(String reservationId, EntityBoundBattleHealthProjection health) {
        Objects.requireNonNull(health, "health");
        gateway.projectDisplayedHealth(
                requireReservationId(reservationId),
                health.presentationEntityId(),
                health.targetHp(),
                health.damage()
        );
    }

    @Override
    public void relocateEntity(String reservationId, EntityBoundBattleWorldRelocation relocation) {
        Objects.requireNonNull(relocation, "relocation");
        gateway.relocate(
                requireReservationId(reservationId),
                relocation.presentationEntityId(),
                relocation.origin(),
                relocation.destination()
        );
    }

    @Override
    public void showCombatantCue(String reservationId, EntityBoundBattlePresentationCommand cue) {
        Objects.requireNonNull(cue, "cue");
        gateway.showCue(
                requireReservationId(reservationId),
                cue.presentationEntityId(),
                cue.command()
        );
    }

    private static String requireReservationId(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        return reservationId.strip();
    }
}
