package io.autoptu.cobblemon.battlecore;

/**
 * Platform-specific presentation operations over already-resolved live entity handles.
 *
 * Implementations may call Fabric/Cobblemon APIs, but they only receive presentation outputs. They
 * must never return PTU decisions or derive stats, legality, targeting, initiative or battle results.
 */
public interface PresentationEntityPlatformBackend<T> {
    void animateMove(T attacker, T target, String moveId);

    void projectDisplayedHealth(T entity, int targetHp, int damage);

    void relocate(T entity, WorldBlockCoordinate origin, WorldBlockCoordinate destination);

    void showCue(T entity, BattlePresentationCommand command);
}
