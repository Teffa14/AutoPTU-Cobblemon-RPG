package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleCameraMode;
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
    private static final String CAMERA_MODE_EVIDENCE_PROPERTY = "autoptu.crafticsCameraModeEvidence";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static final int MODE_STABLE_TICKS_BEFORE_CAPTURE = 3;
    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean battleRequested;
    private static boolean cameraReady;
    private static boolean trainerExternalCaptured;
    private static boolean tacticalAerialCaptured;
    private static boolean actionCinematicCaptured;
    private static FabricBattleCameraMode observedMode;
    private static int observedModeStableTicks;
    private static boolean readyCaptured;
    private static boolean firstCaptured;
    private static boolean counterCaptured;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            battleRequested = cameraReady = false;
            trainerExternalCaptured = tacticalAerialCaptured = actionCinematicCaptured = false;
            observedMode = null;
            observedModeStableTicks = 0;
            readyCaptured = firstCaptured = counterCaptured = false;
            LOGGER.info("AutoPTU battle visual evidence client joined; capture armed");
        });
        ClientTickEvents.END_CLIENT_TICK.register(FabricBattleVisualEvidenceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0) { requestConnection(client); return; }
        if (counterCaptured || client.player == null || client.world == null) return;
        ticksSinceJoin++;
        if (ticksSinceJoin <= 40 && client.currentScreen != null) client.setScreen(null);
        if (client.getNetworkHandler() == null) return;

        if (!battleRequested && ticksSinceJoin >= 60) {
            client.getNetworkHandler().sendChatCommand("autoptu testbattle charmander");
            battleRequested = true;
            LOGGER.info("AutoPTU battle visual evidence requested authoritative demo battle");
            return;
        }

        if (battleRequested && !cameraReady && ticksSinceJoin >= 66) {
            // The detached camera no longer requires moving the trainer to fake a viewing position.
            // Keep the trainer at the exact position used by the server-authored external frame.
            cameraReady = true;
            LOGGER.info("AutoPTU battle visual evidence detached camera staging ready");
            return;
        }

        if (cameraReady) sanitizeCaptureHud(client);

        if (cameraReady && Boolean.getBoolean(CAMERA_MODE_EVIDENCE_PROPERTY) && captureStableCameraMode(client)) {
            return;
        }

        // AutoPTU-Java owns the fixed demo cadence and PTU results. These windows only capture its
        // server-synchronized presentation and never use Cobblemon-native HP as combat authority.
        if (cameraReady && !readyCaptured && ticksSinceJoin >= 70) {
            capture(client, "autoptu-battle-ready.png"); readyCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured ready window"); return;
        }
        if (readyCaptured && !firstCaptured && ticksSinceJoin >= 90) {
            capture(client, "autoptu-battle-first-strike.png"); firstCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured post-first-move window"); return;
        }
        if (firstCaptured && !counterCaptured && ticksSinceJoin >= 120) {
            capture(client, "autoptu-battle-counter-strike.png"); counterCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured post-counter-move window");
        }
    }

    private static boolean captureStableCameraMode(MinecraftClient client) {
        FabricDetachedBattleCameraState.Snapshot snapshot = FabricDetachedBattleCameraState.snapshot();
        if (snapshot == null) {
            observedMode = null;
            observedModeStableTicks = 0;
            return false;
        }

        if (snapshot.mode() != observedMode) {
            observedMode = snapshot.mode();
            observedModeStableTicks = 1;
            return false;
        }
        observedModeStableTicks++;
        if (observedModeStableTicks < MODE_STABLE_TICKS_BEFORE_CAPTURE) return false;

        if (snapshot.mode() == FabricBattleCameraMode.TRAINER_EXTERNAL && !trainerExternalCaptured) {
            capture(client, "autoptu-camera-trainer-external.png");
            trainerExternalCaptured = true;
            LOGGER.info("AutoPTU camera evidence captured trainer-external mode after stable render window");
            return true;
        }
        if (snapshot.mode() == FabricBattleCameraMode.TACTICAL_AERIAL && trainerExternalCaptured && !tacticalAerialCaptured) {
            capture(client, "autoptu-camera-tactical-aerial.png");
            tacticalAerialCaptured = true;
            LOGGER.info("AutoPTU camera evidence captured tactical-aerial mode after stable render window");
            return true;
        }
        if (snapshot.mode() == FabricBattleCameraMode.ACTION_CINEMATIC && tacticalAerialCaptured && !actionCinematicCaptured) {
            capture(client, "autoptu-camera-action-cinematic.png");
            actionCinematicCaptured = true;
            LOGGER.info("AutoPTU camera evidence captured action-cinematic mode after stable render window");
            return true;
        }
        return false;
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
