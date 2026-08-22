package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleClientActionPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Fabric 1.21.1 C2S payload carrying only the existing transport-safe battle intent. */
public record FabricBattleActionPayload(BattleClientActionPacket packet) implements CustomPayload {
    public static final Id<FabricBattleActionPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_action"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricBattleActionPayload> CODEC =
            CustomPayload.codecOf(FabricBattleActionPayload::write, FabricBattleActionPayload::new);

    public FabricBattleActionPayload(PacketByteBuf buf) {
        this(new BattleClientActionPacket(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                readNullableString(buf),
                readNullableString(buf),
                readNullableString(buf),
                readNullableInt(buf),
                readNullableInt(buf)));
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(packet.reservationId());
        buf.writeString(packet.actorId());
        buf.writeString(packet.actionKind());
        writeNullableString(buf, packet.moveId());
        writeNullableString(buf, packet.targetMode());
        writeNullableString(buf, packet.targetCombatantId());
        writeNullableInt(buf, packet.targetX());
        writeNullableInt(buf, packet.targetY());
    }

    private static String readNullableString(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readString() : null;
    }

    private static void writeNullableString(PacketByteBuf buf, String value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeString(value);
    }

    private static Integer readNullableInt(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readInt() : null;
    }

    private static void writeNullableInt(PacketByteBuf buf, Integer value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeInt(value);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
