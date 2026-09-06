package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.fabric.battle.MareaCanonicalWildEncounterBlueprintSource;
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
        WildPopulationContentRegistry.register(new WildPopulationContentRegistry.Source(
                "fixture.ouros.marea",
                population -> population.siteId().startsWith("ouros.marea."),
                world -> world != null && world.getServer() != null && world == world.getServer().getOverworld(),
                WildPopulationContentRegistry.projectionResolver(MareaWildEcologyContent.projectionProfiles()),
                new MareaCanonicalWildEncounterBlueprintSource(),
                encounter -> encounter.speciesStatus() == CanonicalWildEncounterCatalogue.SpeciesStatus.OFFICIAL
                        && !encounter.fusion()
                        && "standard".equals(encounter.formId())
        ));
        WildEcologyProjectionRegistry.register(
                "fixture.ouros.marea",
                MareaWildEcologyProjectionSource::projectedActors
        );
    }
}
