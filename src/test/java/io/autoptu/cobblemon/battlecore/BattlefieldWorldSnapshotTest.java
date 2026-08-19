package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlefieldWorldSnapshotTest {
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 100, 64, 200,
            1, 0, 0, 1
    );

    @Test
    void freezesRawWorldFactsWithoutDerivingPtuLegality() {
        BattleGridTransform transform = BattleGridTransform.from(ARENA);
        WorldTileObservation first = new WorldTileObservation(
                new BattleGridCoordinate(0, 0),
                transform.toWorld(new BattleGridCoordinate(0, 0)),
                63,
                "minecraft:grass_block",
                "",
                true,
                false,
                false
        );
        WorldTileObservation second = new WorldTileObservation(
                new BattleGridCoordinate(1, 0),
                transform.toWorld(new BattleGridCoordinate(1, 0)),
                61,
                "minecraft:water",
                "minecraft:water",
                false,
                false,
                true
        );
        ArrayList<WorldTileObservation> source = new ArrayList<>(List.of(first, second));

        BattlefieldWorldSnapshot snapshot = new BattlefieldWorldSnapshot("battle-1", ARENA, source);
        source.clear();

        assertEquals("battle-1", snapshot.reservationId());
        assertEquals(2, snapshot.tiles().size());
        assertEquals(61, snapshot.tiles().get(1).observedSurfaceY());
        assertTrue(snapshot.tiles().get(1).hasFluidObservation());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.tiles().add(first));
    }

    @Test
    void rejectsObservationThatDoesNotMatchFrozenArenaTransform() {
        WorldTileObservation injected = new WorldTileObservation(
                new BattleGridCoordinate(2, 3),
                new WorldBlockCoordinate("minecraft:overworld", 999, 64, 999),
                64,
                "minecraft:stone",
                "",
                true,
                false,
                false
        );

        assertThrows(IllegalArgumentException.class,
                () -> new BattlefieldWorldSnapshot("battle-1", ARENA, List.of(injected)));
    }

    @Test
    void rejectsDuplicateGridObservationsEvenWhenPayloadDiffers() {
        BattleGridCoordinate grid = new BattleGridCoordinate(2, 3);
        WorldBlockCoordinate anchor = BattleGridTransform.from(ARENA).toWorld(grid);
        WorldTileObservation first = new WorldTileObservation(
                grid, anchor, 64, "minecraft:stone", "", true, false, false);
        WorldTileObservation duplicate = new WorldTileObservation(
                grid, anchor, 65, "minecraft:dirt", "", true, false, false);

        assertThrows(IllegalArgumentException.class,
                () -> new BattlefieldWorldSnapshot("battle-1", ARENA, List.of(first, duplicate)));
    }

    @Test
    void tileObservationNormalizesIdentifiersButPreservesObservedFacts() {
        WorldTileObservation observation = new WorldTileObservation(
                new BattleGridCoordinate(0, 0),
                BattleGridTransform.from(ARENA).toWorld(new BattleGridCoordinate(0, 0)),
                62,
                "  minecraft:mud  ",
                "  minecraft:water  ",
                false,
                true,
                true
        );

        assertEquals("minecraft:mud", observation.blockStateId());
        assertEquals("minecraft:water", observation.fluidStateId());
        assertEquals(62, observation.observedSurfaceY());
        assertTrue(observation.airAtAnchor());
        assertTrue(observation.replaceableAtAnchor());
    }
}
