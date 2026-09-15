package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattlePracticeScenario;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Setup is cosmetic until the authenticated server accepts the start command. */
public final class BattlePracticeScreen extends Screen {
    private final boolean operator;
    private int scenarioIndex;
    private String ally = "bulbasaur";
    private String enemy = "pikachu";
    private String seed = "20260823";
    private TextFieldWidget allyField;
    private TextFieldWidget enemyField;
    private TextFieldWidget seedField;
    private ButtonWidget start;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public BattlePracticeScreen(boolean operator) {
        super(Text.translatable("autoptu.practice.title"));
        this.operator = operator;
    }

    @Override protected void init() {
        panelWidth = Math.min(490, width - 20);
        panelHeight = Math.min(300, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), button -> {
            if (client != null) client.setScreen(new BattleGuideScreen(this));
        }).dimensions(left + panelWidth - 30, top + 6, 20, 20).build());
        int fieldWidth = (panelWidth - 30) / 2;
        allyField = field(left + 10, top + 49, fieldWidth, "autoptu.practice.ally", ally);
        enemyField = field(left + 20 + fieldWidth, top + 49, fieldWidth, "autoptu.practice.enemy", enemy);
        allyField.setChangedListener(value -> { ally = value; updateStart(); });
        enemyField.setChangedListener(value -> { enemy = value; updateStart(); });
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> cycle(-1))
                .dimensions(left + 10, top + 80, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> cycle(1))
                .dimensions(left + panelWidth - 34, top + 80, 24, 20).build());
        seedField = field(left + 10, top + panelHeight - 60, Math.max(90, panelWidth / 2 - 20),
                "autoptu.practice.seed", seed);
        seedField.setMaxLength(20);
        seedField.setTooltip(Tooltip.of(Text.translatable("autoptu.practice.seed_help")));
        seedField.setChangedListener(value -> { seed = value; updateStart(); });
        addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.practice.random"), button ->
                seedField.setText(Long.toString(java.util.concurrent.ThreadLocalRandom.current().nextLong())))
                .dimensions(left + panelWidth / 2, top + panelHeight - 60, panelWidth / 2 - 10, 20).build());
        start = addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.practice.start"), button -> startBattle())
                .dimensions(left + 10, top + panelHeight - 28, panelWidth / 2 - 15, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(left + panelWidth / 2 + 5, top + panelHeight - 28, panelWidth / 2 - 15, 20).build());
        updateStart();
        setInitialFocus(allyField);
    }

    private TextFieldWidget field(int x, int y, int fieldWidth, String label, String initial) {
        var field = new TextFieldWidget(textRenderer, x, y, fieldWidth, 20, Text.translatable(label));
        field.setMaxLength(64);
        field.setText(initial);
        return addDrawableChild(field);
    }

    private void cycle(int direction) {
        scenarioIndex = Math.floorMod(scenarioIndex + direction, BattlePracticeScenario.CATALOG.size());
    }

    private boolean valid() {
        if (!ally.matches("[a-z0-9_-]{1,64}") || !enemy.matches("[a-z0-9_-]{1,64}")) return false;
        try { Long.parseLong(seed); return true; }
        catch (NumberFormatException ignored) { return false; }
    }

    private void updateStart() {
        if (start != null) {
            start.active = operator && valid();
            start.setTooltip(Tooltip.of(Text.translatable(operator ? "autoptu.practice.disclaimer" : "autoptu.practice.operator")));
        }
    }

    private void startBattle() {
        if (!operator || !valid() || client == null || client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendChatCommand("autoptu admin battle play " + ally + " " + enemy + " "
                + BattlePracticeScenario.CATALOG.get(scenarioIndex).id() + " " + seed);
        close();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF0111C28);
        context.drawBorder(left, top, panelWidth, panelHeight, 0xFF375568);
        context.drawTextWithShadow(textRenderer, title, left + 10, top + 10, 0xFFEAF2F5);
        context.drawTextWithShadow(textRenderer, Text.translatable("autoptu.practice.ally"), left + 10, top + 36, 0xFF70D8AA);
        context.drawTextWithShadow(textRenderer, Text.translatable("autoptu.practice.enemy"), left + 20 + (panelWidth - 30) / 2,
                top + 36, 0xFFF3A080);
        var scenario = BattlePracticeScenario.CATALOG.get(scenarioIndex);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("autoptu.practice.scenario." + scenario.id()),
                width / 2, top + 86, 0xFFFFD068);
        int availableHeight = panelHeight - 181;
        if (availableHeight >= 24) {
            int cell = Math.max(2, Math.min(16, availableHeight / scenario.height()));
            int gridX = left + 12;
            int gridY = top + 109;
            for (int row = 0; row < scenario.height(); row++) {
                for (int col = 0; col < scenario.width(); col++) {
                    int color = 0xFF213B48;
                    if (col == scenario.allyStart().x() && row == scenario.allyStart().y()) color = 0xFF70D8AA;
                    if (col == scenario.enemyStart().x() && row == scenario.enemyStart().y()) color = 0xFFF3A080;
                    context.fill(gridX + col * cell, gridY + row * cell, gridX + (col + 1) * cell - 1,
                            gridY + (row + 1) * cell - 1, color);
                }
            }
            int textX = gridX + scenario.width() * cell + 12;
            int wrapWidth = Math.max(30, left + panelWidth - textX - 10);
            context.drawTextWithShadow(textRenderer, scenario.width() + " x " + scenario.height() + " | HP " + scenario.hp(),
                    textX, gridY, 0xFFEAF2F5);
            int lineY = gridY + 14;
            for (var line : textRenderer.wrapLines(Text.translatable("autoptu.practice.description." + scenario.id()), wrapWidth)) {
                if (lineY + 9 > top + panelHeight - 76) break;
                context.drawTextWithShadow(textRenderer, line, textX, lineY, 0xFFA1B5C3);
                lineY += 10;
            }
        }
        context.drawTextWithShadow(textRenderer, Text.translatable(operator ? "autoptu.practice.seed" : "autoptu.practice.operator"),
                left + 10, top + panelHeight - 72, operator ? 0xFFA1B5C3 : 0xFFF3A080);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
