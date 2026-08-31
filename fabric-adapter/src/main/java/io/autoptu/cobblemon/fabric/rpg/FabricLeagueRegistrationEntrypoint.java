package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.api.ModInitializer;

/** Isolated Fabric initializer for Gym/League registration state. */
public final class FabricLeagueRegistrationEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricLeagueRegistrationRuntime.register();
    }
}
