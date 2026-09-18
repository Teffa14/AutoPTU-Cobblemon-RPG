package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Read-only HUD projection from the owning battle session. */
public record FabricBattleStatusPayload(String allyName, int allyHp, int allyMaxHp,
        String enemyName, int enemyHp, int enemyMaxHp, String phase, int round) implements CustomPayload {
    public static final Id<FabricBattleStatusPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_status"));
    public static final PacketCodec<PacketByteBuf, FabricBattleStatusPayload> CODEC =
            CustomPayload.codecOf(FabricBattleStatusPayload::write, FabricBattleStatusPayload::read);
    private static boolean registered;
    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC); registered = true;
    }
    public static void send(ServerPlayerEntity player, String ally, int hp, int max, String enemy,
                            int enemyHp, int enemyMax, String phase, int round) {
        if (ServerPlayNetworking.canSend(player, ID)) ServerPlayNetworking.send(player,
                new FabricBattleStatusPayload(ally, hp, max, enemy, enemyHp, enemyMax, phase, round));
    }
    public static void clear(ServerPlayerEntity player) { send(player, "", 0, 1, "", 0, 1, "", 0); }
    private static FabricBattleStatusPayload read(PacketByteBuf buf) {
        return new FabricBattleStatusPayload(buf.readString(128), buf.readVarInt(), buf.readVarInt(),
                buf.readString(128), buf.readVarInt(), buf.readVarInt(), buf.readString(128), buf.readVarInt());
    }
    private void write(PacketByteBuf buf) {
        buf.writeString(allyName, 128); buf.writeVarInt(allyHp); buf.writeVarInt(allyMaxHp);
        buf.writeString(enemyName, 128); buf.writeVarInt(enemyHp); buf.writeVarInt(enemyMaxHp);
        buf.writeString(phase, 128); buf.writeVarInt(round);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
