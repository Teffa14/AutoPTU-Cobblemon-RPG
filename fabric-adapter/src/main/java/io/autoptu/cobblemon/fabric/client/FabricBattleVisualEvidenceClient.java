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
import net.minecraft.network.message.ChatVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** QA-only production-client capture for the server-owned playable AutoPTU battle. */
public final class FabricBattleVisualEvidenceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-battle-visual-evidence");
    private static final String ENABLE_PROPERTY = "autoptu.battleVisualEvidenceCapture";
    private static final String SERVER_PROPERTY = "autoptu.visualEvidenceServer";
    private static final String DEFAULT_SERVER = "127.0.0.1:25565";
    private static int ticksBeforeConnect;
    private static int ticksSinceJoin = -1;
    private static boolean connectRequested;
    private static boolean battleRequested;
    private static boolean cameraPlaced;
    private static boolean readyCaptured;
    private static boolean firstCaptured;
    private static boolean counterCaptured;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticksSinceJoin = 0;
            battleRequested = cameraPlaced = readyCaptured = firstCaptured = counterCaptured = false;
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

        if (battleRequested && !cameraPlaced && battlePokemon(client).size() >= 2) {
            // Camera-only command. The server-owned battle has already materialized both combatants.
            client.getNetworkHandler().sendChatCommand("tp @s ~4 ~2 ~-6 0 10");
            cameraPlaced = true;
            LOGGER.info("AutoPTU battle visual evidence observed two server-synchronized battle Pokemon");
            LOGGER.info("AutoPTU battle visual evidence camera placed");
            return;
        }

        // Hide and clear non-battle UI only after both server commands have been sent. Hiding chat
        // earlier prevents Minecraft from sending slash commands at all.
        if (cameraPlaced) sanitizeCaptureHud(client);

        // Do not use Cobblemon-native HP as battle authority. AutoPTU-Java owns the fixed demo
        // cadence and PTU results; these windows only capture its server-synchronized presentation.
        if (cameraPlaced && !readyCaptured && ticksSinceJoin >= 70) {
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

    private static List<PokemonEntity> battlePokemon(MinecraftClient client) {
        return client.world.getEntitiesByClass(
                PokemonEntity.class,
                client.player.getBoundingBox().expand(32.0D),
                pokemon -> pokemon.hasCustomName()
                        && pokemon.getCustomName() != null
                        && pokemon.getCustomName().getString().contains(" | HP ")
        );
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
