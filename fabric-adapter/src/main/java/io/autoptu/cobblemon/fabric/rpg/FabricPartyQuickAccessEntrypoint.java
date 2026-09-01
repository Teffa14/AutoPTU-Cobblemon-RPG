package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.api.ModInitializer;

/** Dedicated entrypoint for the normal-player party quick-access gesture. */
public final class FabricPartyQuickAccessEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricPartyQuickAccessRuntime.register();
    }
}
