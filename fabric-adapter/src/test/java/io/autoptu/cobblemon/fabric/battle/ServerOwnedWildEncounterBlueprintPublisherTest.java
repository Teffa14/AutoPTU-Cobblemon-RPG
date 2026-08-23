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

class ServerOwnedWildEncounterBlueprintPublisherTest {
    @Test
    void publishesTrustedCampaignBlueprintBeforeAnyExternalIdentityExists() {
        var worldRegistry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var requested = new AtomicReference<String>();
        var expected = blueprint("ouros:route-1:encounter-12", "sentret");
        CanonicalWildEncounterBlueprintSource campaignSource = encounterId -> {
            requested.set(encounterId);
            return Optional.of(expected);
        };
        var publisher = new ServerOwnedWildEncounterBlueprintPublisher(campaignSource, worldRegistry);

        assertTrue(publisher.publish("  ouros:route-1:encounter-12  "));

        assertEquals("ouros:route-1:encounter-12", requested.get());
        assertEquals(expected, worldRegistry.resolve("ouros:route-1:encounter-12").orElseThrow());
        assertEquals(1, worldRegistry.size());
    }

    @Test
    void missingCampaignStateFailsClosedWithoutPartialRegistration() {
        var worldRegistry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var publisher = new ServerOwnedWildEncounterBlueprintPublisher(
                encounterId -> Optional.empty(),
                worldRegistry
        );

        assertFalse(publisher.publish("ouros:route-1:missing"));
        assertEquals(0, worldRegistry.size());
    }

    @Test
    void confusedDeputyCampaignResponseIsRejected() {
        var worldRegistry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var publisher = new ServerOwnedWildEncounterBlueprintPublisher(
                encounterId -> Optional.of(blueprint("ouros:wrong-encounter", "zubat")),
                worldRegistry
        );

        assertThrows(IllegalStateException.class,
                () -> publisher.publish("ouros:route-1:encounter-13"));
        assertEquals(0, worldRegistry.size());
    }

    @Test
    void duplicatePublicationCannotReplaceFirstCanonicalDecision() {
        var worldRegistry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var current = new AtomicReference<>(blueprint("ouros:route-1:encounter-14", "sentret"));
        var publisher = new ServerOwnedWildEncounterBlueprintPublisher(
                encounterId -> Optional.of(current.get()),
                worldRegistry
        );

        assertTrue(publisher.publish("ouros:route-1:encounter-14"));
        current.set(blueprint("ouros:route-1:encounter-14", "zubat"));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish("ouros:route-1:encounter-14"));
        assertEquals("sentret", worldRegistry.resolve("ouros:route-1:encounter-14")
                .orElseThrow().pokemon().getFirst().speciesId());
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
