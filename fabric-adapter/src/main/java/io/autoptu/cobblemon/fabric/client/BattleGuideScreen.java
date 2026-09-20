package io.autoptu.cobblemon.fabric.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** Read-only illustrated guide. Copying a command never executes it. */
public final class BattleGuideScreen extends Screen {
    private static final List<String> COMMANDS = List.of(
            "/autoptu battle practice", "/autoptu admin battle play charmander pikachu",
            "/autoptu admin battle play lucario gengar distance 12345", "/autoptu battle menu",
            "/autoptu battle report", "/autoptu admin battle stop");
    private final Screen parent;
    private int page;
    private int scroll;
    private int left, top, panelWidth, panelHeight, bodyWidth, visibleLines;
    private List<OrderedText> lines = List.of();
    private boolean copied;

    public BattleGuideScreen(Screen parent) {
        super(Text.translatable("autoptu.guide.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        panelWidth = Math.min(600, width - 20);
        panelHeight = Math.min(350, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        bodyWidth = panelWidth - (panelWidth >= 460 && panelHeight >= 260 ? 160 : 24);
        visibleLines = Math.max(1, (panelHeight - 139) / 12);
        lines = textRenderer.wrapLines(Text.translatable("autoptu.guide.page." + page + ".body"), bodyWidth);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visibleLines)));
        int footer = top + panelHeight - 29;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> turn(-1))
                .dimensions(left + 10, footer, 24, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> turn(1))
                .dimensions(left + 39, footer, 24, 20).build()).active = page + 1 < COMMANDS.size();
        addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.guide.copy"), b -> {
            if (client != null) { client.keyboard.setClipboard(COMMANDS.get(page)); copied = true; }
        }).dimensions(left + 10, footer - 27, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("autoptu.guide.setup"), b -> {
            if (client != null && client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand("autoptu battle practice");
                client.setScreen(null);
            }
        }).dimensions(left + panelWidth - 174, footer, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(left + panelWidth - 77, footer, 67, 20).build());
    }

    private void turn(int direction) {
        page = Math.max(0, Math.min(COMMANDS.size() - 1, page + direction));
        scroll = 0;
        copied = false;
        clearAndInit();
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT) { turn(-1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { turn(1); return true; }
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            moveScroll(key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void moveScroll(int delta) {
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visibleLines), scroll + delta));
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        moveScroll(vertical > 0 ? -2 : vertical < 0 ? 2 : 0);
        return true;
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF2111C28);
        context.drawBorder(left, top, panelWidth, panelHeight, 0xFF375568);
        context.fill(left, top, left + panelWidth * (page + 1) / COMMANDS.size(), top + 2, 0xFF70D8AA);
        context.drawTextWithShadow(textRenderer, title, left + 12, top + 12, 0xFFA1B5C3);
        context.drawTextWithShadow(textRenderer, (page + 1) + " / " + COMMANDS.size(), left + panelWidth - 48, top + 12, 0xFF70D8AA);
        context.drawTextWithShadow(textRenderer, Text.translatable("autoptu.guide.page." + page + ".title"),
                left + 12, top + 33, 0xFFFFD068);
        for (int i = 0; i < visibleLines && scroll + i < lines.size(); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(scroll + i), left + 12, top + 55 + i * 12, 0xFFEAF2F5);
        }
        if (lines.size() > visibleLines) {
            int trackX = left + 16 + bodyWidth;
            int trackHeight = visibleLines * 12;
            context.fill(trackX, top + 54, trackX + 2, top + 54 + trackHeight, 0xFF375568);
            int thumb = Math.max(6, trackHeight * visibleLines / lines.size());
            int offset = (trackHeight - thumb) * scroll / Math.max(1, lines.size() - visibleLines);
            context.fill(trackX, top + 54 + offset, trackX + 2, top + 54 + offset + thumb, 0xFF70D8AA);
        }
        if (panelWidth >= 460 && panelHeight >= 260) drawExample(context, left + panelWidth - 133, top + 58);
        context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(COMMANDS.get(page), panelWidth - 24),
                left + 12, top + panelHeight - 76, 0xFF70D8AA);
        if (copied) context.drawTextWithShadow(textRenderer, Text.translatable("autoptu.guide.copied"),
                left + 132, top + panelHeight - 50, 0xFF70D8AA);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawExample(DrawContext context, int x, int y) {
        for (int row = 0; row < 4; row++) for (int col = 0; col < 6; col++) {
            boolean destination = page >= 3 && col == 2 && row == 1;
            int color = destination ? 0xFFFFD068 : page == 3 && col < 3 ? 0xFF315C4F : 0xFF213B48;
            context.fill(x + col * 19, y + row * 19, x + col * 19 + 17, y + row * 19 + 17, color);
        }
        context.drawCenteredTextWithShadow(textRenderer, "P", x + 27, y + 24, 0xFF70D8AA);
        context.drawCenteredTextWithShadow(textRenderer, "X", x + 84, y + 24, 0xFFF3A080);
        context.drawTextWithShadow(textRenderer, Text.translatable("autoptu.guide.diagram"), x, y + 85, 0xFFA1B5C3);
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
