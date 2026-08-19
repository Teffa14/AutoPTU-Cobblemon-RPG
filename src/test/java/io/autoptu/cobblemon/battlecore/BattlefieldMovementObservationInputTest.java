package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlefieldMovementObservationInputTest {
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 100, 64, 200,
            1, 0, 0, 1
    );

    @Test
    void projectsOnlyAdapterNeutralPhysicalFacts() {
        BattleGridTransform transform = BattleGridTransform.from(ARENA);
        BattleGridCoordinate dryGrid = new BattleGridCoordinate(0, 0);
        BattleGridCoordinate wetGrid = new BattleGridCoordinate(1, 0);
        BattlefieldWorldSnapshot world = new BattlefieldWorldSnapshot(
                "battle-24",
                ARENA,
                List.of(
                        new WorldTileObservation(
                                dryGrid, transform.toWorld(dryGrid), 63,
                                "minecraft:grass_block", "", true, false, false),
                        new WorldTileObservation(
                                wetGrid, transform.toWorld(wetGrid), 61,
                                "minecraft:water", "minecraft:water", false, false, true)
                )
        );

        BattlefieldMovementObservationInput input = BattlefieldMovementObservationInput.from(world);

        assertEquals("battle-24", input.reservationId());
        assertEquals(2, input.tilesByCoordinate().size());
        assertEquals(63, input.tile(dryGrid).observedSurfaceY());
        assertTrue(input.tile(dryGrid).collisionShapePresent());
        assertFalse(input.tile(dryGrid).fluidPresent());
        assertTrue(input.tile(wetGrid).fluidPresent());
        assertTrue(input.tile(wetGrid).replaceableAtAnchor());
        assertThrows(UnsupportedOperationException.class,
                () -> input.tilesByCoordinate().put(dryGrid, input.tile(dryGrid)));
    }

    @Test
    void coreFacingTileContractContainsNoMinecraftIdentifiersOrPtuLegalityFields() {
        Set<String> componentNames = Arrays.stream(CoreMovementTileObservation.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "gridCoordinate",
                "observedSurfaceY",
                "collisionShapePresent",
                "airAtAnchor",
                "fluidPresent",
                "replaceableAtAnchor"
        ), componentNames);
        assertFalse(componentNames.contains("blockStateId"));
        assertFalse(componentNames.contains("fluidStateId"));
        assertFalse(componentNames.contains("terrainCost"));
        assertFalse(componentNames.contains("traversable"));
        assertFalse(componentNames.contains("hazard"));
    }

    @Test
    void rejectsForgedCoordinateKeyAndUnknownLookup() {
        BattleGridCoordinate embedded = new BattleGridCoordinate(2, 3);
        CoreMovementTileObservation observation = new CoreMovementTileObservation(
                embedded, 64, true, false, false, false);

        assertThrows(IllegalArgumentException.class, () -> new BattlefieldMovementObservationInput(
                "battle-24",
                ARENA,
                Map.of(new BattleGridCoordinate(9, 9), observation)
        ));

        BattlefieldMovementObservationInput valid = new BattlefieldMovementObservationInput(
                "battle-24", ARENA, Map.of(embedded, observation));
        assertThrows(IllegalArgumentException.class,
                () -> valid.tile(new BattleGridCoordinate(0, 0)));
    }
}
