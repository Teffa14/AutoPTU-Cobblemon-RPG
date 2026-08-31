package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.battlecore.BattleClientActionRequest;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoice;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoiceSet;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricBattleChoiceRuntimeTest {
    private final UUID playerUuid = UUID.fromString("72ec2817-454a-45b8-a57d-9b814434ef38");
    private final UUID spectatorUuid = UUID.fromString("1e84117e-c4f8-48b5-a134-718352d54f88");

    @AfterEach
    void cleanup() {
        FabricBattleChoiceRuntime.unbind(playerUuid);
        FabricBattleChoiceRuntime.unbind(spectatorUuid);
        FabricBattleChoiceRuntime.stopSpectating(playerUuid);
        FabricBattleChoiceRuntime.stopSpectating(spectatorUuid);
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
    void targetOverlayUsesOnlyAuthoritativeShiftAndTargetAnchors() {
        BattleGridCoordinate shift = new BattleGridCoordinate(2, 3);
        BattleGridCoordinate tile = new BattleGridCoordinate(4, 5);
        BattleGridCoordinate combatant = new BattleGridCoordinate(6, 7);
        BattleGridCoordinate self = new BattleGridCoordinate(8, 9);
        BattleGridCoordinate field = new BattleGridCoordinate(10, 11);
        BattleCoreLegalChoiceSet set = new BattleCoreLegalChoiceSet(
                "reservation-17",
                "player-mon-1",
                List.of(
                        new BattleCoreLegalChoice.Shift("player-mon-1", shift, "shift"),
                        new BattleCoreLegalChoice.Move("player-mon-1", "ember", BattleClientActionRequest.Target.Mode.TILE,
                                null, tile, "STANDARD", "tile"),
                        new BattleCoreLegalChoice.Move("player-mon-1", "tackle", BattleClientActionRequest.Target.Mode.COMBATANT,
                                "wild-mon-1", combatant, "STANDARD", "combatant"),
                        new BattleCoreLegalChoice.Move("player-mon-1", "focus", BattleClientActionRequest.Target.Mode.SELF,
                                null, self, "STANDARD", "self"),
                        new BattleCoreLegalChoice.Move("player-mon-1", "weather", BattleClientActionRequest.Target.Mode.FIELD,
                                null, field, "STANDARD", "field")
                )
        );

        assertEquals(Set.of(shift, tile, combatant), FabricBattleChoiceRuntime.authoritativeTargetAnchors(set));
    }

    @Test
    void targetOverlayDeduplicatesAnchorsAlreadyChosenByUpstream() {
        BattleGridCoordinate shared = new BattleGridCoordinate(3, 4);
        BattleCoreLegalChoiceSet set = new BattleCoreLegalChoiceSet(
                "reservation-17",
                "player-mon-1",
                List.of(
                        new BattleCoreLegalChoice.Shift("player-mon-1", shared, "shift"),
                        new BattleCoreLegalChoice.Move("player-mon-1", "ember", BattleClientActionRequest.Target.Mode.TILE,
                                null, shared, "STANDARD", "tile")
                )
        );

        assertEquals(Set.of(shared), FabricBattleChoiceRuntime.authoritativeTargetAnchors(set));
    }

    @Test
    void battleStatusIsUnboundWithoutServerOwnedSession() {
        FabricBattleChoiceRuntime.BattleStatusView status = FabricBattleChoiceRuntime.status(playerUuid);

        assertFalse(status.bound());
        assertNull(status.actorId());
        assertNull(status.authoritativeLegalChoiceCount());
        assertNull(FabricBattleChoiceRuntime.spectateId(playerUuid));
    }

    @Test
    void battleStatusUsesServerOwnedBindingWithoutInventingBattleState() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");

        FabricBattleChoiceRuntime.BattleStatusView status = FabricBattleChoiceRuntime.status(playerUuid);

        assertTrue(status.bound());
        assertEquals("player-mon-1", status.actorId());
        assertNull(status.authoritativeLegalChoiceCount());
        assertNotNull(FabricBattleChoiceRuntime.spectateId(playerUuid));
    }

    @Test
    void spectatorCanAttachOnlyThroughServerGeneratedOpaqueId() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");
        String battleId = FabricBattleChoiceRuntime.spectateId(playerUuid);

        assertTrue(FabricBattleChoiceRuntime.beginSpectating(spectatorUuid, battleId));
        FabricBattleChoiceRuntime.BattleStatusView status = FabricBattleChoiceRuntime.spectatorStatus(spectatorUuid);

        assertTrue(status.bound());
        assertEquals("player-mon-1", status.actorId());
        assertNull(status.authoritativeLegalChoiceCount());
        assertFalse(FabricBattleChoiceRuntime.hasBinding(spectatorUuid));
    }

    @Test
    void spectatorRequestFailsClosedForUnknownId() {
        assertFalse(FabricBattleChoiceRuntime.beginSpectating(spectatorUuid, UUID.randomUUID().toString()));
        assertFalse(FabricBattleChoiceRuntime.spectatorStatus(spectatorUuid).bound());
    }

    @Test
    void participantCannotBecomeSpectatorAndGainAmbiguousScope() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");
        String battleId = FabricBattleChoiceRuntime.spectateId(playerUuid);

        assertFalse(FabricBattleChoiceRuntime.beginSpectating(playerUuid, battleId));
        assertFalse(FabricBattleChoiceRuntime.spectatorStatus(playerUuid).bound());
    }

    @Test
    void unbindingLastParticipantInvalidatesSpectatorProjection() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");
        String battleId = FabricBattleChoiceRuntime.spectateId(playerUuid);
        assertTrue(FabricBattleChoiceRuntime.beginSpectating(spectatorUuid, battleId));

        FabricBattleChoiceRuntime.unbind(playerUuid);

        assertFalse(FabricBattleChoiceRuntime.spectatorStatus(spectatorUuid).bound());
    }

    @Test
    void rebindingSameAuthoritativeScopeKeepsSpectateIdStable() {
        FabricBattleChoiceRuntime.bind(playerUuid, "reservation-17", "player-mon-1");
        String first = FabricBattleChoiceRuntime.spectateId(playerUuid);

        FabricBattleChoiceRuntime.bind(playerUuid, " reservation-17 ", " player-mon-1 ");

        assertEquals(first, FabricBattleChoiceRuntime.spectateId(playerUuid));
    }
}
