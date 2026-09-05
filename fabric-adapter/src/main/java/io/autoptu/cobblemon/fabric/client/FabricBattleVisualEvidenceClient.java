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
    private static final int[] OFFSETS = {2, 6, 10, 14, 18, 22, 26, 30, 34};
    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean shapeSceneStarted;
    private static boolean cameraPlaced;
    private static int phase;
    private static int phaseStart = -1;
    private static int captureIndex;
    private static boolean done;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            shapeSceneStarted = cameraPlaced = done = false;
            phase = 0;
            phaseStart = -1;
            captureIndex = 0;
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
            // QA evidence only: use a higher, steeper angle so ground footprints are readable.
            // This does not alter production battle-camera behavior.
            client.getNetworkHandler().sendChatCommand("tp @s ~5 ~7 ~-10 0 28");
            cameraPlaced = true;
            LOGGER.info("AutoPTU PTU-shape evidence camera placed at elevated footprint-readable angle");
            return;
        }
        if (cameraPlaced) sanitizeCaptureHud(client);
        if (!cameraPlaced || ticksSinceJoin < 80) return;

        if (phaseStart < 0) {
            startPhase(client);
            return;
        }

        int elapsed = ticksSinceJoin - phaseStart;
        if (captureIndex < OFFSETS.length && elapsed >= OFFSETS[captureIndex]) {
            String name = phaseName();
            captureIndex++;
            capture(client, String.format("autoptu-%s-%02d.png", name, captureIndex));
            LOGGER.info("AutoPTU shape evidence captured {} frame {} at +{} ticks", name, captureIndex, elapsed);
            return;
        }

        if (captureIndex >= OFFSETS.length && elapsed >= 48) {
            LOGGER.info("AutoPTU shape evidence completed {} window", phaseName());
            phase++;
            if (phase >= 4) {
                done = true;
                return;
            }
            client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz reset");
            phaseStart = -1;
            captureIndex = 0;
        }
    }

    private static void startPhase(MinecraftClient client) {
        String name = phaseName();
        client.getNetworkHandler().sendChatCommand("autoptu admin shapeviz " + name);
        phaseStart = ticksSinceJoin;
        captureIndex = 0;
        LOGGER.info("AutoPTU shape evidence started {} window at client tick {}", name, ticksSinceJoin);
    }

    private static String phaseName() {
        return switch (phase) {
            case 0 -> "ranged";
            case 1 -> "aoe";
            case 2 -> "blast";
            case 3 -> "line";
            default -> throw new IllegalStateException("invalid shape evidence phase " + phase);
        };
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
