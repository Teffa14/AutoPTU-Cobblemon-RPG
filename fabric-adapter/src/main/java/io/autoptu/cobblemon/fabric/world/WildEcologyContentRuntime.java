package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.fabric.battle.MareaCanonicalWildEncounterBlueprintSource;
import net.fabricmc.api.ModInitializer;

/**
 * Central registration boundary for authored visible-wild ecology content.
 *
 * Region/species modules contribute server-authored data here. Projection assembly and gameplay behavior
 * remain in generic Wild* runtimes, so adding an approved region does not add another Fabric behavior source.
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
        for (var source : MareaWildEcologyContent.ecologyProjectionSources()) {
            WildEcologyProjectionContentRegistry.register(source);
        }
        WildEcologyProjectionRegistry.register(
                "server-owned.visible-wilds",
                WildEcologyProjectionSource::projectedActors
        );
    }
}
