package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import io.autoptu.cobblemon.fabric.network.FabricBattleMenuPayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleSelectionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** In-world tactical action selector. Only server-provided choices are displayed. */
public final class FabricBattleMenuClient implements ClientModInitializer {
    private static String confirmToken = "";
    private static long tokenExpiry;
    private static long tick;
    @Override public void onInitializeClient() {
        KeyBinding battleMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_actions", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.autoptu"));
        KeyBinding confirm = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_confirm", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, "key.categories.autoptu"));
        KeyBinding cancel = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_cancel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSPACE, "key.categories.autoptu"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            if (tick > tokenExpiry || client.world == null) confirmToken = "";
            while (battleMenu.wasPressed()) {
                if (client.getNetworkHandler() != null && client.currentScreen == null) {
                    client.getNetworkHandler().sendChatCommand("autoptu battle menu");
                }
            }
            while (confirm.wasPressed()) {
                if (!confirmToken.isEmpty() && client.currentScreen == null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("autoptu battle confirm " + confirmToken);
                    confirmToken = "";
                }
            }
            while (cancel.wasPressed()) {
                if (!confirmToken.isEmpty() && client.currentScreen == null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("autoptu battle cancel");
                    confirmToken = "";
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleMenuPayload.ID,
                (payload, context) -> context.client().setScreen(new BattleScreen(payload.entries(), payload.canEndTurn())));
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleSelectionPayload.ID, (payload, context) -> {
            confirmToken = payload.token(); tokenExpiry = tick + 300;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> confirmToken = "");
        HudRenderCallback.EVENT.register((context, counter) -> {
            var client = MinecraftClient.getInstance();
            if (confirmToken.isEmpty() || client.options.hudHidden || client.currentScreen != null) return;
            int x = context.getScaledWindowWidth() / 2, y = context.getScaledWindowHeight() - 94;
            context.fill(x - 128, y - 4, x + 128, y + 15, 0xDD182537);
            context.drawCenteredTextWithShadow(client.textRenderer, "ENTER Confirm   /   BACKSPACE Cancel", x, y + 2, 0xFFFFD15A);
        });
    }

    private static final class BattleScreen extends Screen {
        private final List<BattleChoiceMenuService.Entry> entries;
        private final boolean canEndTurn;
        private boolean movement;
        private int page;
        private int rows, panelWidth, panelHeight, left, top;
        BattleScreen(List<BattleChoiceMenuService.Entry> entries, boolean canEndTurn) {
            super(Text.literal("Battle actions"));
            this.entries = List.copyOf(entries);
            this.canEndTurn = canEndTurn;
        }
        private List<BattleChoiceMenuService.Entry> filtered() {
            return entries.stream().filter(entry -> entry.label().startsWith("Shift to ") == movement).toList();
        }
        @Override protected void init() {
            panelWidth = Math.min(320, width - 32);
            rows = Math.max(1, Math.min(8, (height - 112) / 20));
            panelHeight = 102 + rows * 20;
            left = (width - panelWidth) / 2;
            top = (height - panelHeight) / 2 + 8;
            int half = (panelWidth - 6) / 2;
            var visible = filtered();
            page = Math.max(0, Math.min(page, Math.max(0, (visible.size() - 1) / rows)));
            addDrawableChild(ButtonWidget.builder(Text.literal(movement ? "ATTACKS" : "[ ATTACKS ]"), button -> {
                movement = false; page = 0; clearAndInit();
            }).dimensions(left, top + 30, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(movement ? "[ MOVEMENT ]" : "MOVEMENT"), button -> {
                movement = true; page = 0; clearAndInit();
            }).dimensions(left + half + 6, top + 30, panelWidth - half - 6, 20).build());
            for (int row = 0; row < rows; row++) {
                int index = page * rows + row;
                if (index >= visible.size()) break;
                var entry = visible.get(index);
                addDrawableChild(ButtonWidget.builder(Text.literal(entry.label()), button -> {
                    if (client != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("autoptu battle preview " + entry.choiceId());
                        close();
                    }
                }).dimensions(left, top + 55 + row * 20, panelWidth, 19)
                        .tooltip(Tooltip.of(Text.literal(entry.label() + "\nPreview first; Enter confirms on the field."))).build());
            }
            int footer = top + 58 + rows * 20;
            var previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                page--; clearAndInit();
            }).dimensions(left, footer, 28, 20).build());
            previous.active = page > 0;
            var next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                page++; clearAndInit();
            }).dimensions(left + panelWidth - 28, footer, 28, 20).build());
            next.active = (page + 1) * rows < visible.size();
            if (canEndTurn) {
                int buttonWidth = (panelWidth - 78) / 2;
                addDrawableChild(ButtonWidget.builder(Text.literal("End turn"), button -> {
                    if (client != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("autoptu battle endturn");
                        close();
                    }
                }).dimensions(left + 36, footer, buttonWidth, 20)
                        .tooltip(Tooltip.of(Text.literal("Pass unused actions to the opponent."))).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                        .dimensions(left + 42 + buttonWidth, footer, buttonWidth, 20).build());
            } else {
                addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                        .dimensions(left + panelWidth / 2 - 40, footer, 80, 20).build());
            }
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(left - 10, top - 10, left + panelWidth + 10, top + panelHeight - 10, 0xED101B2A);
            context.fill(left - 10, top - 10, left + panelWidth + 10, top - 8, movement ? 0xFF58DFA1 : 0xFFF49668);
            context.drawText(textRenderer, "BATTLE / CHOOSE AN ACTION", left, top, 0xFFF1F6FC, false);
            context.drawText(textRenderer, textRenderer.trimToWidth("Select an action to preview it on the field.", panelWidth), left, top + 14, 0xFF9DB8C9, false);
            if (filtered().isEmpty()) context.drawCenteredTextWithShadow(textRenderer, "No available choices", width / 2, top + 61, 0xFF9DB8C9);
            int totalPages = Math.max(1, (filtered().size() + rows - 1) / rows);
            context.drawCenteredTextWithShadow(textRenderer, "Page " + (page + 1) + " / " + totalPages,
                    width / 2, top + 81 + rows * 20, 0xFF9DB8C9);
            super.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean shouldPause() { return false; }
    }
}
