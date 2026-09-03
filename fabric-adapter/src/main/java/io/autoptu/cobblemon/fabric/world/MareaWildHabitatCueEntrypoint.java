package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;

/** Registers the normal-world Marea habitat feedback runtime. */
public final class MareaWildHabitatCueEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        MareaWildHabitatCueRuntime.register();
    }
}
