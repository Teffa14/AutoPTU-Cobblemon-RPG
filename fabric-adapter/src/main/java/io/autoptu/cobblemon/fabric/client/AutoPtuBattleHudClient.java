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

import java.util.ArrayList;
import java.util.List;

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
    private static final int LOG_WIDTH = 169;
    private static final int LOG_HEIGHT = 55;
    private static final int LOG_TEXT_WIDTH = 151;
    private static final int LOG_LINE_HEIGHT = 10;
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
            renderStatusBadge(context, text, combatant.statusId(), x, y, reversed);
        }
    }

    private static void renderStatusBadge(
            DrawContext context,
            TextRenderer text,
            String statusId,
            int x,
            int y,
            boolean reversed
    ) {
        String status = trim(statusId.toUpperCase(), 8);
        int padding = 3;
        int badgeWidth = Math.min(42, text.getWidth(status) + padding * 2);
        int badgeX = reversed ? x + TILE_WIDTH - 8 - badgeWidth : x + 8;
        int badgeY = y + 25;
        int badgeHeight = 10;
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, 0xD921242A);
        context.fill(badgeX, badgeY, badgeX + 2, badgeY + badgeHeight, statusAccent(statusId));
        context.drawTextWithShadow(text, status, badgeX + padding, badgeY + 1, 0xFFF2F2F2);
    }

    private static void renderMessage(
            DrawContext context,
            TextRenderer text,
            FabricBattleHudPayload hud,
            int screenWidth,
            int screenHeight
    ) {
        if (hud.message().isBlank() && !hud.testMode()) return;
        int x = screenWidth - LOG_WIDTH - INSET;
        int y = screenHeight - LOG_HEIGHT - 30;
        context.drawTexture(LOG_FRAME, x, y, 0.0F, 0.0F, LOG_WIDTH, LOG_HEIGHT, LOG_WIDTH, LOG_HEIGHT);
        String message = hud.message().isBlank() ? "Animation lab" : hud.message();
        int maxLines = hud.testMode() ? 3 : 4;
        List<String> lines = wrapMessage(text, message, LOG_TEXT_WIDTH, maxLines);
        for (int index = 0; index < lines.size(); index++) {
            context.drawTextWithShadow(text, lines.get(index), x + 8, y + 7 + index * LOG_LINE_HEIGHT, 0xFFFFFF);
        }
        if (hud.testMode()) {
            context.fill(x + 7, y + 39, x + 91, y + 50, 0xB321242A);
            context.fill(x + 7, y + 39, x + 9, y + 50, 0xFFFFB84D);
            context.drawTextWithShadow(text, "QA VISUAL ONLY", x + 12, y + 40, 0xFFFFD38A);
        }
    }

    static int hpColor(float ratio) {
        if (ratio > 0.5F) return 0xFF55C65A;
        if (ratio > 0.2F) return 0xFFE2C74D;
        return 0xFFE05252;
    }

    static List<String> wrapMessage(TextRenderer text, String value, int maxWidth, int maxLines) {
        if (value == null || value.isBlank() || maxWidth <= 0 || maxLines <= 0) return List.of();
        String[] words = value.trim().split("\\s+");
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (text.getWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                if (lines.size() >= maxLines) break;
                current.setLength(0);
            }
            if (text.getWidth(word) <= maxWidth) {
                current.append(word);
            } else {
                current.append(trimToWidth(text, word, maxWidth));
            }
        }
        if (lines.size() < maxLines && !current.isEmpty()) lines.add(current.toString());
        if (lines.size() == maxLines && words.length > 0) {
            int last = lines.size() - 1;
            String valueAtLast = lines.get(last);
            if (text.getWidth(value) > maxWidth * maxLines && !valueAtLast.endsWith("…")) {
                lines.set(last, trimToWidth(text, valueAtLast + "…", maxWidth));
            }
        }
        return List.copyOf(lines);
    }

    private static String trimToWidth(TextRenderer text, String value, int maxWidth) {
        if (text.getWidth(value) <= maxWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && text.getWidth(value.substring(0, end) + suffix) > maxWidth) end--;
        return value.substring(0, end) + suffix;
    }

    private static int statusAccent(String statusId) {
        String normalized = statusId == null ? "" : statusId.trim().toLowerCase();
        return switch (normalized) {
            case "burn", "burned" -> 0xFFE8733A;
            case "poison", "poisoned", "badly_poisoned" -> 0xFFB66BD7;
            case "paralysis", "paralyzed" -> 0xFFE2C74D;
            case "sleep", "asleep" -> 0xFF7F8EA3;
            case "freeze", "frozen" -> 0xFF68C7DE;
            default -> 0xFFFFB84D;
        };
    }

    private static String trim(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
