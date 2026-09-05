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
 * {@link WildPopulationContentRegistry}; this class no longer owns production lifecycle policy.</p>
 */
public final class MareaVisibleWildPokemonRuntime {
    private MareaVisibleWildPokemonRuntime() {}

    public static void register() {
        WildPopulationRuntime.register();
    }

    /** Explicit Marea-only projection retained for the authored build command and runtime smoke. */
    public static int ensureProjected(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
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
        requireMareaEncounter(encounter);
        return WildPopulationRuntime.ensureProjected(world, encounter);
    }

    static PokemonEntity actorForEncounter(ServerWorld world, String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return null;
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(canonicalEncounterId.strip()).orElse(null);
        if (encounter == null || !encounter.siteId().startsWith("ouros.marea.")) return null;
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
        requireMareaEncounter(encounter);
        WildPopulationRuntime.keepInProjectedHabitat(entity, encounter, projectedSiteId);
    }

    static BlockPos projectedPresentationAnchor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String projectedSiteId
    ) {
        requireMareaEncounter(encounter);
        return WildPopulationRuntime.projectedPresentationAnchor(encounter, projectedSiteId);
    }

    private static void requireMareaEncounter(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter == null) throw new IllegalArgumentException("encounter is required");
        if (!encounter.siteId().startsWith("ouros.marea.")) {
            throw new IllegalArgumentException("Marea compatibility facade accepts only Marea authored encounters");
        }
    }
}
