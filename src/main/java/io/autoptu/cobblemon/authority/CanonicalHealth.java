package io.autoptu.cobblemon.authority;

/**
 * Persistent server-authoritative HP state for a combatant.
 *
 * Minecraft/Cobblemon health displays may mirror these values, but they are not
 * accepted as the source of truth for PTU battle bootstrap.
 */
public record CanonicalHealth(int currentHp, int maxHp) {
    public CanonicalHealth {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }
        if (currentHp < 0 || currentHp > maxHp) {
            throw new IllegalArgumentException("currentHp must be between 0 and maxHp");
        }
    }
}
