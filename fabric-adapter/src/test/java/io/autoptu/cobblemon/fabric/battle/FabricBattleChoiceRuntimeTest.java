package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FabricBattleChoiceRuntimeTest {
    @Test
    void hudTitleUsesOnlyBoundActorAndAuthoritativeChoiceCount() {
        assertEquals(
                "AutoPTU • player-mon-1 • legal choices 4",
                FabricBattleChoiceRuntime.hudTitle("player-mon-1", 4)
        );
    }

    @Test
    void hudTitleShowsZeroChoicesWithoutInventingTurnMeaning() {
        assertEquals(
                "AutoPTU • wild-mon-2 • legal choices 0",
                FabricBattleChoiceRuntime.hudTitle("wild-mon-2", 0)
        );
    }

    @Test
    void hudTitleRejectsInvalidPresentationInputs() {
        assertThrows(IllegalArgumentException.class, () -> FabricBattleChoiceRuntime.hudTitle(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> FabricBattleChoiceRuntime.hudTitle("actor", -1));
    }
}
