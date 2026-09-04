package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * AutoPTU-owned battle overlay using Cobblemon 1.7.3 battle artwork and layout language.
 *
 * The client never reads CobblemonClient.battle or Pokemon gameplay fields. It renders only the
 * server-authored payload. Unknown authoritative max HP is shown as current HP without a ratio.
 */
@SuppressWarnings("deprecation")
public final class AutoPtuBattleHudClient implements ClientModInitializer {
    private static final Identifier LEFT_TILE = Identifier.of("cobblemon", "textures/gui/battle/battle_info_base.png");
    private static final Identifier RIGHT_TILE = Identifier.of("cobblemon", "textures/gui/battle/battle_info_base_flipped.png");
    private static final Identifier LOG_FRAME = Identifier.of("cobblemon", "textures/gui/battle/battle_log.png");
    private static final int TILE_WIDTH = 140;
    private static final int TILE_HEIGHT = 40;
    private static final int INSET = 12;
    private static final int HP_BAR_WIDTH = 72;
    private static final int HP_BAR_HEIGHT = 5;
    private static volatile FabricBattleHudPayload state = FabricBattleHudPayload.hidden();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleHudPayload.ID, (payload, context) -> state = payload);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> state = FabricBattleHudPayload.hidden());
        HudRenderCallback.EVENT.register(AutoPtuBattleHudClient::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        FabricBattleHudPayload hud = state;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hud.visible() || client.player == null || client.options.hudHidden) return;

        int width = client.getWindow().getScaledWidth();
        renderCombatant(context, client.textRenderer, hud.left(), INSET, 10, false);
        renderCombatant(context, client.textRenderer, hud.right(), width - INSET - TILE_WIDTH, 10, true);
        renderMessage(context, client.textRenderer, hud, width, client.getWindow().getScaledHeight());
    }

    private static void renderCombatant(
            DrawContext context,
            TextRenderer text,
            FabricBattleHudPayload.Combatant combatant,
            int x,
            int y,
            boolean reversed
    ) {
        Identifier tile = reversed ? RIGHT_TILE : LEFT_TILE;
        context.drawTexture(tile, x, y, 0.0F, 0.0F, TILE_WIDTH, TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);

        int textX = reversed ? x + 10 : x + 38;
        String name = combatant.displayName().isBlank() ? "AutoPTU" : combatant.displayName();
        context.drawTextWithShadow(text, name, textX, y + 6, 0xFFFFFF);
        if (combatant.level() > 0) {
            String level = "Lv. " + combatant.level();
            int levelX = reversed ? x + TILE_WIDTH - 10 - text.getWidth(level) : x + 38;
            context.drawTextWithShadow(text, level, levelX, y + 17, 0xD9D9D9);
        }

        int barX = reversed ? x + 16 : x + 52;
        int barY = y + 29;
        context.fill(barX - 1, barY - 1, barX + HP_BAR_WIDTH + 1, barY + HP_BAR_HEIGHT + 1, 0xD9000000);
        if (combatant.hasKnownMaxHp()) {
            int fill = Math.max(0, Math.min(HP_BAR_WIDTH, Math.round(HP_BAR_WIDTH * combatant.hpRatio())));
            context.fill(barX, barY, barX + HP_BAR_WIDTH, barY + HP_BAR_HEIGHT, 0xFF30343B);
            context.fill(barX, barY, barX + fill, barY + HP_BAR_HEIGHT, hpColor(combatant.hpRatio()));
        } else {
            // Indeterminate current-only bar. Deliberately no fabricated ratio.
            context.fill(barX, barY, barX + HP_BAR_WIDTH, barY + HP_BAR_HEIGHT, 0xFF50555D);
            for (int offset = 0; offset < HP_BAR_WIDTH; offset += 8) {
                context.fill(barX + offset, barY, Math.min(barX + offset + 4, barX + HP_BAR_WIDTH), barY + HP_BAR_HEIGHT, 0xFF737983);
            }
        }

        String hp = combatant.hasKnownMaxHp()
                ? combatant.currentHp() + "/" + combatant.maxHp()
                : "HP " + combatant.currentHp() + "/?";
        int hpX = reversed ? barX : barX + HP_BAR_WIDTH - text.getWidth(hp);
        context.drawTextWithShadow(text, hp, hpX, y + 34, 0xFFFFFF);

        if (!combatant.statusId().isBlank()) {
            String status = combatant.statusId().toUpperCase();
            int statusX = reversed ? x + TILE_WIDTH - 10 - text.getWidth(status) : x + 8;
            context.drawTextWithShadow(text, status, statusX, y + 27, 0xFFE08A);
        }
    }

    private static void renderMessage(
            DrawContext context,
            TextRenderer text,
            FabricBattleHudPayload hud,
            int screenWidth,
            int screenHeight
    ) {
        if (hud.message().isBlank() && !hud.testMode()) return;
        int frameWidth = 169;
        int frameHeight = 55;
        int x = screenWidth - frameWidth - INSET;
        int y = screenHeight - frameHeight - 30;
        context.drawTexture(LOG_FRAME, x, y, 0.0F, 0.0F, frameWidth, frameHeight, frameWidth, frameHeight);
        String message = hud.message().isBlank() ? "Animation lab" : hud.message();
        context.drawTextWithShadow(text, trim(message, 150), x + 8, y + 10, 0xFFFFFF);
        if (hud.testMode()) {
            context.drawTextWithShadow(text, "QA VISUAL ONLY", x + 8, y + 27, 0xFFE46D);
        }
    }

    static int hpColor(float ratio) {
        if (ratio > 0.5F) return 0xFF55C65A;
        if (ratio > 0.2F) return 0xFFE2C74D;
        return 0xFFE05252;
    }

    private static String trim(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
