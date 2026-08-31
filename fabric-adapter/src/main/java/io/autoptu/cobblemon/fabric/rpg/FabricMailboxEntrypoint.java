package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.api.ModInitializer;

/** Isolated Fabric entrypoint for the physical Ouros mailbox surface. */
public final class FabricMailboxEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricMailboxRuntime.register();
    }
}
