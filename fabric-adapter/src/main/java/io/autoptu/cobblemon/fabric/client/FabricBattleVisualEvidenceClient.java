package io.autoptu.cobblemon.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.network.message.ChatVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** QA-only production-client capture for the server-owned playable AutoPTU battle. */
public final class FabricBattleVisualEvidenceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-battle-visual-evidence");
    private static final String ENABLE_PROPERTY = "autoptu.battleVisualEvidenceCapture";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static final int[] FLAMETHROWER_CAPTURE_TICKS = {82, 90, 98, 106, 114, 122, 130, 138};
    private static final int[] HYDRO_PUMP_CAPTURE_TICKS = {162, 170, 178, 186, 194, 202, 210, 218};
    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean battleRequested;
    private static boolean cameraPlaced;
    private static boolean readyCaptured;
    private static int flamethrowerCaptures;
    private static int hydroPumpCaptures;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            battleRequested = cameraPlaced = readyCaptured = false;
            flamethrowerCaptures = hydroPumpCaptures = 0;
            LOGGER.info("AutoPTU battle visual evidence client joined; capture armed");
        });
        ClientTickEvents.END_CLIENT_TICK.register(FabricBattleVisualEvidenceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0) { requestConnection(client); return; }
        if (hydroPumpCaptures >= HYDRO_PUMP_CAPTURE_TICKS.length || client.player == null || client.world == null) return;
        ticksSinceJoin++;
        if (ticksSinceJoin <= 40 && client.currentScreen != null) client.setScreen(null);
        if (client.getNetworkHandler() == null) return;

        if (!battleRequested && ticksSinceJoin >= 60) {
            client.getNetworkHandler().sendChatCommand("autoptu testbattle charizard");
            battleRequested = true;
            LOGGER.info("AutoPTU battle visual evidence requested Charizard vs Blastoise native move showcase");
            return;
        }

        if (battleRequested && !cameraPlaced && ticksSinceJoin >= 66) {
            client.getNetworkHandler().sendChatCommand("tp @s ~4 ~2 ~-6 0 10");
            cameraPlaced = true;
            LOGGER.info("AutoPTU battle visual evidence camera placed");
            return;
        }

        if (cameraPlaced) sanitizeCaptureHud(client);

        if (cameraPlaced && !readyCaptured && ticksSinceJoin >= 70) {
            capture(client, "autoptu-charizard-blastoise-ready.png");
            readyCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured Charizard vs Blastoise ready window");
            return;
        }

        if (readyCaptured && flamethrowerCaptures < FLAMETHROWER_CAPTURE_TICKS.length
                && ticksSinceJoin >= FLAMETHROWER_CAPTURE_TICKS[flamethrowerCaptures]) {
            int frame = ++flamethrowerCaptures;
            capture(client, String.format("autoptu-charizard-blastoise-flamethrower-%02d.png", frame));
            LOGGER.info("AutoPTU battle visual evidence captured Flamethrower frame {} at client tick {}", frame, ticksSinceJoin);
            return;
        }

        if (flamethrowerCaptures >= FLAMETHROWER_CAPTURE_TICKS.length
                && hydroPumpCaptures < HYDRO_PUMP_CAPTURE_TICKS.length
                && ticksSinceJoin >= HYDRO_PUMP_CAPTURE_TICKS[hydroPumpCaptures]) {
            int frame = ++hydroPumpCaptures;
            capture(client, String.format("autoptu-charizard-blastoise-hydropump-%02d.png", frame));
            LOGGER.info("AutoPTU battle visual evidence captured Hydro Pump frame {} at client tick {}", frame, ticksSinceJoin);
        }
    }

    private static void sanitizeCaptureHud(MinecraftClient client) {
        client.options.getChatVisibility().setValue(ChatVisibility.HIDDEN);
        client.inGameHud.getChatHud().clear(false);
        client.getToastManager().clear();
    }

    private static void capture(MinecraftClient client, String name) {
        if (client.currentScreen != null) client.setScreen(null);
        sanitizeCaptureHud(client);
        ScreenshotRecorder.saveScreenshot(client.runDirectory, name, client.getFramebuffer(),
                message -> LOGGER.info("AutoPTU battle visual evidence screenshot result {}: {}", name, message.getString()));
        LOGGER.info("AutoPTU battle visual evidence screenshot requested: {}", name);
    }

    private static void requestConnection(MinecraftClient client) {
        if (connectRequested || client.currentScreen == null || client.world != null) return;
        if (++ticksBeforeConnect < 40) return;
        String target = System.getProperty(SERVER_PROPERTY, DEFAULT_SERVER).trim();
        if (!ServerAddress.isValid(target)) { connectRequested = true; LOGGER.error("Invalid evidence server {}", target); return; }
        ServerInfo info = new ServerInfo("AutoPTU Battle Visual Evidence", target, ServerInfo.ServerType.OTHER);
        info.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
        connectRequested = true;
        LOGGER.info("AutoPTU battle visual evidence connecting to authoritative server {}", target);
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(target), info, false, null);
    }
}
