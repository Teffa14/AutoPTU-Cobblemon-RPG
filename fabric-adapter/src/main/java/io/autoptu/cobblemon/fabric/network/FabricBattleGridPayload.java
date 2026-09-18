package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.LinkedHashSet;
import java.util.Set;

/** One bounded S2C visual frame. Coordinates never become client battle decisions. */
public record FabricBattleGridPayload(
        BattleArenaSnapshot arena, BattleChoiceVisualPlan plan, boolean committed, BattleGridCoordinate actorOrigin
) implements CustomPayload {
    public static final Id<FabricBattleGridPayload> ID = new Id<>(Identifier.of("autoptu_cobblemon_rpg", "battle_grid"));
    public static final PacketCodec<PacketByteBuf, FabricBattleGridPayload> CODEC =
            CustomPayload.codecOf(FabricBattleGridPayload::write, FabricBattleGridPayload::read);

    private static FabricBattleGridPayload read(PacketByteBuf buf) {
        var arena = new BattleArenaSnapshot(buf.readString(128), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readByte(), buf.readByte(), buf.readByte(), buf.readByte());
        var window = buf.readBoolean() ? new BattleChoiceVisualPlan.GridWindow(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()) : null;
        Set<BattleGridCoordinate> movement = readCells(buf);
        Set<BattleGridCoordinate> attacks = readCells(buf);
        BattleChoiceVisualPlan.Highlight highlight = null;
        if (buf.readBoolean()) {
            var kind = buf.readEnumConstant(BattleChoiceVisualPlan.HighlightKind.class);
            var cell = readNullableCell(buf);
            String label = buf.readString(256);
            highlight = new BattleChoiceVisualPlan.Highlight(kind, cell, "visual", kind == BattleChoiceVisualPlan.HighlightKind.ATTACK ? label : null);
        }
        return new FabricBattleGridPayload(arena, new BattleChoiceVisualPlan(window, movement, attacks, highlight),
                buf.readBoolean(), readNullableCell(buf));
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(arena.dimensionId(), 128);
        buf.writeInt(arena.originX()); buf.writeInt(arena.originY()); buf.writeInt(arena.originZ());
        buf.writeByte(arena.gridXdx()); buf.writeByte(arena.gridXdz()); buf.writeByte(arena.gridYdx()); buf.writeByte(arena.gridYdz());
        var window = plan.gridWindow();
        buf.writeBoolean(window != null);
        if (window != null) {
            buf.writeInt(window.minX()); buf.writeInt(window.maxX()); buf.writeInt(window.minY()); buf.writeInt(window.maxY());
        }
        writeCells(buf, plan.shiftDestinations()); writeCells(buf, plan.attackTargets());
        var highlight = plan.highlight();
        buf.writeBoolean(highlight != null);
        if (highlight != null) {
            buf.writeEnumConstant(highlight.kind()); writeNullableCell(buf, highlight.anchor());
            String label = highlight.moveId() == null ? "MOVE" : highlight.moveId();
            buf.writeString(label.length() > 256 ? label.substring(0, 256) : label, 256);
        }
        buf.writeBoolean(committed); writeNullableCell(buf, actorOrigin);
    }

    private static Set<BattleGridCoordinate> readCells(PacketByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 144) throw new IllegalArgumentException("too many visual cells");
        Set<BattleGridCoordinate> result = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) result.add(new BattleGridCoordinate(buf.readInt(), buf.readInt()));
        return result;
    }

    private static void writeCells(PacketByteBuf buf, Set<BattleGridCoordinate> cells) {
        buf.writeVarInt(cells.size());
        for (var cell : cells) { buf.writeInt(cell.x()); buf.writeInt(cell.y()); }
    }

    private static BattleGridCoordinate readNullableCell(PacketByteBuf buf) {
        return buf.readBoolean() ? new BattleGridCoordinate(buf.readInt(), buf.readInt()) : null;
    }

    private static void writeNullableCell(PacketByteBuf buf, BattleGridCoordinate cell) {
        buf.writeBoolean(cell != null);
        if (cell != null) { buf.writeInt(cell.x()); buf.writeInt(cell.y()); }
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
