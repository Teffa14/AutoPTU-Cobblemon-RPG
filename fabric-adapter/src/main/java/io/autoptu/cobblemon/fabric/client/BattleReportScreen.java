package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattleMatchReport;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Scrollable post-match report, including a live snapshot when requested mid-battle. */
public final class BattleReportScreen extends Screen {
    private final BattleMatchReport report;
    private BattleMatchReport.Side filter = BattleMatchReport.Side.NONE;
    private List<BattleMatchReport.Event> events;
    private int offset;
    private int visibleRows;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public BattleReportScreen(BattleMatchReport report) {
        super(Text.translatable("autoptu.battle.report"));
        this.report = report;
        this.events = report.events();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - 24, 610);
        panelHeight = Math.min(height - 24, 380);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        visibleRows = Math.max(1, (panelHeight - 151) / 17);
        offset = Math.max(0, Math.min(offset, Math.max(0, events.size() - visibleRows)));
        int footer = top + panelHeight - 27;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(left + panelWidth - 90, footer, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.battle.refresh"), button -> {
            if (client != null && client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand("autoptu battle report");
                close();
            }
        }).dimensions(left + panelWidth - 176, footer, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> scroll(-visibleRows))
                .dimensions(left + 10, footer, 25, 20).build()).active = offset > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> scroll(visibleRows))
                .dimensions(left + 41, footer, 25, 20).build()).active = offset + visibleRows < events.size();
        addDrawableChild(ButtonWidget.builder(filterLabel(), button -> {
            filter = switch (filter) {
                case NONE -> BattleMatchReport.Side.ALLY;
                case ALLY -> BattleMatchReport.Side.ENEMY;
                case ENEMY -> BattleMatchReport.Side.NONE;
            };
            events = report.events().stream().filter(event -> filter == BattleMatchReport.Side.NONE || event.side() == filter).toList();
            offset = 0;
            clearAndInit();
        }).dimensions(left + 10, top + 93, Math.min(150, panelWidth / 2 - 12), 20).build());
    }

    private Text filterLabel() {
        return Text.translatable(switch (filter) {
            case NONE -> "autoptu.battle.log_all";
            case ALLY -> "autoptu.battle.log_ally";
            case ENEMY -> "autoptu.battle.log_enemy";
        });
    }

    private void scroll(int amount) {
        offset = Math.max(0, Math.min(Math.max(0, events.size() - visibleRows), offset + amount));
        clearAndInit();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll(verticalAmount > 0 ? -3 : verticalAmount < 0 ? 3 : 0);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_DOWN) {
            scroll(keyCode == GLFW.GLFW_KEY_DOWN ? 1 : visibleRows);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_UP) {
            scroll(keyCode == GLFW.GLFW_KEY_UP ? -1 : -visibleRows);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            scroll(events.size());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            scroll(-events.size());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF2101B27);
        int accent = report.outcome() == BattleMatchReport.Outcome.DEFEAT ? 0xFFF39880 : 0xFF73D5AC;
        context.fill(left, top, left + panelWidth, top + 2, accent);
        context.drawText(textRenderer, title, left + 10, top + 10, 0xFFEFF4F7, false);
        String summary = Text.translatable("autoptu.battle.outcome." + report.outcome().name().toLowerCase(java.util.Locale.ROOT)).getString()
                + " | " + Text.translatable("autoptu.battle.round_value", report.round()).getString()
                + " | " + report.elapsedTicks() / 20 + "s";
        context.drawText(textRenderer, textRenderer.trimToWidth(summary, panelWidth - 20), left + 10, top + 25, accent, false);
        int half = (panelWidth - 30) / 2;
        statistics(context, left + 10, top + 41, half, report.allyName(), report.ally(), 0xFF73D5AC);
        statistics(context, left + 20 + half, top + 41, half, report.enemyName(), report.enemy(), 0xFFF39880);
        int y = top + 118;
        for (int index = offset; index < Math.min(events.size(), offset + visibleRows); index++) {
            var event = events.get(index);
            int rowY = y + (index - offset) * 17;
            context.fill(left + 10, rowY - 2, left + panelWidth - 10, rowY + 13,
                    index % 2 == 0 ? 0xA0243544 : 0x80202B37);
            String line = "R" + event.round() + " " + actor(event.side()) + " " + describe(event);
            int color = event.critical() ? 0xFFFFD37D : event.kind() == BattleMatchReport.Kind.MISS ? 0xFFA5BACB : 0xFFDCE9EF;
            context.drawText(textRenderer, textRenderer.trimToWidth(line, panelWidth - 30), left + 15, rowY + 1, color, false);
            if (mouseX >= left + 10 && mouseX < left + panelWidth - 10 && mouseY >= rowY - 2 && mouseY < rowY + 13) {
                context.drawTooltip(textRenderer, Text.literal(line), mouseX, mouseY);
            }
        }
        if (events.isEmpty()) {
            context.drawText(textRenderer, Text.translatable("autoptu.battle.no_events"), left + 12, y + 3, 0xFFA5BACB, false);
        }
        if (report.omittedEvents() > 0) {
            context.drawText(textRenderer, Text.translatable("autoptu.battle.events_omitted", report.omittedEvents()),
                    left + panelWidth / 2, top + 99, 0xFFA5BACB, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void statistics(DrawContext context, int x, int y, int width, String name, BattleMatchReport.Statistics stats, int accent) {
        context.fill(x, y, x + width, y + 45, 0xBB203240);
        context.fill(x, y, x + 2, y + 45, accent);
        context.drawText(textRenderer, textRenderer.trimToWidth(name, width - 12), x + 6, y + 5, accent, false);
        String first = Text.translatable("autoptu.battle.statistics_hits", stats.hits(), stats.attacks(), stats.damage()).getString();
        String second = Text.translatable("autoptu.battle.statistics_extra", stats.criticals(), stats.shifts()).getString();
        context.drawText(textRenderer, textRenderer.trimToWidth(first, width - 12), x + 6, y + 19, 0xFFE1EBF0, false);
        context.drawText(textRenderer, textRenderer.trimToWidth(second, width - 12), x + 6, y + 31, 0xFFA5BACB, false);
    }

    private String actor(BattleMatchReport.Side side) {
        return switch (side) {
            case ALLY -> report.allyName();
            case ENEMY -> report.enemyName();
            case NONE -> "";
        };
    }

    private String describe(BattleMatchReport.Event event) {
        return switch (event.kind()) {
            case HIT -> event.action() + " : -" + event.damage() + " HP" + (event.critical() ? " CRIT" : "") + " → " + event.targetHp() + " HP";
            case MISS -> event.action() + " : " + Text.translatable("autoptu.battle.miss").getString();
            case MOVEMENT -> Text.translatable("autoptu.battle.movement").getString() + " (" + event.from().x() + "," + event.from().y()
                    + ") → (" + event.to().x() + "," + event.to().y() + ")";
            case TURN -> Text.translatable("autoptu.battle.turn_start").getString();
            case PASS -> Text.translatable("autoptu.battle.end_turn").getString();
            case START -> Text.translatable("autoptu.battle.match_start").getString();
            case END -> Text.translatable("autoptu.battle.outcome." + event.action().toLowerCase(java.util.Locale.ROOT)).getString();
            case ATTACK -> event.action();
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
