package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.api.ModInitializer;

/** Dedicated entrypoint for the normal-player canonical bag quick-access gesture. */
public final class FabricBagQuickAccessEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricBagQuickAccessRuntime.register();
    }
}
