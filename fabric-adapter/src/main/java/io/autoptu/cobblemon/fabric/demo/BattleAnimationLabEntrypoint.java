package io.autoptu.cobblemon.fabric.demo;

import net.fabricmc.api.ModInitializer;

/** Registers the operator-only visual battle animation, shape and HUD laboratories. */
public final class BattleAnimationLabEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        BattleAnimationLabRuntime.register();
        BattleShapeVisualLabRuntime.register();
        BattleHudLabRuntime.register();
    }
}
