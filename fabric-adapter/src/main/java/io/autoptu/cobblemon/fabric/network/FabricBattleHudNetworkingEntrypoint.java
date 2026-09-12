package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.api.ModInitializer;

/** Registers the AutoPTU-owned battle HUD payload before play connections are established. */
public final class FabricBattleHudNetworkingEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricBattleHudNetworking.registerPayloadType();
    }
}
