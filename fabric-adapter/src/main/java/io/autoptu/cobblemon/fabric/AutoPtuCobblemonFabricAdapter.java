package io.autoptu.cobblemon.fabric;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.network.FabricBattleActionNetworking;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveRelocationSmoke;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server Fabric entrypoint for the integration adapter.
 *
 * Startup registers transport only and verifies that the Cobblemon runtime needed by the
 * presentation adapter is actually present. Runtime battle services are wired separately so
 * Fabric/Cobblemon startup cannot invent battle state or PTU behavior.
 */
public final class AutoPtuCobblemonFabricAdapter implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    @Override
    public void onInitialize() {
        FabricBattleActionNetworking.registerPayloadType();
        if (!FabricLoader.getInstance().isModLoaded("cobblemon")) {
            throw new IllegalStateException("Cobblemon runtime is required by the AutoPTU adapter");
        }
        CobblemonLiveRelocationSmoke.registerIfRequested();
        LOGGER.info("AutoPTU Cobblemon runtime detected: {}", PokemonEntity.class.getName());
        LOGGER.info("AutoPTU Fabric server adapter initialized");
    }
}
