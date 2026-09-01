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
 * Explicit QA-only graphical evidence hook.
 *
 * Disabled in normal play. When -Dautoptu.visualEvidenceCapture=true is present, the client
 * connects itself to the configured evidence server, waits for a real multiplayer join, asks the
 * authoritative server to build the operator visual proof scene, then uses Minecraft's own
 * screenshot recorder after the world has rendered. It never creates PTU state or reads Cobblemon
 * battle state.
 */
public final class FabricRpgVisualEvidenceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-rpg-visual-evidence");
    private static final String ENABLE_PROPERTY = "autoptu.visualEvidenceCapture";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static final String SCREENSHOT_NAME = "autoptu-rpg-visualproof.png";
    private static final String QA_SCENE_COMMAND = "autoptuvisualproof";

    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean commandSent;
    private static boolean captured;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            return;
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            commandSent = false;
            captured = false;
            LOGGER.info("AutoPTU visual evidence client joined; capture armed");
        });

        ClientTickEvents.END_CLIENT_TICK.register(FabricRpgVisualEvidenceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0) {
            requestEvidenceServerConnection(client);
            return;
        }
        if (captured || client.player == null || client.world == null) {
            return;
        }

        ticksSinceJoin++;
        if (!commandSent && ticksSinceJoin >= 60) {
            if (client.getNetworkHandler() == null) {
                return;
            }
            client.getNetworkHandler().sendChatCommand(QA_SCENE_COMMAND);
            commandSent = true;
            LOGGER.info("AutoPTU visual evidence requested authoritative visual proof scene");
            return;
        }

        if (commandSent && ticksSinceJoin >= 180) {
            ScreenshotRecorder.saveScreenshot(
                    client.runDirectory,
                    SCREENSHOT_NAME,
                    client.getFramebuffer(),
                    message -> LOGGER.info("AutoPTU visual evidence screenshot result: {}", message.getString()));
            captured = true;
            LOGGER.info("AutoPTU visual evidence screenshot requested: {}", SCREENSHOT_NAME);
        }
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
            LOGGER.error("AutoPTU visual evidence server address is invalid: {}", target);
            return;
        }

        ServerInfo info = new ServerInfo("AutoPTU Visual Evidence", target, ServerInfo.ServerType.OTHER);
        info.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
        connectRequested = true;
        LOGGER.info("AutoPTU visual evidence connecting to authoritative server {}", target);
        ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse(target), info, false, null);
    }
}
