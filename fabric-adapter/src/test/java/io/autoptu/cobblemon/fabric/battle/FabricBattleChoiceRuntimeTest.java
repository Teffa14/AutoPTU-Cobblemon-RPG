package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricBattleChoiceRuntimeTest {
    private final UUID playerUuid = UUID.fromString("72ec2817-454a-45b8-a57d-9b814434ef38");

    @AfterEach
    void cleanup() {
        FabricBattleChoiceRuntime.unbind(playerUuid);
    }

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

    @Test
    void battleStatusIsUnboundWithoutServerOwnedSession() {
        FabricBattleChoiceRuntime.BattleStatusView status = FabricBattleChoiceRuntime.status(playerUuid);

        assertFalse(status.bound());
        assertNull(status.actorId());
        assertNull(status.authoritativeLegalChoiceCount());
    }

    @Test
    void battleStatusUsesServerOwnedBindingWithoutInventingBattleState() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");

        FabricBattleChoiceRuntime.BattleStatusView status = FabricBattleChoiceRuntime.status(playerUuid);

        assertTrue(status.bound());
        assertEquals("player-mon-1", status.actorId());
        assertNull(status.authoritativeLegalChoiceCount());
    }
}
