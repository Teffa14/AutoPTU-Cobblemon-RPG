package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FabricBattleCameraRuntimeTest {
    @Test
    void cameraFocusUsesOnlyServerOwnedArenaAnchor() {
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                "minecraft:overworld",
                12,
                64,
                -8,
                1,
                0,
                0,
                1
        );

        FabricBattleCameraRuntime.CameraFocus focus = FabricBattleCameraRuntime.focusPoint(arena);

        assertEquals(12.5D, focus.x());
        assertEquals(65.0D, focus.y());
        assertEquals(-7.5D, focus.z());
    }

    @Test
    void cameraFocusRejectsMissingArenaInsteadOfInventingOne() {
        assertThrows(IllegalArgumentException.class, () -> FabricBattleCameraRuntime.focusPoint(null));
    }
}
