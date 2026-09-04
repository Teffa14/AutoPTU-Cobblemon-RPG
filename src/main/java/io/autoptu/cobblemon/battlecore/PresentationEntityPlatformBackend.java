package io.autoptu.cobblemon.battlecore;

/**
 * Platform-specific presentation operations over already-resolved live entity handles.
 *
 * Implementations may call Fabric/Cobblemon APIs, but they only receive presentation outputs. They
 * must never return PTU decisions or derive stats, legality, targeting, initiative or battle results.
 */
public interface PresentationEntityPlatformBackend<T> {
    void animateMove(T attacker, T target, String moveId);

    /**
     * Rich move-presentation path that preserves the exact already-authoritative move command.
     *
     * The default keeps existing platform backends source-compatible and projects only the move id.
     * Backends that can visually distinguish explicit semantic outputs such as hit/crit may override
     * this method. They must never calculate those values locally.
     */
    default void animateMove(T attacker, T target, BattlePresentationCommand command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (command.kind() != BattlePresentationCommand.Kind.MOVE_ANIMATION) {
            throw new IllegalArgumentException("command must be MOVE_ANIMATION");
        }
        String moveId = command.data().get("moveId");
        if (moveId == null || moveId.isBlank()) {
            throw new IllegalArgumentException("MOVE_ANIMATION moveId is required");
        }
        animateMove(attacker, target, moveId.strip());
    }

    void projectDisplayedHealth(T entity, int targetHp, int damage);

    void relocate(T entity, WorldBlockCoordinate origin, WorldBlockCoordinate destination);

    void showCue(T entity, BattlePresentationCommand command);
}
