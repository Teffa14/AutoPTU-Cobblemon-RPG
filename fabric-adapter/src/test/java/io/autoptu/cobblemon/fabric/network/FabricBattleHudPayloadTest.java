package io.autoptu.cobblemon.fabric.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricBattleHudPayloadTest {
    @Test
    void knownAuthoritativeMaxProducesOnlyTheServerSuppliedRatio() {
        FabricBattleHudPayload.Combatant combatant =
                new FabricBattleHudPayload.Combatant("Charmander", "charmander", 5, 15, 30, "");
        assertTrue(combatant.hasKnownMaxHp());
        assertEquals(0.5F, combatant.hpRatio());
    }

    @Test
    void unknownMaxNeverInventsAHealthRatio() {
        FabricBattleHudPayload.Combatant combatant =
                new FabricBattleHudPayload.Combatant("Target", "", 0, 17, -1, "");
        assertFalse(combatant.hasKnownMaxHp());
        assertEquals(-1.0F, combatant.hpRatio());
    }

    @Test
    void rejectsImpossibleServerAuthoredHealthPayloads() {
        assertThrows(IllegalArgumentException.class,
                () -> new FabricBattleHudPayload.Combatant("Target", "", 0, 31, 30, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new FabricBattleHudPayload.Combatant("Target", "", 0, 0, -2, ""));
    }
}
