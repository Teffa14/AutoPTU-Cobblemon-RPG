package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleMenuModel;
import io.autoptu.cobblemon.fabric.network.FabricBattleMenuPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Interactive board using typed, server-provided anchors. Clicking does not invent a move. */
public final class BattleTacticalScreen extends Screen {
    private static final int SURFACE = 0xED111C28;
    private static final int BORDER = 0xFF375568;
    private static final int TEXT = 0xFFEAF2F5;
    private static final int MUTED = 0xFFA1B5C3;
    private static final int MOVE = 0xFF70D8AA;
    private static final int ATTACK = 0xFFF3A080;
    private static final int SELECTED = 0xFFFFD068;

    private final FabricBattleMenuPayload payload;
    private final BattleMenuModel model;
    private final List<String> groups;
    private String group;
    private BattleChoiceMenuService.Entry selected;
    private BattleGridCoordinate focus;
    private BattleChoiceVisualPlan.GridWindow window;
    private String feedback = "";
    private boolean boardTab;
    private boolean wide;
    private int page;
    private int age;
    private int x;
    private int y;
    private int contentWidth;
    private int contentHeight;
    private int actionWidth;
    private int boardX;
    private int boardY;
    private int boardWidth;
    private int boardHeight;
    private int gridX;
    private int gridY;
    private int cellSize;
    private int rows;
    private ButtonWidget preview;

    public BattleTacticalScreen(FabricBattleMenuPayload payload) {
        super(Text.translatable("autoptu.battle.actions"));
        this.payload = payload;
        this.model = new BattleMenuModel(payload.entries());
        this.groups = new ArrayList<>(model.attackGroups());
        if (model.hasMovement()) groups.add("movement");
        model.groupKeys().stream().filter(key -> key.startsWith("other:")).forEach(groups::add);
        this.group = groups.isEmpty() ? "" : groups.get(0);
        this.window = payload.window() != null ? payload.window() : model.window().orElse(null);
        this.focus = payload.actorOrigin();
    }

