package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleActionDetail;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Server-owned choice labels/keys for the battle screen; clicks still use revalidated commands. */
public record FabricBattleMenuPayload(List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn,
        BattleChoiceVisualPlan.GridWindow window, BattleGridCoordinate actorOrigin, int totalChoices,
        Map<String, BattleActionDetail> details) implements CustomPayload {
    public static final Id<FabricBattleMenuPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_menu"));
    public static final PacketCodec<PacketByteBuf, FabricBattleMenuPayload> CODEC =
            CustomPayload.codecOf(FabricBattleMenuPayload::write, FabricBattleMenuPayload::read);
    private static boolean registered;
    public FabricBattleMenuPayload {
        entries = List.copyOf(entries);
        details = Map.copyOf(details);
        if (details.size() > 64) throw new IllegalArgumentException("too many action details");
        if (entries.size() > 512 || totalChoices < entries.size()) throw new IllegalArgumentException("invalid menu size");
    }
    public FabricBattleMenuPayload(List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn) {
        this(entries, canEndTurn, null, null, entries.size(), Map.of());
    }
    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        registered = true;
    }
    public static boolean send(ServerPlayerEntity player, List<BattleChoiceMenuService.Entry> entries) {
        return send(player, entries, false);
    }
    public static boolean send(ServerPlayerEntity player, List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn) {
        return send(player, entries, canEndTurn, null, null, Map.of());
    }
    public static boolean send(ServerPlayerEntity player, List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn,
                               BattleChoiceVisualPlan.GridWindow window, BattleGridCoordinate origin, Map<String, BattleActionDetail> details) {
        if (!ServerPlayNetworking.canSend(player, ID)) return false;
        ServerPlayNetworking.send(player, new FabricBattleMenuPayload(entries.stream().limit(512).toList(), canEndTurn,
                window, origin, entries.size(), details));
        return true;
    }
    private static FabricBattleMenuPayload read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 512) throw new IllegalArgumentException("invalid menu size");
        var entries = new ArrayList<BattleChoiceMenuService.Entry>(count);
        for (int i = 0; i < count; i++) {
            String id = buf.readString(4096);
            String label = buf.readString(512);
            var kind = buf.readEnumConstant(BattleChoiceMenuService.EntryKind.class);
            var anchor = readCell(buf);
            entries.add(new BattleChoiceMenuService.Entry(id, label, kind, anchor, buf.readString(256), buf.readString(256)));
        }
        boolean canEndTurn = buf.readBoolean();
        var window = buf.readBoolean() ? new BattleChoiceVisualPlan.GridWindow(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()) : null;
        var origin = readCell(buf);
        int total = buf.readVarInt();
        int detailCount = buf.readVarInt();
        if (detailCount < 0 || detailCount > 64) throw new IllegalArgumentException("invalid details count");
        Map<String, BattleActionDetail> details = new LinkedHashMap<>();
        for (int i = 0; i < detailCount; i++) {
            String moveId = buf.readString(256);
            var detail = new BattleActionDetail(buf.readString(128), buf.readString(512), buf.readString(64), buf.readString(64), buf.readString(64));
            if (details.putIfAbsent(moveId, detail) != null) throw new IllegalArgumentException("duplicate action detail");
        }
        return new FabricBattleMenuPayload(entries, canEndTurn, window, origin, total, details);
    }
    private void write(PacketByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (var entry : entries) {
            buf.writeString(entry.choiceId(), 4096);
            buf.writeString(entry.label(), 512);
            buf.writeEnumConstant(entry.kind());
            writeCell(buf, entry.anchor());
            buf.writeString(entry.moveId(), 256);
            buf.writeString(entry.targetId(), 256);
        }
        buf.writeBoolean(canEndTurn);
        buf.writeBoolean(window != null);
        if (window != null) {
            buf.writeInt(window.minX()); buf.writeInt(window.maxX()); buf.writeInt(window.minY()); buf.writeInt(window.maxY());
        }
        writeCell(buf, actorOrigin);
        buf.writeVarInt(totalChoices);
        buf.writeVarInt(details.size());
        for (var entry : details.entrySet()) {
            var detail = entry.getValue();
            buf.writeString(entry.getKey(), 256);
            buf.writeString(detail.name(), 128);
            buf.writeString(detail.description(), 512);
            buf.writeString(detail.range(), 64);
            buf.writeString(detail.damage(), 64);
            buf.writeString(detail.accuracy(), 64);
        }
    }

    private static BattleGridCoordinate readCell(PacketByteBuf buf) {
        return buf.readBoolean() ? new BattleGridCoordinate(buf.readInt(), buf.readInt()) : null;
    }

    private static void writeCell(PacketByteBuf buf, BattleGridCoordinate cell) {
        buf.writeBoolean(cell != null);
        if (cell != null) { buf.writeInt(cell.x()); buf.writeInt(cell.y()); }
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
