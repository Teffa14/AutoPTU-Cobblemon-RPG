package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleArenaGridTransformTest {
    @Test
    void frozenArenaBuildsTheSameReversibleGridTransformForTheWholeBattle() {
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                "minecraft:the_nether", 40, 66, -12, 0, -1, 1, 0);

        BattleGridTransform transform = BattleGridTransform.from(arena);
        BattleGridCoordinate grid = new BattleGridCoordinate(3, -2);
        WorldBlockCoordinate world = transform.toWorld(grid);

        assertEquals(new WorldBlockCoordinate("minecraft:the_nether", 38, 66, -15), world);
        assertEquals(grid, transform.toGrid(world));
        assertEquals(arena, transform.toArenaSnapshot());
    }
}
