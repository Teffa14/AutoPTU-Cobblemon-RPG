package io.autoptu.cobblemon.battlecore;

/**
 * Adapter-facing sink for one already-authoritative, reservation-scoped combatant presentation stream.
 *
 * Implementations may locate/render Minecraft or Cobblemon entities from the opaque presentation IDs,
 * but must not recalculate PTU legality, damage, movement, status, initiative, ability, item, or Feature
 * behavior. Every value accepted here has already been resolved and identity-bound upstream.
 */
public interface BattleEntityBoundPresentationConsumer {
    void animateMove(String reservationId, EntityBoundMoveAnimation animation);

    void projectHealth(String reservationId, EntityBoundBattleHealthProjection health);

    void relocateEntity(String reservationId, EntityBoundBattleWorldRelocation relocation);

    void showCombatantCue(String reservationId, EntityBoundBattlePresentationCommand cue);
}
