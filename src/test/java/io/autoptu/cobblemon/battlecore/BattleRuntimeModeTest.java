package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleRuntimeModeTest {
    @Test void nativeOwnsGameplayByDefault() {
        assertEquals(BattleRuntimeMode.COBBLEMON, BattleRuntimeMode.parse(null));
        assertEquals(BattleRuntimeMode.COBBLEMON, BattleRuntimeMode.parse(""));
        assertEquals(BattleRuntimeMode.COBBLEMON, BattleRuntimeMode.parse("cobblemon"));
    }

    @Test void experimentalNeedsExactExplicitOptIn() {
        assertEquals(BattleRuntimeMode.PTU_EXPERIMENTAL, BattleRuntimeMode.parse("ptu-experimental"));
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimeMode.parse("ptu"));
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimeMode.parse("true"));
    }
}
