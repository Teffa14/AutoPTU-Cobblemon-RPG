package io.autoptu.cobblemon.fabric.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Presentation-only S2C tactical camera frame.
 *
 * <p>Every active field is authored by the server. The payload carries physical camera framing
 * bounds only; it does not describe PTU legal tiles, movement, targeting, turn state or outcomes.
 */
public record FabricBattleCameraPayload(
        boolean active,
        String battleId,
        int originX,
        int originY,
        int originZ,
        int width,
        int height
) implements CustomPayload {
    public static final Id<FabricBattleCameraPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_camera_frame"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricBattleCameraPayload> CODEC =
            CustomPayload.codecOf(FabricBattleCameraPayload::write, FabricBattleCameraPayload::new);

    public FabricBattleCameraPayload {
        if (battleId == null) battleId = "";
        if (active) {
            if (battleId.isBlank()) throw new IllegalArgumentException("active camera battleId is required");
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("active camera bounds must be positive");
        }
    }

    public FabricBattleCameraPayload(PacketByteBuf buf) {
        this(
                buf.readBoolean(),
                buf.readString(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static FabricBattleCameraPayload clear() {
        return new FabricBattleCameraPayload(false, "", 0, 0, 0, 0, 0);
    }

    private void write(PacketByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeString(battleId);
        buf.writeInt(originX);
        buf.writeInt(originY);
        buf.writeInt(originZ);
        buf.writeInt(width);
        buf.writeInt(height);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
