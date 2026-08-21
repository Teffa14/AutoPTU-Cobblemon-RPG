package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRuntimeEnvironmentSeedTest {
    @Test
    void bindsTrustedEnvironmentToExactReservationRosterAndTeams() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        BattleRuntimeEnvironmentSeed seed = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, " Hail ", " Electric Terrain ", Set.of("team-1"), Map.of("mon-1", true));

        assertEquals("Hail", seed.weather());
        assertEquals("Electric Terrain", seed.terrainName());
        assertEquals(Set.of("team-1"), seed.tailwindTeams());
        assertEquals(Map.of("mon-1", true), seed.groundedByCombatant());
        assertEquals(Map.of(), seed.mountedPairs());
        assertFalse(seed.trickRoomOrdering());
        assertFalse(seed.leagueBattleOrdering());
        assertNull(seed.terrainEffect());
        assertEquals(List.of(), seed.zoneEffects());
        assertEquals(List.of(), seed.roomEffects());
        assertThrows(UnsupportedOperationException.class, () -> seed.groundedByCombatant().clear());
    }

    @Test
    void carriesCanonicalInitiativeOrderingModesWithoutAdapterDerivation() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        BattleRuntimeEnvironmentSeed trickRoom = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("mon-1", true), Map.of(), true, false);
        BattleRuntimeEnvironmentSeed league = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("mon-1", true), Map.of(), false, true);

        assertTrue(trickRoom.trickRoomOrdering());
        assertFalse(trickRoom.leagueBattleOrdering());
        assertFalse(league.trickRoomOrdering());
        assertTrue(league.leagueBattleOrdering());
    }

    @Test
    void carriesCanonicalDurationBearingFieldEffectsWithoutMinecraftDerivation() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "move");
        payload.put("priority", 2);
        BattleRuntimeFieldEffectSeed terrain = new BattleRuntimeFieldEffectSeed(
                BattleRuntimeFieldEffectSeed.Kind.TERRAIN, " Electric Terrain ", 3, payload);
        ArrayList<BattleRuntimeFieldEffectSeed> zones = new ArrayList<>();
        zones.add(new BattleRuntimeFieldEffectSeed(BattleRuntimeFieldEffectSeed.Kind.ZONE, "Hazard Zone", 2));
        ArrayList<BattleRuntimeFieldEffectSeed> rooms = new ArrayList<>();
        rooms.add(new BattleRuntimeFieldEffectSeed(BattleRuntimeFieldEffectSeed.Kind.ROOM, "Wonder Room", 1));

        BattleRuntimeEnvironmentSeed seed = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "Electric Terrain", Set.of(), Map.of("mon-1", true),
                Map.of(), false, false, terrain, zones, rooms);
        payload.clear();
        zones.clear();
        rooms.clear();

        assertEquals("Electric Terrain", seed.terrainEffect().name());
        assertEquals(3, seed.terrainEffect().remainingRounds());
        assertEquals(Map.of("source", "move", "priority", 2), seed.terrainEffect().payload());
        assertEquals("Hazard Zone", seed.zoneEffects().getFirst().name());
        assertEquals("Wonder Room", seed.roomEffects().getFirst().name());
        assertThrows(UnsupportedOperationException.class, () -> seed.terrainEffect().payload().clear());
        assertThrows(UnsupportedOperationException.class, () -> seed.zoneEffects().clear());
        assertThrows(UnsupportedOperationException.class, () -> seed.roomEffects().clear());

        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("mon-1", true), Map.of(), false, false,
                new BattleRuntimeFieldEffectSeed(BattleRuntimeFieldEffectSeed.Kind.ROOM, "forged", 1),
                List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("mon-1", true), Map.of(), false, false,
                null,
                List.of(new BattleRuntimeFieldEffectSeed(BattleRuntimeFieldEffectSeed.Kind.TERRAIN, "forged", 1)),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeFieldEffectSeed(
                BattleRuntimeFieldEffectSeed.Kind.ZONE, "bad payload", 1, Map.of("minecraftBlock", List.of("stone"))));
    }

    @Test
    void preservesCanonicalMountedPairOrderAndRejectsLiveEntityForgery() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1", "rider", "mount");
        LinkedHashMap<String, String> pairs = new LinkedHashMap<>();
        pairs.put(" rider ", " mount ");

        BattleRuntimeEnvironmentSeed seed = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(),
                Map.of("rider", true, "mount", true), pairs);
        pairs.clear();

        assertEquals(Map.of("rider", "mount"), seed.mountedPairs());
        assertFalse(seed.trickRoomOrdering());
        assertFalse(seed.leagueBattleOrdering());
        assertThrows(UnsupportedOperationException.class, () -> seed.mountedPairs().clear());
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("rider", true, "mount", true),
                Map.of("rider", "forged-live-entity")));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of("rider", true, "mount", true),
                Map.of("rider", "rider")));
    }

    @Test
    void rejectsOneMountAssignedToMultipleRiders() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1", "rider-1", "rider-2", "mount");
        LinkedHashMap<String, String> pairs = new LinkedHashMap<>();
        pairs.put("rider-1", "mount");
        pairs.put("rider-2", "mount");
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(),
                Map.of("rider-1", true, "rider-2", true, "mount", true), pairs));
    }

    @Test
    void rejectsMissingInjectedOrForeignEnvironmentAuthority() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");

        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-2", runtime, "Hail", "", Set.of(), Map.of("mon-1", true)));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "Hail", "", Set.of(), Map.of("other", true)));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "Hail", "", Set.of("forged-team"), Map.of("mon-1", true)));
    }

    @Test
    void requiresExplicitGroundedStateForEveryReservedCombatant() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of()));
    }

    private static BattleRuntimePreparationEnvelope runtimePreparation(String reservationId, String... ids) {
        String[] combatantIds = ids.length == 0 ? new String[]{"mon-1"} : ids;
        LinkedHashMap<String, RuntimeCombatantMaterializationInput> inputs = new LinkedHashMap<>();
        LinkedHashMap<String, List<AuthoritativeMoveMetadata>> moveInputs = new LinkedHashMap<>();
        LinkedHashMap<String, Set<String>> statusNames = new LinkedHashMap<>();
        LinkedHashMap<String, BattleCombatantStatusStateProjection> statusEntries = new LinkedHashMap<>();

        AuthoritativeMoveMetadata tackle = new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard", true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will");

        int x = 1;
        for (String id : combatantIds) {
            RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                    id,
                    new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(x++, 3)),
                    new BattleCombatantHealthProjection(id, 42, 50),
                    new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                    new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                    new BattleCombatantTraitsProjection(id, List.of("Ice"), List.of("Slush Rush")),
                    new BattleCombatantMoveLoadoutProjection(id, List.of("tackle")),
                    new BattleCombatantAffiliationProjection(id, "team-1", true),
                    new BattleCombatantGeometryProjection(id, "Small"),
                    new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                    Set.of("burned"));
            inputs.put(id, input);
            moveInputs.put(id, List.of(tackle));
            statusNames.put(id, Set.of("burned"));
            statusEntries.put(id, new BattleCombatantStatusStateProjection(id, List.of(new CanonicalStatusEntry("burned"))));
        }

        BattleCoreMaterializationInputProjection materialization =
                new BattleCoreMaterializationInputProjection(reservationId, 123L, inputs);
        BattleCoreMoveCatalogProjection moves =
                new BattleCoreMoveCatalogProjection(reservationId, moveInputs);
        BattleCoreHeldItemBootstrapProjection heldItems =
                new BattleCoreHeldItemBootstrapProjection(reservationId, Map.of());
        BattleCoreBootstrapProjection bootstrap =
                new BattleCoreBootstrapProjection(reservationId, 123L, statusNames);
        BattleCoreStatusStateBootstrapProjection statuses =
                new BattleCoreStatusStateBootstrapProjection(reservationId, bootstrap, statusEntries);

        return BattleRuntimePreparationEnvelope.from(materialization, moves, heldItems, statuses);
    }
}
