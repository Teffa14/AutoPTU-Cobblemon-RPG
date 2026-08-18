package io.autoptu.cobblemon.authority;

/**
 * Project-owned immutable PTU combat stat snapshot.
 *
 * Values are canonical server-side PTU stats. Minecraft/Cobblemon adapters may
 * observe entity data, but they must not supply trusted battle modifiers through
 * this record.
 */
public record CanonicalCombatStats(
        int atk,
        int def,
        int spatk,
        int spdef,
        int spd
) {
    public CanonicalCombatStats {
        requirePositive("atk", atk);
        requirePositive("def", def);
        requirePositive("spatk", spatk);
        requirePositive("spdef", spdef);
        requirePositive("spd", spd);
    }

    private static void requirePositive(String name, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be >= 1");
        }
    }
}
