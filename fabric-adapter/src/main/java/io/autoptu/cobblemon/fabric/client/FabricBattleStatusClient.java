package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattleHealthDisplay;
import io.autoptu.cobblemon.fabric.network.FabricBattleStatusPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Animated read-only health display. Numeric HP always comes directly from the server. */
public final class FabricBattleStatusClient implements ClientModInitializer {
    private static FabricBattleStatusPayload state;
    private static BattleHealthDisplay ally = new BattleHealthDisplay();
    private static BattleHealthDisplay enemy = new BattleHealthDisplay();
    private static long tick, expires, phaseStarted;
    private static String phaseKey = "";

    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleStatusPayload.ID, (payload, context) -> {
            if (payload.phase().isBlank()) { reset(); return; }
            if (state == null || !state.allyName().equals(payload.allyName()) || !state.enemyName().equals(payload.enemyName())) {
                ally = new BattleHealthDisplay();
                enemy = new BattleHealthDisplay();
            }
            String nextPhase = phaseKey(payload.phase());
            if (!nextPhase.equals(phaseKey) || state == null || state.round() != payload.round()) phaseStarted = tick;
            phaseKey = nextPhase;
            ally.accept(payload.allyHp(), payload.allyMaxHp());
            enemy.accept(payload.enemyHp(), payload.enemyMaxHp());
            state = payload;
            expires = tick + 80;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            if (tick > expires || client.world == null) reset();
            if (state != null) { ally.tick(); enemy.tick(); }
        });
        HudRenderCallback.EVENT.register((context, counter) -> render(context));
    }

    private static void reset() {
        state = null;
        phaseKey = "";
        ally = new BattleHealthDisplay();
        enemy = new BattleHealthDisplay();
    }

    private static String phaseKey(String phase) {
        if (phase.startsWith("YOUR TURN")) return "your_turn";
        return switch (phase) {
            case "RIVAL TURN" -> "rival_turn";
            case "ATTACK WINDUP" -> "windup";
            case "IMPACT" -> "impact";
            case "VICTORY" -> "victory";
            case "DEFEAT" -> "defeat";
            default -> "active";
        };
    }

    private static void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (state == null || client.options.hudHidden || client.currentScreen != null) return;
        int width = context.getScaledWindowWidth();
        int panelWidth = Math.min(188, (width - 30) / 2);
        if (panelWidth < 64) return;
        boolean yourTurn = phaseKey.equals("your_turn");
        boolean rivalTurn = phaseKey.equals("rival_turn");
        panel(context, 10, 10, panelWidth, state.allyName(), state.allyHp(), state.allyMaxHp(),
                0xFF70D8AA, ally, yourTurn);
        panel(context, width - panelWidth - 10, 10, panelWidth, state.enemyName(), state.enemyHp(),
                state.enemyMaxHp(), 0xFFF3A080, enemy, rivalTurn);
        Text phase = Text.translatable("autoptu.hud." + phaseKey);
        String line = Text.translatable("autoptu.hud.round", state.round()).getString() + "  /  " + phase.getString();
        line = client.textRenderer.trimToWidth(line, width - 40);
        int length = client.textRenderer.getWidth(line);
        int accent = phaseKey.equals("defeat") ? 0xFFF3A080 : phaseKey.equals("victory") ? 0xFFFFD068 : 0xFF70D8AA;
        context.fill(width / 2 - length / 2 - 12, 70, width / 2 + length / 2 + 12, 90, 0xEB111C28);
        context.fill(width / 2 - length / 2 - 12, 88, width / 2 + length / 2 + 12, 90, accent);
        context.drawCenteredTextWithShadow(client.textRenderer, line, width / 2, 76, 0xFFEAF2F5);
        // A brief static toast avoids flashing, camera shake, or relying on color alone.
        if (tick - phaseStarted < 36 && (yourTurn || rivalTurn || phaseKey.equals("victory") || phaseKey.equals("defeat"))) {
            Text hint = Text.translatable("autoptu.hud.hint." + phaseKey);
            String clipped = client.textRenderer.trimToWidth(hint.getString(), width - 32);
            context.drawCenteredTextWithShadow(client.textRenderer, clipped, width / 2, 97, accent);
        }
    }

    private static void panel(DrawContext context, int x, int y, int width, String name, int hp, int max,
                              int accent, BattleHealthDisplay health, boolean active) {
        var text = MinecraftClient.getInstance().textRenderer;
        context.fill(x + 2, y + 3, x + width + 2, y + 54, 0x55080F17);
        context.fill(x, y, x + width, y + 52, 0xEA111C28);
        context.fill(x, y, x + 3, y + 52, accent);
        if (active) context.drawBorder(x, y, width, 52, accent);
        if (health.recentImpact() && health.change() < 0 && health.impactAge() < 6) {
            context.fill(x + 3, y + 1, x + width - 1, y + 51, 0x453B1D2A);
        }
        context.drawText(text, text.trimToWidth(name, width - 18), x + 9, y + 7, 0xFFEAF2F5, false);
        String value = hp + " / " + max;
        context.drawText(text, text.trimToWidth(value, width - 48), x + 9, y + 20, 0xFFB8CBD9, false);
        if (hp == 0) {
            context.drawText(text, "KO", x + width - 22, y + 20, 0xFFF3A080, false);
        } else if (health.recentImpact()) {
            String change = (health.change() > 0 ? "+" : "") + health.change();
            context.drawText(text, change, x + width - text.getWidth(change) - 8, y + 20,
                    health.change() < 0 ? 0xFFF3A080 : 0xFF70D8AA, false);
        }
        int barWidth = width - 18;
        int barX = x + 9;
        context.fill(barX, y + 35, barX + barWidth, y + 43, 0xFF263A49);
        context.fill(barX, y + 35, barX + Math.round(barWidth * health.trail()), y + 43, 0xFFB79367);
        float ratio = hp / (float) max;
        int color = ratio <= 0.25F ? 0xFFEE7377 : ratio <= 0.5F ? 0xFFE6C679 : accent;
        context.fill(barX, y + 35, barX + Math.round(barWidth * health.displayed()), y + 43, color);
        context.fill(barX, y + 35, barX + Math.round(barWidth * health.displayed()), y + 37, 0x33FFFFFF);
        for (int i = 1; i < 4; i++) {
            int notch = barX + barWidth * i / 4;
            context.fill(notch, y + 35, notch + 1, y + 43, 0x77111C28);
        }
    }
}
