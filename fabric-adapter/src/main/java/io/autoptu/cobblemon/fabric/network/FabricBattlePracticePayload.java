package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Opens the local setup screen. The actual start command is always permission checked. */
public record FabricBattlePracticePayload(boolean operator) implements CustomPayload {
    public static final Id<FabricBattlePracticePayload> ID = new Id<>(Identifier.of("autoptu", "battle_practice"));
    public static final PacketCodec<PacketByteBuf, FabricBattlePracticePayload> CODEC = PacketCodec.of(
            (value, buffer) -> buffer.writeBoolean(value.operator()),
            buffer -> new FabricBattlePracticePayload(buffer.readBoolean()));
    private static boolean registered;

    public static synchronized void register() {
        if (!registered) {
            PayloadTypeRegistry.playS2C().register(ID, CODEC);
            registered = true;
        }
    }

    public static boolean send(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ID)) return false;
        ServerPlayNetworking.send(player, new FabricBattlePracticePayload(player.hasPermissionLevel(2)));
        return true;
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
