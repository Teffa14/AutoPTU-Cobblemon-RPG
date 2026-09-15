package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleStatusPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FabricBattleStatusClient implements ClientModInitializer {
    private static FabricBattleStatusPayload state;
    private static long tick, expires;
    private static int allyDelta, enemyDelta;
    private static long allyImpactExpires, enemyImpactExpires;
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleStatusPayload.ID, (payload, context) -> {
            if (state != null && !payload.phase().isBlank()) {
                if (payload.allyName().equals(state.allyName()) && payload.allyHp() != state.allyHp()) {
                    allyDelta = payload.allyHp() - state.allyHp(); allyImpactExpires = tick + 30;
                }
                if (payload.enemyName().equals(state.enemyName()) && payload.enemyHp() != state.enemyHp()) {
                    enemyDelta = payload.enemyHp() - state.enemyHp(); enemyImpactExpires = tick + 30;
                }
            } else { allyImpactExpires = 0; enemyImpactExpires = 0; }
            state = payload.phase().isBlank() ? null : payload; expires = tick + 80;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> state = null);
        ClientTickEvents.END_CLIENT_TICK.register(client -> { if (++tick > expires || client.world == null) state = null; });
        HudRenderCallback.EVENT.register((context, counter) -> {
            var client = MinecraftClient.getInstance();
            if (state == null || client.options.hudHidden) return;
            int width = context.getScaledWindowWidth();
            int panelWidth = Math.min(170, (width - 36) / 2);
            panel(context, 12, 12, panelWidth, state.allyName(), state.allyHp(), state.allyMaxHp(), 0xFF4BD6AB,
                    allyDelta, allyImpactExpires);
            panel(context, width - panelWidth - 12, 12, panelWidth, state.enemyName(), state.enemyHp(), state.enemyMaxHp(), 0xFFF08364,
                    enemyDelta, enemyImpactExpires);
            String phase = "ROUND " + state.round() + "  /  " + state.phase();
            phase = client.textRenderer.trimToWidth(phase, width - 32);
            int length = client.textRenderer.getWidth(phase);
            context.fill(width / 2 - length / 2 - 10, 59, width / 2 + length / 2 + 10, 76, 0xD5101B2A);
            context.drawCenteredTextWithShadow(client.textRenderer, phase, width / 2, 64, 0xFFF0F5FC);
        });
    }
    private static void panel(DrawContext context, int x, int y, int width, String name, int hp, int max, int accent,
                              int delta, long impactExpires) {
        var text = MinecraftClient.getInstance().textRenderer;
        boolean recentImpact = tick < impactExpires;
        context.fill(x, y, x + width, y + 41, recentImpact && delta < 0 ? 0xDB3B1D2A : 0xDB101B2A);
        context.fill(x, y, x + 2, y + 41, accent);
        context.drawText(text, text.trimToWidth(name, width - 16), x + 8, y + 6, 0xFFF4F8FC, false);
        String health = hp + " / " + max + " HP";
        context.drawText(text, health, x + 8, y + 18, 0xFFB8CBD9, false);
        if (recentImpact) {
            String change = (delta > 0 ? "+" : "") + delta;
            context.drawText(text, change, x + width - text.getWidth(change) - 8, y + 18,
                    delta < 0 ? 0xFFFF8E82 : 0xFF72EDB5, false);
        }
        context.fill(x + 8, y + 31, x + width - 8, y + 35, 0xFF2B3E51);
        float ratio = Math.max(0, Math.min(1, hp / (float) Math.max(1, max)));
        int color = ratio <= 0.25F ? 0xFFEE5E63 : ratio <= 0.5F ? 0xFFF4C95A : accent;
        context.fill(x + 8, y + 31, x + 8 + Math.round((width - 16) * ratio), y + 35, color);
    }
}
