package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.battlecore.BattleRuntimeMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Native mode registers no duplicate battle keys, starter screens, HUD or grid renderers. */
public final class FabricGameplayClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (BattleRuntimeMode.configured() != BattleRuntimeMode.PTU_EXPERIMENTAL) return;
        for (ClientModInitializer entrypoint : FabricLoader.getInstance().getEntrypoints("autoptu:experimental_client", ClientModInitializer.class)) {
            entrypoint.onInitializeClient();
        }
    }
}
