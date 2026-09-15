package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Server-owned choice labels/keys for the battle screen; clicks still use revalidated commands. */
public record FabricBattleMenuPayload(List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn) implements CustomPayload {
    public static final Id<FabricBattleMenuPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_menu"));
    public static final PacketCodec<PacketByteBuf, FabricBattleMenuPayload> CODEC =
            CustomPayload.codecOf(FabricBattleMenuPayload::write, FabricBattleMenuPayload::read);
    private static boolean registered;
    public FabricBattleMenuPayload { entries = List.copyOf(entries); }
    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }
    public static boolean send(ServerPlayerEntity player, List<BattleChoiceMenuService.Entry> entries) {
        return send(player, entries, false);
    }
    public static boolean send(ServerPlayerEntity player, List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn) {
        if (!ServerPlayNetworking.canSend(player, ID)) return false;
        ServerPlayNetworking.send(player, new FabricBattleMenuPayload(entries.stream().limit(512).toList(), canEndTurn));
        return true;
    }
    private static FabricBattleMenuPayload read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 512) throw new IllegalArgumentException("invalid menu size");
        var entries = new ArrayList<BattleChoiceMenuService.Entry>(count);
        for (int i = 0; i < count; i++) entries.add(new BattleChoiceMenuService.Entry(buf.readString(4096), buf.readString(512)));
        return new FabricBattleMenuPayload(entries, buf.readBoolean());
    }
    private void write(PacketByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (var entry : entries) { buf.writeString(entry.choiceId(), 4096); buf.writeString(entry.label(), 512); }
        buf.writeBoolean(canEndTurn);
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
