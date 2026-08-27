package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import io.autoptu.cobblemon.authority.CanonicalStatusState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEncounterPartyHandoffServiceTest {
    @Test
    void freezesCanonicalPartyAndExactVisibleWildBlueprint() {
        var blueprint = blueprint("encounter-1", "sentret", 11);
        var service = new WorldEncounterPartyHandoffService(id -> Optional.of(blueprint));
        var party = new ArrayList<>(List.of("pokemon-a", "pokemon-b"));
        var inventory = new LinkedHashMap<String, Integer>();
        inventory.put("potion-1", 2);
        var context = new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                "player-1", party, inventory, arena());

        var decision = service.reserve(request("encounter-1", "player-1", "actor-1"), context);
        assertTrue(decision.created());
        assertEquals(List.of("pokemon-a", "pokemon-b"), decision.reservation().canonicalPlayerPokemonIds());
        assertEquals(blueprint, decision.reservation().wildBlueprint());
        assertEquals("actor-1", decision.reservation().externalWildActorId());

        party.clear();
        inventory.put("potion-1", 99);
        assertEquals(List.of("pokemon-a", "pokemon-b"), decision.reservation().canonicalPlayerPokemonIds());
        assertEquals(2, decision.reservation().consumableQuantities().get("potion-1"));
        assertThrows(UnsupportedOperationException.class,
                () -> decision.reservation().canonicalPlayerPokemonIds().add("pokemon-c"));
        assertThrows(UnsupportedOperationException.class,
                () -> decision.reservation().consumableQuantities().put("potion-2", 1));
    }

    @Test
    void missingBlueprintAndMismatchedPlayerFailClosedWithoutReservation() {
        var missing = new WorldEncounterPartyHandoffService(id -> Optional.empty());
        var context = new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                "player-1", List.of("pokemon-a"), Map.of(), arena());
        var missingDecision = missing.reserve(request("encounter-1", "player-1", "actor-1"), context);
        assertEquals(WorldEncounterPartyHandoffService.Outcome.WILD_BLUEPRINT_MISSING, missingDecision.outcome());
        assertFalse(missing.findByEncounterId("encounter-1").isPresent());

        var blueprint = blueprint("encounter-1", "sentret", 11);
        var mismatch = new WorldEncounterPartyHandoffService(id -> Optional.of(blueprint));
        var otherPlayer = new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                "player-2", List.of("pokemon-b"), Map.of(), arena());
        var mismatchDecision = mismatch.reserve(request("encounter-1", "player-1", "actor-1"), otherPlayer);
        assertEquals(WorldEncounterPartyHandoffService.Outcome.PLAYER_CONTEXT_MISMATCH, mismatchDecision.outcome());
        assertFalse(mismatch.findByPlayerId("player-1").isPresent());
    }

    @Test
    void encounterAndPlayerCannotBeSilentlyReboundUntilReleased() {
        var firstBlueprint = blueprint("encounter-1", "sentret", 11);
        var secondBlueprint = blueprint("encounter-2", "hoppip", 10);
        var service = new WorldEncounterPartyHandoffService(id -> Optional.of(
                id.equals("encounter-1") ? firstBlueprint : secondBlueprint));
        var context = new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                "player-1", List.of("pokemon-a"), Map.of(), arena());

        var first = service.reserve(request("encounter-1", "player-1", "actor-1"), context);
        var duplicate = service.reserve(request("encounter-2", "player-1", "actor-2"), context);
        assertTrue(first.created());
        assertEquals(WorldEncounterPartyHandoffService.Outcome.ALREADY_RESERVED, duplicate.outcome());
        assertEquals("encounter-1", duplicate.reservation().canonicalEncounterId());

        assertTrue(service.release("encounter-1"));
        assertTrue(service.reserve(request("encounter-2", "player-1", "actor-2"), context).created());
    }

    private static WorldEncounterTriggerRequestService.Request request(String encounterId, String playerId, String actorId) {
        return new WorldEncounterTriggerRequestService.Request(
                encounterId, playerId, actorId, "cedar_meadow", "visible_roaming_wild",
                "minecraft:overworld", 10, 64, 12, 1234L);
    }

    private static CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint blueprint(
            String encounterId, String species, int level) {
        return new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                encounterId, 1, List.of(seed(species, level)));
    }

    private static ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint seed(String species, int level) {
        return new ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint(
                species,
                level,
                Set.of("tracker"),
                Set.of(),
                new CanonicalStatusState(List.of()),
                new CanonicalCombatStats(8, 9, 10, 11, 12),
                new CanonicalHealth(37, 41),
                new CanonicalMoveLoadout(List.of("tackle")),
                new CanonicalBaseMovement(5, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("normal"), List.of("run-away")),
                new CanonicalAccuracyEvasion(0, 0, 0, 0),
                new CanonicalInjuryState(0),
                null,
                7L
        );
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 31, 80, 31, List.of());
    }
}
