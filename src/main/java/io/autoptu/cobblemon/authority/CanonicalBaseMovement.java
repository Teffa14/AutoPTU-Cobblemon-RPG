package io.autoptu.cobblemon.authority;

/**
 * Persistent server-owned PTU movement values before battle-scoped effects are resolved.
 *
 * These values deliberately exclude sprint, Wallrunner, Naturewalk, Liquefied, abilities,
 * statuses, weather, equipment, Trainer Features and other runtime modifiers. AutoPTU-Java
 * remains responsible for resolving those effects into its battle-time MovementProfile.
 */
public record CanonicalBaseMovement(
        int overland,
        int swim,
        int sky,
        int longJump,
        int highJump
) {
    public CanonicalBaseMovement {
        if (overland < 0 || swim < 0 || sky < 0 || longJump < 0 || highJump < 0) {
            throw new IllegalArgumentException("base movement values must be >= 0");
        }
    }
}
