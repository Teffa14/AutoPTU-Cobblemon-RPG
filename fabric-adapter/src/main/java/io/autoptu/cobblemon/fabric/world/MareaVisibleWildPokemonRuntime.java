package io.autoptu.cobblemon.fabric.world;

import net.minecraft.server.world.ServerWorld;

/**
 * Compatibility facade for existing Marea fixtures and smoke tests.
 *
 * <p>Normal activation, reconciliation, provisioning orchestration, binding and actor lifecycle now
 * belong to {@link WildPopulationRuntime}. Marea contributes authored content through
 * {@link WildEcologyDescriptorRegistry}; this class no longer owns production lifecycle policy.</p>
 */
public final class MareaVisibleWildPokemonRuntime {
    private static final String MAREA_ECOLOGY_SOURCE_ID = "fixture.ouros.marea";

    private MareaVisibleWildPokemonRuntime() {}

    public static void register() {
        WildPopulationRuntime.register();
    }

    /** Explicit Marea-only projection retained for the authored build command and runtime smoke. */
    public static int ensureProjected(ServerWorld world) {
        return WildPopulationSourceProjectionRuntime.ensureProjected(world, MAREA_ECOLOGY_SOURCE_ID);
    }
}
