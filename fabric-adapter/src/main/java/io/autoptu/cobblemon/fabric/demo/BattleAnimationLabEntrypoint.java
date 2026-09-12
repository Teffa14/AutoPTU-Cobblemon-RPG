package io.autoptu.cobblemon.fabric.demo;

import net.fabricmc.api.ModInitializer;

/** Registers the operator-only visual battle animation and HUD laboratories. */
public final class BattleAnimationLabEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        BattleAnimationLabRuntime.register();
        BattleHudLabRuntime.register();
    }
}
