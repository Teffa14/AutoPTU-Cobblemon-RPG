package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;

/** Central registration boundary for authored visible-wild ecology content. */
public final class WildEcologyContentRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        for (var descriptor : MareaWildEcologyContent.descriptors()) {
            WildEcologyDescriptorRegistry.register(descriptor);
        }
        WildEcologyProjectionRegistry.register(
                "server-owned.visible-wilds",
                WildEcologyProjectionSource::projectedActors
        );
    }
}
