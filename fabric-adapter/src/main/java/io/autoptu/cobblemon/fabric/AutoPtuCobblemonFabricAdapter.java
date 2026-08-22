package io.autoptu.cobblemon.fabric;

import io.autoptu.cobblemon.fabric.network.FabricBattleActionNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server Fabric entrypoint for the integration adapter.
 *
 * Startup only registers the C2S payload codec. Runtime battle services are wired separately so
 * Fabric startup cannot invent battle state or PTU behavior.
 */
public final class AutoPtuCobblemonFabricAdapter implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    @Override
    public void onInitialize() {
        FabricBattleActionNetworking.registerPayloadType();
        LOGGER.info("AutoPTU Fabric server adapter initialized");
    }
}
