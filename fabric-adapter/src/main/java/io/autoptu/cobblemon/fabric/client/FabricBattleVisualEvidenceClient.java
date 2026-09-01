package io.autoptu.cobblemon.fabric.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
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

import java.util.List;

/**
 * QA-only graphical capture for the existing authoritative PlayableBattleTestRuntime.
 *
 * The client never supplies damage, hit, HP or any other battle fact. It starts the server-owned
 * demo battle through the normal command surface, uses a vanilla operator teleport only to place
 * the camera, and records deterministic windows around the server-owned battle cadence. The
 * screenshots themselves expose the server-synchronized PTU HP nameplates for manual validation.
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
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;

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
        if (counterStrikeCaptured || client.player == null || client.world == null) return;

        ticksSinceJoin++;
        if (ticksSinceJoin <= 40 && client.currentScreen != null) client.setScreen(null);
        if (client.getNetworkHandler() == null) return;

        if (!battleRequested && ticksSinceJoin >= 60) {
            client.getNetworkHandler().sendChatCommand("autoptu testbattle charmander");
            battleRequested = true;
            LOGGER.info("AutoPTU battle visual evidence requested authoritative demo battle");
            return;
        }

        if (battleRequested && !cameraPlaced && ticksSinceJoin >= 66) {
            client.getNetworkHandler().sendChatCommand("tp @s ~4 ~2 ~-6 0 10");
            cameraPlaced = true;
            LOGGER.info("AutoPTU battle visual evidence camera placed");
            return;
        }

        List<PokemonEntity> battlePokemon = battlePokemon(client);
        if (cameraPlaced && !readyCaptured && ticksSinceJoin >= 70 && battlePokemon.size() >= 2) {
            capture(client, READY_SCREENSHOT);
            readyCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence observed two server-synchronized battle Pokemon");
            return;
        }

        // PlayableBattleTestRuntime resolves its first move 20 server ticks after construction and
        // subsequent turns every 30 server ticks. Capture later deterministic windows rather than
        // treating Cobblemon native health as PTU truth. The visible custom nameplates remain the
        // server-synchronized authoritative HP projection and are inspected in the artifact.
        if (readyCaptured && !firstStrikeCaptured && ticksSinceJoin >= 100) {
            capture(client, FIRST_STRIKE_SCREENSHOT);
            firstStrikeCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured post-first-move window");
            return;
        }
        if (firstStrikeCaptured && !counterStrikeCaptured && ticksSinceJoin >= 135) {
            capture(client, COUNTER_STRIKE_SCREENSHOT);
            counterStrikeCaptured = true;
            LOGGER.info("AutoPTU battle visual evidence captured post-counter-move window");
        }
    }

    private static List<PokemonEntity> battlePokemon(MinecraftClient client) {
        return client.world.getEntitiesByClass(
                PokemonEntity.class,
                client.player.getBoundingBox().expand(32.0D),
                pokemon -> pokemon.hasCustomName()
                        && pokemon.getCustomName() != null
                        && pokemon.getCustomName().getString().contains(" | HP ")
        );
    }

    private static void capture(MinecraftClient client, String name) {
        if (client.currentScreen != null) client.setScreen(null);
        ScreenshotRecorder.saveScreenshot(
                client.runDirectory,
                name,
                client.getFramebuffer(),
                message -> LOGGER.info("AutoPTU battle visual evidence screenshot result {}: {}", name, message.getString()));
        LOGGER.info("AutoPTU battle visual evidence screenshot requested: {}", name);
    }

    private static void requestEvidenceServerConnection(MinecraftClient client) {
        if (connectRequested || client.currentScreen == null || client.world != null) return;
        if (++ticksBeforeConnect < 40) return;

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
