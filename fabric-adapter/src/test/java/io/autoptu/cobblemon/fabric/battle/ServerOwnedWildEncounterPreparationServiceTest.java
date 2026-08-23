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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildEncounterPreparationServiceTest {
    @Test
    void resolvesCanonicalBlueprintBeforeAttachingOpaqueExternalActor() {
        AtomicReference<String> requestedEncounterId = new AtomicReference<>();
        CanonicalWildEncounterBlueprintSource source = encounterId -> {
            requestedEncounterId.set(encounterId);
            return Optional.of(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                    encounterId,
                    2,
                    List.of(seed("pidgey"))
            ));
        };
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        ServerOwnedWildEncounterPreparationService service = new ServerOwnedWildEncounterPreparationService(source, provisioner);

        var prepared = service.prepare("ouros:route-3:encounter-21", "opaque-cobblemon-wild-uuid").orElseThrow();

        assertEquals("ouros:route-3:encounter-21", requestedEncounterId.get());
        assertEquals("opaque-cobblemon-wild-uuid", prepared.externalWildActorId());
        assertEquals(2, prepared.side());
        assertEquals("pidgey", prepared.pokemon().getFirst().speciesId());
        assertTrue(provisioner.findByExternalActor("opaque-cobblemon-wild-uuid").isPresent());
    }

    @Test
    void changingExternalActorDoesNotChangeCanonicalIdentityOrValues() {
        CanonicalWildEncounterBlueprintSource source = encounterId -> Optional.of(
                new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                        encounterId,
                        1,
                        List.of(seed("eevee"))
                )
        );
        var first = new ServerOwnedWildEncounterPreparationService(
                source,
                new ServerOwnedWildEncounterProvisioningService()
        ).prepare("ouros:forest:encounter-4", "external-a").orElseThrow();
        var second = new ServerOwnedWildEncounterPreparationService(
                source,
                new ServerOwnedWildEncounterProvisioningService()
        ).prepare("ouros:forest:encounter-4", "external-b").orElseThrow();

        assertEquals(first.canonicalParticipantId(), second.canonicalParticipantId());
        assertEquals(first.pokemon(), second.pokemon());
        assertEquals(first.deterministicSeed(), second.deterministicSeed());
        assertFalse(first.externalWildActorId().equals(second.externalWildActorId()));
    }

    @Test
    void missingCanonicalEncounterFailsClosedWithoutProvisioning() {
        ServerOwnedWildEncounterProvisioningService provisioner = new ServerOwnedWildEncounterProvisioningService();
        ServerOwnedWildEncounterPreparationService service = new ServerOwnedWildEncounterPreparationService(
                encounterId -> Optional.empty(),
                provisioner
        );

        assertTrue(service.prepare("ouros:missing", "opaque-wild").isEmpty());
        assertTrue(provisioner.findByExternalActor("opaque-wild").isEmpty());
    }

    @Test
    void rejectsConfusedDeputyBlueprintWithDifferentCanonicalEncounterId() {
        ServerOwnedWildEncounterPreparationService service = new ServerOwnedWildEncounterPreparationService(
                encounterId -> Optional.of(new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                        "ouros:different-encounter",
                        1,
                        List.of(seed("zubat"))
                )),
                new ServerOwnedWildEncounterProvisioningService()
        );

        assertThrows(IllegalStateException.class, () -> service.prepare("ouros:requested-encounter", "opaque-wild"));
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
