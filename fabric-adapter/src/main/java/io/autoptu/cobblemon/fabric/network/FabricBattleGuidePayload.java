package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** No battle permissions or commands are conveyed by this help-screen request. */
public record FabricBattleGuidePayload() implements CustomPayload {
    public static final Id<FabricBattleGuidePayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_guide"));
    public static final PacketCodec<PacketByteBuf, FabricBattleGuidePayload> CODEC =
            CustomPayload.codecOf(FabricBattleGuidePayload::write, buffer -> new FabricBattleGuidePayload());
    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }

    private void write(PacketByteBuf buffer) {}

    public static boolean send(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ID)) return false;
        ServerPlayNetworking.send(player, new FabricBattleGuidePayload());
        return true;
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
