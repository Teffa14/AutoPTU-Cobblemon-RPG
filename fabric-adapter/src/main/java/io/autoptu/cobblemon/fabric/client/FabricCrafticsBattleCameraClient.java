package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleCameraNetworking;
import io.autoptu.cobblemon.fabric.network.FabricBattleCameraPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Detached AutoPTU battle camera with optional Craftics scene-bound synchronization.
 *
 * <p>AutoPTU owns the camera mode and physical focus point. The client switches to third-person so
 * the trainer body can remain visible and the first-person hand is not rendered, then the camera
 * mixin places the view independently from the player's eyes. Craftics is used only as an optional
 * presentation companion for matching scene bounds; its combat, AP, movement, targeting, damage,
 * turn and result state are never activated.
 */
public final class FabricCrafticsBattleCameraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-detached-battle-camera");
    private static final CrafticsBridge CRAFTICS = CrafticsBridge.discover();
    private static Perspective previousPerspective;
    private static boolean perspectiveCaptured;

    @Override
    public void onInitializeClient() {
        FabricBattleCameraNetworking.registerPayloadType();
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleCameraPayload.ID, (payload, context) ->
                context.client().execute(() -> apply(context.client(), payload)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> FabricDetachedBattleCameraState.tick());
    }

    private static void apply(MinecraftClient client, FabricBattleCameraPayload payload) {
        try {
            if (!payload.active()) {
                FabricDetachedBattleCameraState.clear();
                restorePerspective(client);
                CRAFTICS.clearBounds();
                LOGGER.info("AutoPTU detached battle camera cleared");
                return;
            }

            capturePerspective(client);
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            FabricDetachedBattleCameraState.apply(payload);
            CRAFTICS.syncBounds(payload);
            LOGGER.info(
                    "AutoPTU detached battle camera mode {} active for {} at focus [{},{},{}]",
                    payload.mode(),
                    payload.battleId(),
                    payload.focusX(),
                    payload.focusY(),
                    payload.focusZ()
            );
        } catch (ReflectiveOperationException exception) {
            FabricDetachedBattleCameraState.clear();
            restorePerspective(client);
            LOGGER.error("Craftics scene-bound synchronization failed closed; AutoPTU battle state was not changed", exception);
        }
    }

    private static void capturePerspective(MinecraftClient client) {
        if (perspectiveCaptured) return;
        previousPerspective = client.options.getPerspective();
        perspectiveCaptured = true;
    }

    private static void restorePerspective(MinecraftClient client) {
        if (!perspectiveCaptured) return;
        client.options.setPerspective(previousPerspective == null ? Perspective.FIRST_PERSON : previousPerspective);
        previousPerspective = null;
        perspectiveCaptured = false;
    }

    private record CrafticsBridge(boolean available, Method setSceneBounds, Method clearSceneBounds) {
        static CrafticsBridge discover() {
            if (!FabricLoader.getInstance().isModLoaded("craftics")) {
                LOGGER.info("Craftics is not installed; detached AutoPTU camera remains available");
                return unavailable();
            }
            try {
                Class<?> state = Class.forName("com.crackedgames.craftics.client.CombatState");
                CrafticsBridge bridge = new CrafticsBridge(
                        true,
                        state.getMethod("setSceneBounds", int.class, int.class, int.class, int.class, int.class),
                        state.getMethod("clearSceneBounds")
                );
                LOGGER.info("Craftics presentation bounds bridge available");
                return bridge;
            } catch (ReflectiveOperationException exception) {
                LOGGER.warn("Installed Craftics does not expose the pinned scene-bound surface; continuing without it", exception);
                return unavailable();
            }
        }

        private static CrafticsBridge unavailable() {
            return new CrafticsBridge(false, null, null);
        }

        void syncBounds(FabricBattleCameraPayload payload) throws ReflectiveOperationException {
            if (!available) return;
            setSceneBounds.invoke(
                    null,
                    payload.originX(),
                    payload.originY(),
                    payload.originZ(),
                    payload.width(),
                    payload.height()
            );
        }

        void clearBounds() throws ReflectiveOperationException {
            if (!available) return;
            clearSceneBounds.invoke(null);
        }
    }
}
