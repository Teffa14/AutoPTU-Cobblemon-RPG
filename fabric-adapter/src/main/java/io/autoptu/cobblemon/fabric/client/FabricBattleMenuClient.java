package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleMenuPayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleSelectionPayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleReportPayload;
import io.autoptu.cobblemon.fabric.network.FabricBattlePracticePayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleGuidePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** In-world tactical action selector. Only server-provided choices are displayed. */
public final class FabricBattleMenuClient implements ClientModInitializer {
    private static String confirmToken = "";
    private static long tokenExpiry;
    private static long tick;
    @Override public void onInitializeClient() {
        KeyBinding report = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_report", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.autoptu"));
        KeyBinding battleMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_actions", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.autoptu"));
        KeyBinding confirm = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_confirm", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, "key.categories.autoptu"));
        KeyBinding cancel = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoptu.battle_cancel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSPACE, "key.categories.autoptu"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            while (report.wasPressed()) {
                if (client.getNetworkHandler() != null && client.currentScreen == null) {
                    client.getNetworkHandler().sendChatCommand("autoptu battle report");
                }
            }
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
                (payload, context) -> context.client().setScreen(new BattleTacticalScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleReportPayload.ID,
                (payload, context) -> context.client().setScreen(new BattleReportScreen(payload.report())));
        ClientPlayNetworking.registerGlobalReceiver(FabricBattlePracticePayload.ID,
                (payload, context) -> context.client().setScreen(new BattlePracticeScreen(payload.operator())));
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleGuidePayload.ID,
                (payload, context) -> context.client().setScreen(new BattleGuideScreen(context.client().currentScreen)));
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleSelectionPayload.ID, (payload, context) -> {
            confirmToken = payload.token(); tokenExpiry = tick + 300;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> confirmToken = "");
        HudRenderCallback.EVENT.register((context, counter) -> {
            var client = MinecraftClient.getInstance();
            if (confirmToken.isEmpty() || client.options.hudHidden || client.currentScreen != null) return;
            int x = context.getScaledWindowWidth() / 2, y = context.getScaledWindowHeight() - 94;
            context.fill(x - 128, y - 4, x + 128, y + 15, 0xDD182537);
            context.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.translatable("autoptu.battle.confirm_keys",
                    confirm.getBoundKeyLocalizedText(), cancel.getBoundKeyLocalizedText()), x, y + 2, 0xFFFFD15A);
        });
    }

    public static void preview(String choiceId) {
        var client = MinecraftClient.getInstance();
        confirmToken = "";
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand("autoptu battle preview " + choiceId);
        }
    }
}
