package io.autoptu.cobblemon.fabric.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Presentation-only S2C detached battle camera frame.
 *
 * <p>Every active field is authored by the server. The payload carries physical camera framing
 * information only; it does not describe PTU legal tiles, movement, targeting, turn state, damage
 * or outcomes.
 */
public record FabricBattleCameraPayload(
        boolean active,
        String battleId,
        FabricBattleCameraMode mode,
        int originX,
        int originY,
        int originZ,
        int width,
        int height,
        double focusX,
        double focusY,
        double focusZ,
        float yaw
) implements CustomPayload {
    public static final Id<FabricBattleCameraPayload> ID =
            new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_camera_frame"));
    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, FabricBattleCameraPayload> CODEC =
            CustomPayload.codecOf(FabricBattleCameraPayload::write, FabricBattleCameraPayload::new);

    public FabricBattleCameraPayload {
        if (battleId == null) battleId = "";
        if (mode == null) mode = FabricBattleCameraMode.TACTICAL_AERIAL;
        if (active) {
            if (battleId.isBlank()) throw new IllegalArgumentException("active camera battleId is required");
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("active camera bounds must be positive");
            if (!Double.isFinite(focusX) || !Double.isFinite(focusY) || !Double.isFinite(focusZ)) {
                throw new IllegalArgumentException("active camera focus must be finite");
            }
            if (!Float.isFinite(yaw)) throw new IllegalArgumentException("active camera yaw must be finite");
        }
    }

    public FabricBattleCameraPayload(PacketByteBuf buf) {
        this(
                buf.readBoolean(),
                buf.readString(),
                FabricBattleCameraMode.fromWire(buf.readString()),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
        );
    }

    public static FabricBattleCameraPayload clear() {
        return new FabricBattleCameraPayload(
                false,
                "",
                FabricBattleCameraMode.TACTICAL_AERIAL,
                0,
                0,
                0,
                0,
                0,
                0.0D,
                0.0D,
                0.0D,
                0.0F
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeString(battleId);
        buf.writeString(mode.name());
        buf.writeInt(originX);
        buf.writeInt(originY);
        buf.writeInt(originZ);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeDouble(focusX);
        buf.writeDouble(focusY);
        buf.writeDouble(focusZ);
        buf.writeFloat(yaw);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
