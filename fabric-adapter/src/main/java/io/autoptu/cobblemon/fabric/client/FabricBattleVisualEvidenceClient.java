package io.autoptu.cobblemon.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QA-only graphical capture for the existing authoritative PlayableBattleTestRuntime.
 *
 * The client never supplies damage, hit, HP or any other battle fact. It starts the server-owned
 * demo battle through the normal command surface, uses a vanilla operator teleport only to place
 * the camera, and records frames after AutoPTU-Java has advanced the battle.
 */
public final class FabricBattleVisualEvidenceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-battle-visual-evidence");
    private static final String ENABLE_PROPERTY = "autoptu.battleVisualEvidenceCapture";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static final String READY_SCREENSHOT = "autoptu-battle-ready.png";
    private static final String FIRST_STRIKE_SCREENSHOT = "autoptu-battle-first-strike.png";
    private static final String COUNTER_STRIKE_SCREENSHOT = "autoptu-battle-counter-strike.png";

    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean battleRequested;
    private static boolean cameraPlaced;
    private static boolean readyCaptured;
    private static boolean firstStrikeCaptured;
    private static boolean counterStrikeCaptured;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            return;
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            battleRequested = false;
            cameraPlaced = false;
            readyCaptured = false;
            firstStrikeCaptured = false;
            counterStrikeCaptured = false;
            LOGGER.info("AutoPTU battle visual evidence client joined; capture armed");
        });
        ClientTickEvents.END_CLIENT_TICK.register(FabricBattleVisualEvidenceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0) {
            requestEvidenceServerConnection(client);
            return;
        }
        if (counterStrikeCaptured || client.player == null || client.world == null) {
            return;
        }

        ticksSinceJoin++;
        if (ticksSinceJoin <= 40 && client.currentScreen != null) {
            client.setScreen(null);
        }
        if (client.getNetworkHandler() == null) {
            return;
        }

        if (!battleRequested && ticksSinceJoin >= 60) {
            client.getNetworkHandler().sendChatCommand("autoptu testbattle charmander");
            battleRequested = true;
            LOGGER.info("AutoPTU battle visual evidence requested authoritative demo battle");
            return;
        }

        if (battleRequested && !cameraPlaced && ticksSinceJoin >= 66) {
            // Vanilla camera placement only. The combatants were already spawned by the server-owned
            // battle runtime; this command does not move or mutate either combatant.
            client.getNetworkHandler().sendChatCommand("tp @s ~4 ~2 ~-6 0 10");
            cameraPlaced = true;
            LOGGER.info("AutoPTU battle visual evidence camera placed");
            return;
        }

        if (cameraPlaced && !readyCaptured && ticksSinceJoin >= 72) {
            capture(client, READY_SCREENSHOT);
            readyCaptured = true;
            return;
        }
        if (readyCaptured && !firstStrikeCaptured && ticksSinceJoin >= 84) {
            capture(client, FIRST_STRIKE_SCREENSHOT);
            firstStrikeCaptured = true;
            return;
        }
        if (firstStrikeCaptured && !counterStrikeCaptured && ticksSinceJoin >= 114) {
            capture(client, COUNTER_STRIKE_SCREENSHOT);
            counterStrikeCaptured = true;
        }
    }

    private static void capture(MinecraftClient client, String name) {
        if (client.currentScreen != null) {
            client.setScreen(null);
        }
        ScreenshotRecorder.saveScreenshot(
                client.runDirectory,
                name,
                client.getFramebuffer(),
                message -> LOGGER.info("AutoPTU battle visual evidence screenshot result {}: {}", name, message.getString()));
        LOGGER.info("AutoPTU battle visual evidence screenshot requested: {}", name);
    }

    private static void requestEvidenceServerConnection(MinecraftClient client) {
        if (connectRequested || client.currentScreen == null || client.world != null) {
            return;
        }
        if (++ticksBeforeConnect < 40) {
            return;
        }

        String target = System.getProperty(SERVER_PROPERTY, DEFAULT_SERVER).trim();
        if (!ServerAddress.isValid(target)) {
            connectRequested = true;
            LOGGER.error("AutoPTU battle visual evidence server address is invalid: {}", target);
            return;
        }

        ServerInfo info = new ServerInfo("AutoPTU Battle Visual Evidence", target, ServerInfo.ServerType.OTHER);
        info.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
        connectRequested = true;
        LOGGER.info("AutoPTU battle visual evidence connecting to authoritative server {}", target);
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(target), info, false, null);
    }
}
