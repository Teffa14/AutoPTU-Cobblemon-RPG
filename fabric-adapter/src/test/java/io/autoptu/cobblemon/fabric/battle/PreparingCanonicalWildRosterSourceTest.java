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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparingCanonicalWildRosterSourceTest {
    @Test
    void preparesPublishedCanonicalEncounterFromTrustedActorCorrelation() {
        WorldScopedCanonicalWildEncounterBlueprintRegistry blueprints = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        blueprints.register(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                "ouros:forest:encounter-4",
                1,
                List.of(seed("eevee"))
        ));
        WorldScopedWildEncounterCorrelationRegistry correlations = new WorldScopedWildEncounterCorrelationRegistry();
        correlations.register("ouros:forest:encounter-4", "opaque-wild");
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        PreparingCanonicalWildRosterSource source = new PreparingCanonicalWildRosterSource(
                correlations,
                new ServerOwnedWildEncounterPreparationService(blueprints, provisioner)
        );

        var roster = source.resolve("opaque-battle", 1, "opaque-wild").orElseThrow();

        var prepared = provisioner.findByExternalActor("opaque-wild").orElseThrow();
        assertEquals(prepared.canonicalParticipantId(), roster.canonicalParticipantId());
        assertEquals(prepared.pokemon().stream().map(p -> p.pokemonId()).toList(), roster.canonicalPokemonIds());
        assertEquals("eevee", prepared.pokemon().getFirst().speciesId());
    }

    @Test
    void externalActorWithoutServerOwnedCorrelationCannotSelectAnEncounter() {
        WorldScopedCanonicalWildEncounterBlueprintRegistry blueprints = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        blueprints.register(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                "opaque-wild",
                1,
                List.of(seed("zubat"))
        ));
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        PreparingCanonicalWildRosterSource source = new PreparingCanonicalWildRosterSource(
                new WorldScopedWildEncounterCorrelationRegistry(),
                new ServerOwnedWildEncounterPreparationService(blueprints, provisioner)
        );

        assertTrue(source.resolve("opaque-battle", 1, "opaque-wild").isEmpty());
        assertTrue(provisioner.findByExternalActor("opaque-wild").isEmpty());
    }

    @Test
    void sideMismatchFailsClosedAndRollsBackNewProvisioning() {
        WorldScopedCanonicalWildEncounterBlueprintRegistry blueprints = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        blueprints.register(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                "ouros:cave:encounter-9",
                2,
                List.of(seed("geodude"))
        ));
        WorldScopedWildEncounterCorrelationRegistry correlations = new WorldScopedWildEncounterCorrelationRegistry();
        correlations.register("ouros:cave:encounter-9", "opaque-wild");
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        PreparingCanonicalWildRosterSource source = new PreparingCanonicalWildRosterSource(
                correlations,
                new ServerOwnedWildEncounterPreparationService(blueprints, provisioner)
        );

        assertTrue(source.resolve("opaque-battle", 1, "opaque-wild").isEmpty());
        assertTrue(provisioner.findByExternalActor("opaque-wild").isEmpty());
    }

    @Test
    void retryReusesMatchingProvisionedRosterWithoutReplacingCanonicalValues() {
        WorldScopedCanonicalWildEncounterBlueprintRegistry blueprints = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        blueprints.register(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                "ouros:route-3:encounter-21",
                1,
                List.of(seed("pidgey"))
        ));
        WorldScopedWildEncounterCorrelationRegistry correlations = new WorldScopedWildEncounterCorrelationRegistry();
        correlations.register("ouros:route-3:encounter-21", "opaque-wild");
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        PreparingCanonicalWildRosterSource source = new PreparingCanonicalWildRosterSource(
                correlations,
                new ServerOwnedWildEncounterPreparationService(blueprints, provisioner)
        );

        var first = source.resolve("battle-a", 1, "opaque-wild").orElseThrow();
        var second = source.resolve("battle-b", 1, "opaque-wild").orElseThrow();

        assertEquals(first, second);
        assertEquals("pidgey", provisioner.findByExternalActor("opaque-wild").orElseThrow().pokemon().getFirst().speciesId());
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
