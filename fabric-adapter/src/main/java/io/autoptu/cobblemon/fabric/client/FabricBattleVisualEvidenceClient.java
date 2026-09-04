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

/** QA-only production-client capture for PTU-relevant attack-shape presentation. */
public final class FabricBattleVisualEvidenceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-battle-visual-evidence");
    private static final String ENABLE_PROPERTY = "autoptu.battleVisualEvidenceCapture";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean shapeSceneStarted;
    private static boolean cameraPlaced;
    private static boolean rangedPlayed;
    private static boolean aoePlayed;
    private static boolean blastPlayed;
    private static boolean linePlayed;
    private static boolean done;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            shapeSceneStarted = cameraPlaced = rangedPlayed = aoePlayed = blastPlayed = linePlayed = done = false;
            LOGGER.info("AutoPTU battle visual evidence client joined; PTU shape capture armed");
        });
        ClientTickEvents.END_CLIENT_TICK.register(FabricBattleVisualEvidenceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0) { requestConnection(client); return; }
        if (done || client.player == null || client.world == null) return;
        ticksSinceJoin++;
        if (ticksSinceJoin <= 40 && client.currentScreen != null) client.setScreen(null);
        if (client.getNetworkHandler() == null) return;

        if (!shapeSceneStarted && ticksSinceJoin >= 60) {
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz start");
            shapeSceneStarted = true;
            LOGGER.info("AutoPTU PTU-shape scene requested");
            return;
        }
        if (shapeSceneStarted && !cameraPlaced && ticksSinceJoin >= 66) {
            client.getNetworkHandler().sendChatCommand("tp @s ~5 ~4 ~-10 0 12");
            cameraPlaced = true;
            LOGGER.info("AutoPTU PTU-shape camera placed");
            return;
        }
        if (cameraPlaced) sanitizeCaptureHud(client);

        if (!rangedPlayed && ticksSinceJoin >= 80) {
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz ranged");
            rangedPlayed = true;
            return;
        }
        if (rangedPlayed && ticksSinceJoin >= 83 && ticksSinceJoin <= 91) {
            capture(client, String.format("autoptu-ranged-%02d.png", ticksSinceJoin - 82));
            if (ticksSinceJoin == 91) LOGGER.info("AutoPTU shape evidence captured ranged frames");
            return;
        }
        if (!aoePlayed && ticksSinceJoin >= 110) {
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz reset");
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz aoe");
            aoePlayed = true;
            return;
        }
        if (aoePlayed && ticksSinceJoin >= 113 && ticksSinceJoin <= 121) {
            capture(client, String.format("autoptu-aoe-%02d.png", ticksSinceJoin - 112));
            if (ticksSinceJoin == 121) LOGGER.info("AutoPTU shape evidence captured AoE frames");
            return;
        }
        if (!blastPlayed && ticksSinceJoin >= 140) {
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz reset");
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz blast");
            blastPlayed = true;
            return;
        }
        if (blastPlayed && ticksSinceJoin >= 143 && ticksSinceJoin <= 151) {
            capture(client, String.format("autoptu-blast-%02d.png", ticksSinceJoin - 142));
            if (ticksSinceJoin == 151) LOGGER.info("AutoPTU shape evidence captured Blast frames");
            return;
        }
        if (!linePlayed && ticksSinceJoin >= 170) {
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz reset");
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz line");
            linePlayed = true;
            return;
        }
        if (linePlayed && ticksSinceJoin >= 173 && ticksSinceJoin <= 181) {
            capture(client, String.format("autoptu-line-%02d.png", ticksSinceJoin - 172));
            if (ticksSinceJoin == 181) {
                LOGGER.info("AutoPTU shape evidence captured Line frames");
                done = true;
            }
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
    }

    private static void requestConnection(MinecraftClient client) {
        if (connectRequested || client.currentScreen == null || client.world != null) return;
        if (++ticksBeforeConnect < 40) return;
        String target = System.getProperty(SERVER_PROPERTY, DEFAULT_SERVER).trim();
        if (!ServerAddress.isValid(target)) { connectRequested = true; LOGGER.error("Invalid evidence server {}", target); return; }
        ServerInfo info = new ServerInfo("AutoPTU Battle Visual Evidence", target, ServerInfo.ServerType.OTHER);
        info.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
        connectRequested = true;
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(target), info, false, null);
    }
}
