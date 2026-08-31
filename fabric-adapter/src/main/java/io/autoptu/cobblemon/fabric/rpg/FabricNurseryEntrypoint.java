package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.fabric.persistence.FabricNurseryStoreRuntime;
import net.fabricmc.api.ModInitializer;

/** Isolated Fabric initializer for the nursery persistence slice. */
public final class FabricNurseryEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNurseryStoreRuntime.register();
        FabricNurseryRuntime.register();
    }
}