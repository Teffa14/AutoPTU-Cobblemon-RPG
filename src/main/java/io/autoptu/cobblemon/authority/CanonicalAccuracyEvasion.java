package io.autoptu.cobblemon.authority;

/**
 * Persistent server-owned PTU accuracy/evasion inputs.
 *
 * These values mirror the Python oracle's PokemonSpec accuracy_cs, evasion_phys,
 * evasion_spec, and evasion_spd fields. They are baseline inputs only: abilities,
 * items, Trainer Features, statuses, terrain and temporary effects remain battle-core
 * responsibilities and must not be collapsed into this persistent record.
 */
public record CanonicalAccuracyEvasion(
        int accuracyStage,
        int physicalEvasionBonus,
        int specialEvasionBonus,
        int statusEvasionBonus
) {
    public CanonicalAccuracyEvasion {
        if (accuracyStage < -6 || accuracyStage > 6) {
            throw new IllegalArgumentException("accuracyStage must be between -6 and 6");
        }
    }
}
