package io.autoptu.cobblemon.fabric.demo;

import net.fabricmc.api.ModInitializer;

/** Registers the operator-only visual battle animation laboratory. */
public final class BattleAnimationLabEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        BattleAnimationLabRuntime.register();
    }
}
