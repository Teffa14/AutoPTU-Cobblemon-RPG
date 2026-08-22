package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleClientActionPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FabricBattleActionPayloadTest {
    @Test
    void roundTripsMinimalMoveIntentWithoutPrincipalOrRuleState() {
        BattleClientActionPacket packet = new BattleClientActionPacket(
                "reservation-1",
                "pokemon-1",
                "MOVE",
                "tackle",
                "COMBATANT",
                "pokemon-2",
                null,
                null);

        FabricBattleActionPayload payload = new FabricBattleActionPayload(packet);
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        FabricBattleActionPayload.CODEC.encode(buffer, payload);
        FabricBattleActionPayload decoded = FabricBattleActionPayload.CODEC.decode(buffer);

        assertEquals(packet, decoded.packet());
    }

    @Test
    void wireShapeCannotCarryAuthenticatedIdentityOrTrustedBattleResults() {
        Set<String> forbidden = Set.of(
                "authenticatedPrincipalId", "playerId", "trainerId", "stats", "modifiers",
                "inventory", "actionBudget", "frequency", "accuracy", "damage", "hp",
                "legalChoices", "outcome", "result");

        Set<String> packetFields = Arrays.stream(BattleClientActionPacket.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        for (String field : forbidden) {
            assertFalse(packetFields.contains(field), "client transport must not carry trusted field: " + field);
        }
    }
}
