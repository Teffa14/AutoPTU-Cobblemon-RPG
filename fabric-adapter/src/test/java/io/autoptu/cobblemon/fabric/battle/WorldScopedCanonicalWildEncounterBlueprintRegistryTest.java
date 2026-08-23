package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import io.autoptu.cobblemon.authority.CanonicalStatusState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldScopedCanonicalWildEncounterBlueprintRegistryTest {
    @Test
    void storesTrustedBlueprintByCanonicalEncounterIdentity() {
        var registry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var blueprint = blueprint("ouros:forest:encounter-7", "pidgey");

        registry.register(blueprint);

        assertEquals(1, registry.size());
        assertEquals(blueprint, registry.resolve("ouros:forest:encounter-7").orElseThrow());
        assertTrue(registry.resolve("  ouros:forest:encounter-7  ").isPresent());
    }

    @Test
    void registrationIsCreateOnlyAndCannotSilentlyReplaceCanonicalValues() {
        var registry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        registry.register(blueprint("ouros:forest:encounter-8", "pidgey"));

        assertThrows(IllegalStateException.class,
                () -> registry.register(blueprint("ouros:forest:encounter-8", "zubat")));
        assertEquals("pidgey", registry.resolve("ouros:forest:encounter-8")
                .orElseThrow().pokemon().getFirst().speciesId());
    }

    @Test
    void removalIsExplicitAndMissingIdentifiersFailClosed() {
        var registry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        registry.register(blueprint("ouros:cave:encounter-2", "zubat"));

        assertFalse(registry.resolve(null).isPresent());
        assertFalse(registry.resolve(" ").isPresent());
        assertFalse(registry.remove("unknown"));
        assertTrue(registry.remove("ouros:cave:encounter-2"));
        assertEquals(0, registry.size());
        assertTrue(registry.resolve("ouros:cave:encounter-2").isEmpty());
    }

    private static CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint blueprint(
            String encounterId,
            String species
    ) {
        return new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                encounterId,
                1,
                List.of(seed(species))
        );
    }

    private static ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint seed(String species) {
        return new ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint(
                species,
                12,
                Set.of("tracker"),
                Set.of(),
                new CanonicalStatusState(List.of()),
                new CanonicalCombatStats(8, 9, 10, 11, 12),
                new CanonicalHealth(37, 41),
                new CanonicalMoveLoadout(List.of("tackle", "quick-attack")),
                new CanonicalBaseMovement(5, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("normal"), List.of("run-away")),
                new CanonicalAccuracyEvasion(0, 0, 0, 0),
                new CanonicalInjuryState(0),
                null,
                7L
        );
    }
}
