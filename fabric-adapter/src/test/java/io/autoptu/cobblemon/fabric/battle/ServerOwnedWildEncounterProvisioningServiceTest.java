package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleParticipantKind;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildEncounterProvisioningServiceTest {
    @Test
    void canonicalIdsAndSeedDependOnCanonicalEncounterDataNotExternalActorIdentity() {
        ServerOwnedWildEncounterProvisioningService first = new ServerOwnedWildEncounterProvisioningService();
        ServerOwnedWildEncounterProvisioningService second = new ServerOwnedWildEncounterProvisioningService();

        var a = first.provision("ouros:route-3:encounter-17", "cobblemon-wild-a", 2, List.of(seed("pidgey")));
        var b = second.provision("ouros:route-3:encounter-17", "totally-different-external-id", 2, List.of(seed("pidgey")));

        assertEquals(a.canonicalParticipantId(), b.canonicalParticipantId());
        assertEquals(a.pokemon().getFirst().pokemonId(), b.pokemon().getFirst().pokemonId());
        assertEquals(a.deterministicSeed(), b.deterministicSeed());
        assertNotEquals(a.externalWildActorId(), b.externalWildActorId());
        assertFalse(a.canonicalParticipantId().contains(a.externalWildActorId()));
        assertFalse(a.pokemon().getFirst().pokemonId().contains(a.externalWildActorId()));
    }

    @Test
    void exposesProvisionedRosterToIdentityBinderAndCanonicalReservationLookup() {
        ServerOwnedWildEncounterProvisioningService service = new ServerOwnedWildEncounterProvisioningService();
        var provisioned = service.provision(
                "ouros:forest:encounter-9",
                "external-wild-actor",
                1,
                List.of(seed("caterpie"), seed("weedle"))
        );

        var roster = service.resolve("cobblemon-battle-uuid", 1, "external-wild-actor").orElseThrow();
        assertEquals(provisioned.canonicalParticipantId(), roster.canonicalParticipantId());
        assertEquals(provisioned.pokemon().stream().map(p -> p.pokemonId()).toList(), roster.canonicalPokemonIds());

        var first = provisioned.pokemon().getFirst();
        assertEquals(
                first,
                service.findCombatant(BattleParticipantKind.WILD, provisioned.canonicalParticipantId(), first.pokemonId())
                        .orElseThrow()
        );
        assertTrue(service.findCombatant(
                BattleParticipantKind.PLAYER,
                provisioned.canonicalParticipantId(),
                first.pokemonId()
        ).isEmpty());
    }

    @Test
    void rejectsWrongSideUnknownActorAndDuplicateProvisioning() {
        ServerOwnedWildEncounterProvisioningService service = new ServerOwnedWildEncounterProvisioningService();
        service.provision("ouros:cave:encounter-2", "external-wild", 3, List.of(seed("zubat")));

        assertTrue(service.resolve("battle", 2, "external-wild").isEmpty());
        assertTrue(service.resolve("battle", 3, "unknown-wild").isEmpty());
        assertThrows(IllegalStateException.class, () ->
                service.provision("ouros:cave:encounter-3", "external-wild", 3, List.of(seed("geodude"))));
        assertThrows(IllegalStateException.class, () ->
                service.provision("ouros:cave:encounter-2", "another-external-wild", 3, List.of(seed("zubat"))));
    }

    @Test
    void releaseRemovesBothBindingAndCanonicalCombatants() {
        ServerOwnedWildEncounterProvisioningService service = new ServerOwnedWildEncounterProvisioningService();
        var provisioned = service.provision("ouros:lake:encounter-4", "external-magikarp", 5, List.of(seed("magikarp")));
        String pokemonId = provisioned.pokemon().getFirst().pokemonId();

        assertTrue(service.release("external-magikarp"));
        assertTrue(service.findByExternalActor("external-magikarp").isEmpty());
        assertTrue(service.resolve("battle", 5, "external-magikarp").isEmpty());
        assertTrue(service.findCombatant(BattleParticipantKind.WILD, provisioned.canonicalParticipantId(), pokemonId).isEmpty());
        assertFalse(service.release("external-magikarp"));
    }

    @Test
    void canonicalPokemonValuesComeFromServerBlueprint() {
        ServerOwnedWildEncounterProvisioningService service = new ServerOwnedWildEncounterProvisioningService();
        var provisioned = service.provision("ouros:test:encounter", "opaque-external", 1, List.of(seed("eevee")));
        var pokemon = provisioned.pokemon().getFirst();

        assertEquals("eevee", pokemon.speciesId());
        assertEquals(12, pokemon.level());
        assertEquals(new CanonicalHealth(37, 41), pokemon.health());
        assertEquals(List.of("tackle", "quick-attack"), pokemon.moveLoadout().moveIds());
        assertEquals(List.of("normal"), pokemon.battleTraits().types());
        assertEquals(List.of("run-away"), pokemon.battleTraits().abilities());
        assertEquals(7L, pokemon.revision());
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