    @Override
    protected void init() {
        wide = width >= 520;
        contentWidth = Math.min(width - 24, 680);
        contentHeight = Math.min(height - 24, 370);
        x = (width - contentWidth) / 2;
        y = (height - contentHeight) / 2;
        actionWidth = wide ? Math.min(235, contentWidth / 2 - 12) : contentWidth - 20;
        boardX = wide ? x + actionWidth + 24 : x + 10;
        boardY = y + 48;
        boardWidth = wide ? contentWidth - actionWidth - 34 : contentWidth - 20;
        boardHeight = contentHeight - 107;
        if (window != null) {
            cellSize = Math.max(4, Math.min(37, Math.min((boardWidth - 28) / window.width(),
                    (boardHeight - 26) / window.height())));
            gridX = boardX + (boardWidth - window.width() * cellSize) / 2;
            gridY = boardY + (boardHeight - window.height() * cellSize) / 2 + 6;
        }
        rows = Math.max(1, (contentHeight - 120) / 32);
        page = Math.max(0, Math.min(page, Math.max(0, (groups.size() - 1) / rows)));
        if (wide || !boardTab) addActionButtons();

        int bottom = y + contentHeight - 28;
        int controlWidth = Math.max(40, (contentWidth - 38) / 4);
        preview = addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.battle.preview"),
                button -> sendPreview()).dimensions(x + 10, bottom, controlWidth, 20).build());
        preview.active = selected != null;
        addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.battle.refresh"), button -> refresh())
                .dimensions(x + 16 + controlWidth, bottom, controlWidth, 20).build());
        var end = addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.battle.end_turn"), button -> {
            command("autoptu battle endturn");
            close();
        }).dimensions(x + 22 + controlWidth * 2, bottom, controlWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("autoptu.battle.pass_help"))).build());
        end.active = payload.canEndTurn();
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(x + 28 + controlWidth * 3, bottom, controlWidth, 20).build());
        if (!wide) {
            addDrawableChild(ButtonWidget.builder(Text.translatable(boardTab ? "autoptu.battle.actions" : "autoptu.battle.board"), button -> {
                boardTab = !boardTab;
                clearAndInit();
            }).dimensions(x + contentWidth - 106, y + 8, 96, 20).build());
        }
    }

    private void addActionButtons() {
        for (int row = 0; row < rows; row++) {
            int index = page * rows + row;
            if (index >= groups.size()) break;
            String key = groups.get(index);
            String label = groupName(key);
            int count = model.group(key).size();
            var detail = key.startsWith("attack:") ? payload.details().get(key.substring(7)) : null;
            Text tooltip = Text.literal(label).append(Text.literal("\n"))
                    .append(Text.translatable("autoptu.battle.target_count", count));
            if (detail != null) tooltip = tooltip.copy().append(Text.literal("\n" + detail.range() + " | " + detail.damage()
                    + " | " + detail.accuracy() + "\n" + detail.description()));
            var button = ButtonWidget.builder(Text.literal((key.equals(group) ? "> " : "") + label + " (" + count + ")"),
                    clicked -> selectGroup(key)).dimensions(x + 10, y + 48 + row * 32, actionWidth, 28)
                    .tooltip(Tooltip.of(tooltip)).build();
            addDrawableChild(button);
        }
        int pages = Math.max(1, (groups.size() + rows - 1) / rows);
        if (pages > 1) {
            var previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                page--;
                clearAndInit();
            }).dimensions(x + 10, y + contentHeight - 66, 28, 20).build());
            previous.active = page > 0;
            var next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                page++;
                clearAndInit();
            }).dimensions(x + actionWidth - 18, y + contentHeight - 66, 28, 20).build());
            next.active = page < pages - 1;
        }
    }

    private void selectGroup(String key) {
        group = key;
        feedback = "";
        var entries = model.group(key);
        selected = entries.size() == 1 ? entries.get(0) : null;
        if (selected != null && selected.anchor() != null) focus = selected.anchor();
        else if (!entries.isEmpty()) focus = entries.get(0).anchor();
        if (focus != null && window != null && !window.contains(focus)) centerWindow(focus);
        if (!wide) boardTab = true;
        clearAndInit();
    }

    private void centerWindow(BattleGridCoordinate cell) {
        if (window == null) return;
        long left = Math.max(Integer.MIN_VALUE, Math.min((long) Integer.MAX_VALUE - window.width() + 1,
                (long) cell.x() - window.width() / 2));
        long top = Math.max(Integer.MIN_VALUE, Math.min((long) Integer.MAX_VALUE - window.height() + 1,
                (long) cell.y() - window.height() / 2));
        window = new BattleChoiceVisualPlan.GridWindow((int) left, (int) (left + window.width() - 1),
                (int) top, (int) (top + window.height() - 1));
    }

    private String groupName(String key) {
        if (key.equals("movement")) return Text.translatable("autoptu.battle.movement").getString();
        var entries = model.group(key);
        if (entries.isEmpty()) return Text.translatable("autoptu.battle.no_actions").getString();
        var first = entries.get(0);
        var detail = payload.details().get(first.moveId());
        if (detail != null) return detail.name();
        return first.kind() == BattleChoiceMenuService.EntryKind.ATTACK ? first.moveId() : first.label();
    }

    private void sendPreview() {
        if (selected == null) return;
        FabricBattleMenuClient.preview(selected.choiceId());
        close();
    }

    private void refresh() {
        command("autoptu battle menu");
        close();
    }

    private void command(String command) {
        if (client != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    @Override
    public void tick() {
        age++;
        if (client == null || client.world == null) close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && (wide || boardTab)) {
            BattleGridCoordinate cell = hitCell(mouseX, mouseY);
            if (cell != null) {
                focus = cell;
                chooseCell(cell);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void chooseCell(BattleGridCoordinate cell) {
        var entries = model.at(group, cell);
        if (entries.isEmpty()) {
            feedback = Text.translatable("autoptu.battle.invalid_cell").getString();
            selected = null;
        } else if (entries.size() == 1) {
            selected = entries.get(0);
            feedback = selected.label();
        } else {
            int current = selected == null ? -1 : entries.indexOf(selected);
            selected = entries.get((current + 1) % entries.size());
            feedback = selected.label() + " [" + ((current + 1) % entries.size() + 1) + "/" + entries.size() + "]";
        }
        preview.active = selected != null;
    }

    private BattleGridCoordinate hitCell(double mouseX, double mouseY) {
        if (window == null || mouseX < gridX || mouseY < gridY) return null;
        int column = (int) ((mouseX - gridX) / cellSize);
        int row = (int) ((mouseY - gridY) / cellSize);
        if (column >= window.width() || row >= window.height()) return null;
        return new BattleGridCoordinate(window.minX() + column, window.minY() + row);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            var entries = model.group(group);
            if (entries.isEmpty()) return true;
            int index = selected == null ? -1 : entries.indexOf(selected);
            index = Math.floorMod(index + (keyCode == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1), entries.size());
            selected = entries.get(index);
            focus = selected.anchor();
            if (focus != null && window != null && !window.contains(focus)) centerWindow(focus);
            feedback = selected.label();
            clearAndInit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            refresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (selected != null) sendPreview();
            else if (focus != null) chooseCell(focus);
            return true;
        }
        if (window != null && (wide || boardTab)) {
            int dx = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : 0;
            int dy = keyCode == GLFW.GLFW_KEY_UP ? -1 : keyCode == GLFW.GLFW_KEY_DOWN ? 1 : 0;
            if (dx != 0 || dy != 0) {
                if (focus == null) focus = new BattleGridCoordinate(window.minX(), window.minY());
                long nextX = Math.max(window.minX(), Math.min(window.maxX(), (long) focus.x() + dx));
                long nextY = Math.max(window.minY(), Math.min(window.maxY(), (long) focus.y() + dy));
                focus = new BattleGridCoordinate((int) nextX, (int) nextY);
                chooseCell(focus);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(x, y, x + contentWidth, y + contentHeight, SURFACE);
        context.fill(x, y, x + contentWidth, y + 2, group.equals("movement") ? MOVE : ATTACK);
        int titleWidth = wide ? contentWidth - 20 : contentWidth - 130;
        context.drawText(textRenderer, textRenderer.trimToWidth(title.getString(), titleWidth), x + 10, y + 12, TEXT, false);
        context.drawText(textRenderer, Text.translatable("autoptu.battle.choose_then_preview"), x + 10, y + 33, MUTED, false);
        if (groups.isEmpty() && (wide || !boardTab)) {
            context.drawText(textRenderer, Text.translatable("autoptu.battle.no_actions"), x + 12, y + 60, MUTED, false);
        }
        if (wide || boardTab) renderBoard(context, mouseX, mouseY);
        if (wide || !boardTab) renderDetails(context);
        String bottom = feedback.isEmpty() ? (age > 300 ? Text.translatable("autoptu.battle.refresh_hint").getString()
                : Text.translatable("autoptu.battle.keyboard_hint").getString()) : feedback;
        context.drawText(textRenderer, textRenderer.trimToWidth(bottom, contentWidth - 20), x + 10,
                y + contentHeight - 41, feedback.isEmpty() ? MUTED : SELECTED, false);
        if (payload.totalChoices() > payload.entries().size()) {
            context.drawText(textRenderer, Text.translatable("autoptu.battle.limited_choices", payload.entries().size(), payload.totalChoices()),
                    x + 10, y + contentHeight - 54, ATTACK, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDetails(DrawContext context) {
        var detail = group.startsWith("attack:") ? payload.details().get(group.substring(7)) : null;
        if (detail == null) return;
        int detailY = y + 52 + Math.min(rows, groups.size() - page * rows) * 32;
        if (detailY + 46 >= y + contentHeight - 68) return;
        context.drawText(textRenderer, detail.range() + " | " + detail.damage() + " | " + detail.accuracy(),
                x + 13, detailY, ATTACK, false);
        int offset = 14;
        for (var line : textRenderer.wrapLines(Text.literal(detail.description()), actionWidth - 8)) {
            if (offset > 40) break;
            context.drawText(textRenderer, line, x + 13, detailY + offset, MUTED, false);
            offset += 10;
        }
    }

    private void renderBoard(DrawContext context, int mouseX, int mouseY) {
        context.fill(boardX, boardY, boardX + boardWidth, boardY + boardHeight, 0x99101A24);
        context.drawText(textRenderer, textRenderer.trimToWidth(groupName(group), boardWidth - 14), boardX + 7, boardY + 5, TEXT, false);
        if (window == null) {
            context.drawText(textRenderer, Text.translatable("autoptu.battle.no_grid_target"), boardX + 8, boardY + 26, MUTED, false);
            return;
        }
        var hover = hitCell(mouseX, mouseY);
        for (int row = 0; row < window.height(); row++) {
            for (int column = 0; column < window.width(); column++) {
                var cell = new BattleGridCoordinate(window.minX() + column, window.minY() + row);
                int cellX = gridX + column * cellSize;
                int cellY = gridY + row * cellSize;
                boolean legal = !model.at(group, cell).isEmpty();
                boolean chosen = selected != null && cell.equals(selected.anchor());
                boolean actor = cell.equals(payload.actorOrigin());
                int color = chosen ? 0xFF826126 : legal ? group.equals("movement") ? 0xFF255541 : 0xFF64372E : 0xFF1D2F3D;
                context.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, color);
                if (cell.equals(hover) || cell.equals(focus)) {
                    context.drawBorder(cellX, cellY, cellSize, cellSize, legal ? SELECTED : BORDER);
                }
                if (actor && cellSize >= 12) {
                    context.drawCenteredTextWithShadow(textRenderer, "P", cellX + cellSize / 2, cellY + (cellSize - 8) / 2, MOVE);
                } else if (legal && cellSize >= 12) {
                    String marker = chosen ? "+" : group.equals("movement") ? "·" : "X";
                    context.drawCenteredTextWithShadow(textRenderer, marker, cellX + cellSize / 2, cellY + (cellSize - 8) / 2, chosen ? SELECTED : TEXT);
                }
            }
        }
        if (cellSize >= 17) {
            for (int column = 0; column < window.width(); column++) {
                context.drawCenteredTextWithShadow(textRenderer, Integer.toString(window.minX() + column),
                        gridX + column * cellSize + cellSize / 2, gridY - 10, MUTED);
            }
            for (int row = 0; row < window.height(); row++) {
                context.drawText(textRenderer, Integer.toString(window.minY() + row), gridX - 13,
                        gridY + row * cellSize + (cellSize - 8) / 2, MUTED, false);
            }
        }
    }
}
