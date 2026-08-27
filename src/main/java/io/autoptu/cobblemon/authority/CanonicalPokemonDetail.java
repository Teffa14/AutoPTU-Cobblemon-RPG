package io.autoptu.cobblemon.authority;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Player-visible read model built only from the durable canonical Pokemon aggregate. */
public record CanonicalPokemonDetail(
        int slot,
        String pokemonId,
        String speciesId,
        int level,
        CanonicalHealth health,
        List<String> statuses,
        CanonicalCombatStats combatStats,
        CanonicalMoveLoadout moveLoadout,
        CanonicalBaseMovement baseMovement,
        CanonicalBattleTraits battleTraits,
        CanonicalAccuracyEvasion accuracyEvasion,
        CanonicalInjuryState injuryState,
        boolean heldItemEquipped,
        List<String> capabilities,
        long revision
) {
    public CanonicalPokemonDetail {
        if (slot < 1) throw new IllegalArgumentException("slot must be >= 1");
        if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId is required");
        if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        statuses = sorted(statuses == null ? Set.of() : Set.copyOf(statuses));
        capabilities = sorted(capabilities == null ? Set.of() : Set.copyOf(capabilities));
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
