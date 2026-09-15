package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** The token binds a confirm input to the exact preview the player was shown. Empty token clears it. */
public record FabricBattleSelectionPayload(String token, String label) implements CustomPayload {
    public static final Id<FabricBattleSelectionPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_selection"));
    public static final PacketCodec<PacketByteBuf, FabricBattleSelectionPayload> CODEC =
            CustomPayload.codecOf((payload, buf) -> { buf.writeString(payload.token(), 128); buf.writeString(payload.label(), 512); },
                    buf -> new FabricBattleSelectionPayload(buf.readString(128), buf.readString(512)));
    private static boolean registered;
    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }
    public static void send(ServerPlayerEntity player, String token, String label) {
        if (ServerPlayNetworking.canSend(player, ID)) ServerPlayNetworking.send(player,
                new FabricBattleSelectionPayload(token == null ? "" : token, label == null ? "" : label));
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
