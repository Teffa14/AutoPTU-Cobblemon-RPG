package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;

/**
 * Central registration boundary for authored visible-wild ecology sources.
 *
 * Region/species content may contribute projection sources here, but gameplay behavior remains in
 * the generic Wild* runtimes. Adding another approved region therefore adds data/source registration
 * rather than another Fabric behavior entrypoint.
 */
public final class WildEcologyContentRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        WildEcologyProjectionRegistry.register("fixture.ouros.marea", MareaWildEcologyProjectionSource::projectedActors);
    }
}
