package io.autoptu.cobblemon.battlecore;

/**
 * Adapter-neutral platform gateway for rendering already-authoritative battle presentation.
 *
 * A future Fabric/Cobblemon implementation may resolve the opaque presentation entity IDs to live
 * entities and perform visual/world projection. This contract never receives PTU inputs from the
 * platform and never returns battle-rule decisions. HP, relocation endpoints, move identity and cue
 * semantics must already have been resolved and identity-bound before reaching this boundary.
 */
public interface PresentationEntityGateway {
    void animateMove(
            String reservationId,
            String attackerPresentationEntityId,
            String targetPresentationEntityId,
            String moveId
    );

    /**
     * Rich move-presentation path preserving the exact authoritative semantic command.
     * Existing gateways remain source-compatible through the move-id-only fallback.
     */
    default void animateMove(
            String reservationId,
            String attackerPresentationEntityId,
            String targetPresentationEntityId,
            BattlePresentationCommand command
    ) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (command.kind() != BattlePresentationCommand.Kind.MOVE_ANIMATION) {
            throw new IllegalArgumentException("command must be MOVE_ANIMATION");
        }
        String moveId = command.data().get("moveId");
        if (moveId == null || moveId.isBlank()) {
            throw new IllegalArgumentException("MOVE_ANIMATION moveId is required");
        }
        animateMove(
                reservationId,
                attackerPresentationEntityId,
                targetPresentationEntityId,
                moveId.strip()
        );
    }

    void projectDisplayedHealth(
            String reservationId,
            String presentationEntityId,
            int targetHp,
            int damage
    );

    void relocate(
            String reservationId,
            String presentationEntityId,
            WorldBlockCoordinate origin,
            WorldBlockCoordinate destination
    );

    void showCue(
            String reservationId,
            String presentationEntityId,
            BattlePresentationCommand command
    );
}
