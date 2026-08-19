package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;

/**
 * Immutable projection of persistent server-owned PTU base movement for one reserved combatant.
 *
 * This DTO deliberately stops before AutoPTU-Java MovementProfile resolution. Sprint,
 * Wallrunner, Naturewalk, statuses, abilities, weather, equipment, Trainer Features,
 * canFly/canSwim/canBurrow/canPhase, Liquefied, rough-terrain bypass, and every other
 * battle-scoped modifier remain authoritative battle-core concerns.
 */
public record BattleCombatantBaseMovementProjection(
        String combatantId,
        int overland,
        int swim,
        int sky,
        int longJump,
        int highJump
) {
    public BattleCombatantBaseMovementProjection {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        requireNonNegative("overland", overland);
        requireNonNegative("swim", swim);
        requireNonNegative("sky", sky);
        requireNonNegative("longJump", longJump);
        requireNonNegative("highJump", highJump);
    }

    public static BattleCombatantBaseMovementProjection from(BattlePokemonSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        CanonicalBaseMovement movement = snapshot.baseMovement();
        if (movement == null) {
            throw new IllegalArgumentException(
                    "canonical base movement is required for combatant: " + snapshot.pokemonId()
            );
        }
        return new BattleCombatantBaseMovementProjection(
                snapshot.pokemonId(),
                movement.overland(),
                movement.swim(),
                movement.sky(),
                movement.longJump(),
                movement.highJump()
        );
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }
}
