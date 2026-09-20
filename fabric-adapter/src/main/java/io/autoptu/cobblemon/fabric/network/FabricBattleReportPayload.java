package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleMatchReport;
import io.autoptu.cobblemon.battlecore.BattleMatchReportCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

/** A report is sent only to the authenticated player who owns the practice session. */
public record FabricBattleReportPayload(BattleMatchReport report) implements CustomPayload {
    public static final Id<FabricBattleReportPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_report"));
    public static final PacketCodec<PacketByteBuf, FabricBattleReportPayload> CODEC =
            CustomPayload.codecOf(FabricBattleReportPayload::write, FabricBattleReportPayload::read);
    private static boolean registered;

    public FabricBattleReportPayload {
        Objects.requireNonNull(report);
    }

    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }

    public static boolean send(ServerPlayerEntity player, BattleMatchReport report) {
        if (!ServerPlayNetworking.canSend(player, ID)) return false;
        ServerPlayNetworking.send(player, new FabricBattleReportPayload(report));
        return true;
    }

    private void write(PacketByteBuf buffer) {
        buffer.writeByteArray(BattleMatchReportCodec.encode(report));
    }

    private static FabricBattleReportPayload read(PacketByteBuf buffer) {
        return new FabricBattleReportPayload(BattleMatchReportCodec.decode(buffer.readByteArray(BattleMatchReportCodec.MAX_BYTES)));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
