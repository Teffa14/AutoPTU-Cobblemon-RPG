package io.autoptu.cobblemon.fabric;

import io.autoptu.cobblemon.battlecore.BattleRuntimeMode;
import io.autoptu.cobblemon.fabric.battle.NativeCobblemonBattleRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** Native gameplay is the default. Experimental stores remain untouched, not silently migrated. */
public final class FabricGameplayEntrypoint implements ModInitializer {
    @Override public void onInitialize() {
        var mode = BattleRuntimeMode.configured();
        io.autoptu.cobblemon.fabric.ptu.PtuPokemonDataRuntime.register();
        LoggerFactory.getLogger("autoptu-cobblemon-rpg").info("AutoPTU gameplay owner: {}", mode);
        if (mode == BattleRuntimeMode.COBBLEMON) {
            // Keep registry IDs available when opening an existing save. Only their experimental
            // interaction handlers are disabled; removing block IDs would damage authored worlds.
            io.autoptu.cobblemon.fabric.rpg.FabricRpgContent.register();
            NativeCobblemonBattleRuntime.register();
            io.autoptu.cobblemon.fabric.battle.NativeCobblemonDuelRuntime.register();
        } else {
            for (ModInitializer entrypoint : FabricLoader.getInstance().getEntrypoints("autoptu:experimental_main", ModInitializer.class)) {
                entrypoint.onInitialize();
            }
        }
    }
}
