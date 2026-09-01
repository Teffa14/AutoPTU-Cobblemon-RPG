package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleCameraNetworking;
import io.autoptu.cobblemon.fabric.network.FabricBattleCameraPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Optional Craftics presentation adapter for server-authored AutoPTU battle camera frames.
 *
 * <p>The bridge deliberately does not call Craftics enterCombat, setInCombat, selection, movement,
 * AP, targeting, damage or turn APIs. It only activates Craftics' cinematic isometric camera over
 * physical bounds supplied by the AutoPTU server, preserving AutoPTU as the sole battle authority.
 */
public final class FabricCrafticsBattleCameraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-craftics-camera");
    private static final CrafticsBridge CRAFTICS = CrafticsBridge.discover();

    @Override
    public void onInitializeClient() {
        FabricBattleCameraNetworking.registerPayloadType();
        ClientPlayNetworking.registerGlobalReceiver(FabricBattleCameraPayload.ID, (payload, context) ->
                context.client().execute(() -> apply(payload)));
    }

    private static void apply(FabricBattleCameraPayload payload) {
        if (!CRAFTICS.available()) return;
        try {
            if (!payload.active()) {
                CRAFTICS.clear();
                LOGGER.info("AutoPTU Craftics tactical camera cleared");
                return;
            }
            CRAFTICS.activate(
                    payload.originX(), payload.originY(), payload.originZ(), payload.width(), payload.height());
            LOGGER.info(
                    "AutoPTU Craftics tactical camera active for {} at [{},{},{}] {}x{}",
                    payload.battleId(), payload.originX(), payload.originY(), payload.originZ(),
                    payload.width(), payload.height());
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Craftics camera adapter failed closed; AutoPTU battle state was not changed", exception);
        }
    }

    private record CrafticsBridge(
            boolean available,
            Method setSceneBounds,
            Method clearSceneBounds,
            Method setCinematicActive,
            Method seedCinematicFocusOnPlayer,
            Method focusOn,
            Method setCombatYaw,
            Method setCombatPitch
    ) {
        static CrafticsBridge discover() {
            if (!FabricLoader.getInstance().isModLoaded("craftics")) {
                LOGGER.info("Craftics is not installed; AutoPTU keeps the vanilla server-owned camera fallback");
                return unavailable();
            }
            try {
                Class<?> state = Class.forName("com.crackedgames.craftics.client.CombatState");
                CrafticsBridge bridge = new CrafticsBridge(
                        true,
                        state.getMethod("setSceneBounds", int.class, int.class, int.class, int.class, int.class),
                        state.getMethod("clearSceneBounds"),
                        state.getMethod("setCinematicActive", boolean.class),
                        state.getMethod("seedCinematicFocusOnPlayer"),
                        state.getMethod("focusOn", double.class, double.class),
                        state.getMethod("setCombatYaw", float.class),
                        state.getMethod("setCombatPitch", float.class));
                LOGGER.info("Craftics camera presentation bridge available");
                return bridge;
            } catch (ReflectiveOperationException exception) {
                LOGGER.warn("Installed Craftics does not expose the pinned camera surface; adapter disabled", exception);
                return unavailable();
            }
        }

        private static CrafticsBridge unavailable() {
            return new CrafticsBridge(false, null, null, null, null, null, null, null);
        }

        void activate(int originX, int originY, int originZ, int width, int height)
                throws ReflectiveOperationException {
            setSceneBounds.invoke(null, originX, originY, originZ, width, height);
            setCombatYaw.invoke(null, 225.0F);
            setCombatPitch.invoke(null, 55.0F);
            seedCinematicFocusOnPlayer.invoke(null);
            setCinematicActive.invoke(null, true);
            focusOn.invoke(null, originX + width / 2.0D, originZ + height / 2.0D);
        }

        void clear() throws ReflectiveOperationException {
            setCinematicActive.invoke(null, false);
            clearSceneBounds.invoke(null);
        }
    }
}
