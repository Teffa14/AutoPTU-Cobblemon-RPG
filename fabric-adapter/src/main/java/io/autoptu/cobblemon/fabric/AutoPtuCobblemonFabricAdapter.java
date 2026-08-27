package io.autoptu.cobblemon.fabric;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.battle.CobblemonLiveBattleInterceptionSmoke;
import io.autoptu.cobblemon.fabric.battle.FabricAuthenticatedPlayerContextResolverSmoke;
import io.autoptu.cobblemon.fabric.demo.PlayableBattleTestRuntime;
import io.autoptu.cobblemon.fabric.network.FabricBattleActionNetworking;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRestartSmoke;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveHealthSmoke;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveReactionPlaybackSmoke;
import io.autoptu.cobblemon.fabric.presentation.CobblemonLiveRelocationSmoke;
import io.autoptu.cobblemon.fabric.rpg.FabricPartyHealingRuntime;
import io.autoptu.cobblemon.fabric.rpg.FabricStarterCatalogueRuntime;
import io.autoptu.cobblemon.fabric.world.CedarMeadowRuntime;
import io.autoptu.cobblemon.fabric.world.OurosBuildManifestExportRuntime;
import io.autoptu.cobblemon.fabric.world.OurosGrandPalaceManifestExportRuntime;
import io.autoptu.cobblemon.fabric.world.OurosSignatureBuildRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server Fabric entrypoint for the integration adapter. */
public final class AutoPtuCobblemonFabricAdapter implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    @Override
    public void onInitialize() {
        FabricCanonicalPlayerStoreRuntime.register();
        FabricCanonicalPlayerProvisioning.register();
        FabricCanonicalPlayerStoreRestartSmoke.registerIfEnabled();
        FabricBattleActionNetworking.registerPayloadType();
        if (!FabricLoader.getInstance().isModLoaded("cobblemon")) {
            throw new IllegalStateException("Cobblemon runtime is required by the AutoPTU adapter");
        }
        PlayableBattleTestRuntime.register();
        FabricPartyHealingRuntime.register();
        FabricStarterCatalogueRuntime.register();
        CedarMeadowRuntime.register();
        OurosSignatureBuildRuntime.register();
        OurosBuildManifestExportRuntime.registerIfEnabled();
        OurosGrandPalaceManifestExportRuntime.registerIfEnabled();
        CobblemonLiveRelocationSmoke.registerIfEnabled();
        CobblemonLiveReactionPlaybackSmoke.registerIfEnabled();
        CobblemonLiveHealthSmoke.registerIfEnabled();
        CobblemonLiveBattleInterceptionSmoke.registerIfEnabled();
        FabricAuthenticatedPlayerContextResolverSmoke.registerIfEnabled();
        LOGGER.info("AutoPTU Cobblemon runtime detected: {}", PokemonEntity.class.getName());
        LOGGER.info("AutoPTU Fabric server adapter initialized");
    }
}
