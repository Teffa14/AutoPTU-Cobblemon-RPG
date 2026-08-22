package io.autoptu.cobblemon.fabric;

import io.autoptu.cobblemon.fabric.battle.CobblemonBattleStartInterceptor;
import io.autoptu.cobblemon.fabric.battle.CobblemonLiveBattleInterceptionSmoke;
import io.autoptu.cobblemon.fabric.battle.FabricAuthenticatedPlayerContextResolverSmoke;
import io.autoptu.cobblemon.fabric.network.FabricBattleActionNetworking;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRestartSmoke;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveHealthSmoke;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveRelocationSmoke;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoPtuCobblemonFabricAdapter implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    @Override
    public void onInitialize() {
        FabricCanonicalPlayerStoreRuntime.register();
        FabricCanonicalPlayerStoreRestartSmoke.registerIfEnabled();
        FabricBattleActionNetworking.register();
        CobblemonBattleStartInterceptor.register();
        CobblemonLiveRelocationSmoke.registerIfEnabled();
        CobblemonLiveHealthSmoke.registerIfEnabled();
        CobblemonLiveBattleInterceptionSmoke.registerIfEnabled();
        FabricAuthenticatedPlayerContextResolverSmoke.registerIfEnabled();
        LOGGER.info("AutoPTU Fabric server adapter initialized");
        try {
            Class<?> cobblemon = Class.forName("com.cobblemon.mod.common.Cobblemon");
            LOGGER.info("AutoPTU Cobblemon runtime detected: {}", cobblemon.getName());
        } catch (ClassNotFoundException missingCobblemon) {
            LOGGER.warn("AutoPTU Cobblemon runtime not detected");
        }
    }
}
