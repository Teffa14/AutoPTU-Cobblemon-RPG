package io.autoptu.cobblemon.authority;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Server-owned canonical state for a Pokémon that participates in an encounter without belonging
 * to a player inventory. This is intentionally separate from Cobblemon entity state.
 */
public record CanonicalEncounterPokemonState(
        String pokemonId,
        String speciesId,
        int level,
        Set<String> capabilities,
        Set<String> statuses,
        CanonicalStatusState statusState,
        CanonicalCombatStats combatStats,
        CanonicalHealth health,
        CanonicalMoveLoadout moveLoadout,
        CanonicalBaseMovement baseMovement,
        CanonicalBattleTraits battleTraits,
        CanonicalAccuracyEvasion accuracyEvasion,
        CanonicalInjuryState injuryState,
        String heldItemInstanceId,
        long revision
) implements CanonicalBattlePokemonView {
    public CanonicalEncounterPokemonState {
        if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId must not be blank");
        if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId must not be blank");
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        statuses = normalizeStatuses(statuses);
        statusState = statusState == null ? CanonicalStatusState.fromNames(statuses) : statusState;
        if (!statusState.names().equals(statuses)) {
            throw new IllegalArgumentException("statusState names must exactly match canonical statuses");
        }
        heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank() ? null : heldItemInstanceId.strip();
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
    }

    private static Set<String> normalizeStatuses(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            normalized.add(value.strip().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }
}
