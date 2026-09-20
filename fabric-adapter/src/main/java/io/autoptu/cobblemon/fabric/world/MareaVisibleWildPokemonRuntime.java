package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
        if (world == null) throw new IllegalArgumentException("world is required");
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !MAREA_ECOLOGY_SOURCE_ID.equals(descriptor.sourceId())) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                if (WildPopulationRuntime.ensureProjected(world, encounter) != null) visible++;
            }
        }
        return visible;
    }

    static PokemonEntity ensureProjected(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        return WildPopulationRuntime.ensureProjected(world, encounter);
    }

    static PokemonEntity actorForEncounter(ServerWorld world, String canonicalEncounterId) {
        return WildPopulationRuntime.actorForEncounter(world, canonicalEncounterId);
    }

    static int presenceReconcileIntervalTicks() {
        return WildPopulationRuntime.presenceReconcileIntervalTicks();
    }

    static int reconcileActivePopulations(ServerWorld world) {
        return WildPopulationRuntime.reconcileActivePopulations(world);
    }

    static void keepInProjectedHabitat(
            PokemonEntity entity,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String projectedSiteId
    ) {
        WildPopulationRuntime.keepInProjectedHabitat(entity, encounter, projectedSiteId);
    }

    static BlockPos projectedPresentationAnchor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String projectedSiteId
    ) {
        return WildPopulationRuntime.projectedPresentationAnchor(encounter, projectedSiteId);
    }
}
