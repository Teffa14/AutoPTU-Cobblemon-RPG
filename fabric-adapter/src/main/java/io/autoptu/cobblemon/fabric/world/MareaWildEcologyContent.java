package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import io.autoptu.cobblemon.fabric.battle.MareaCanonicalWildEncounterBlueprintSource;

import java.util.List;
import java.util.Set;

/** Authored Marea ecology data consumed by region-agnostic Wild* runtimes. */
final class MareaWildEcologyContent {
    private static final long LOWER_SHELF_CYCLE_TICKS = 168_000L;
    private static final long LOWER_SHELF_DEPARTURE_TICK = 84_000L;
    private static final long LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK = 90_001L;
    private static final long LOWER_SHELF_STOPOVER_END_TICK = 90_002L;
    private static final long LOWER_SHELF_FINAL_TRANSIT_END_TICK = 90_003L;
    private static final long LOWER_SHELF_ARRIVAL_END_TICK = 90_004L;
    private static final String LOWER_SHELF_STOPOVER_SITE_ID = "ouros.marea.sendero_crossing";

    private static final WildBehaviorProfile AMBIENT_BEHAVIOR_PROFILE = new WildBehaviorProfile(
            14.0D, 7.0D, 3, 5, 80L, 60L, 0.001D, 14.0D, 35.0F,
            0.025D, 1.0D, 2.5D, 0.018D, 6.0D, 0.012D, 0.08D, 0.04D, 1.5D);

    private static final Set<String> AUTHORED_ALPHA_ENCOUNTERS = Set.of(
            CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID,
            CanonicalWildEncounterCatalogue.MAREA_CROSSING_FLETCHLING_ID,
            CanonicalWildEncounterCatalogue.MAREA_MIRADOR_FLETCHLING_ID,
            CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_FLETCHLING_ID);

    private static final List<WildPopulationProjectionProfile> PROJECTION_PROFILES = List.of(
            new WildPopulationProjectionProfile(
                    "ouros.marea.migration.lower_shelf_to_crossing.v1",
                    CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
                    LOWER_SHELF_CYCLE_TICKS,
                    List.of(
                            WildPopulationProjectionProfile.Window.home(0L, LOWER_SHELF_DEPARTURE_TICK, MigrationPhase.PREPARING),
                            WildPopulationProjectionProfile.Window.home(LOWER_SHELF_DEPARTURE_TICK, LOWER_SHELF_DEPARTURE_TICK + 1L, MigrationPhase.DEPARTING),
                            WildPopulationProjectionProfile.Window.hidden(LOWER_SHELF_DEPARTURE_TICK + 1L, LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK, MigrationPhase.IN_TRANSIT),
                            WildPopulationProjectionProfile.Window.site(LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK, LOWER_SHELF_STOPOVER_END_TICK, MigrationPhase.STOPOVER, LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(LOWER_SHELF_STOPOVER_END_TICK, LOWER_SHELF_FINAL_TRANSIT_END_TICK, MigrationPhase.IN_TRANSIT, LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(LOWER_SHELF_FINAL_TRANSIT_END_TICK, LOWER_SHELF_ARRIVAL_END_TICK, MigrationPhase.ARRIVING, LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(LOWER_SHELF_ARRIVAL_END_TICK, LOWER_SHELF_CYCLE_TICKS, MigrationPhase.SEASONAL_RESIDENCE, LOWER_SHELF_STOPOVER_SITE_ID))));

    private static final List<WildEcologyDescriptorRegistry.Descriptor> DESCRIPTORS = List.of(
            new WildEcologyDescriptorRegistry.Descriptor(
                    "fixture.ouros.marea",
                    population -> population.siteId().startsWith("ouros.marea."),
                    world -> world != null && world.getServer() != null && world == world.getServer().getOverworld(),
                    PROJECTION_PROFILES,
                    new MareaCanonicalWildEncounterBlueprintSource(),
                    encounter -> encounter.speciesStatus() == CanonicalWildEncounterCatalogue.SpeciesStatus.OFFICIAL
                            && !encounter.fusion() && "standard".equals(encounter.formId()),
                    AMBIENT_BEHAVIOR_PROFILE,
                    encounter -> AUTHORED_ALPHA_ENCOUNTERS.contains(encounter.canonicalEncounterId()) ? WildSocialRole.ALPHA : WildSocialRole.MEMBER,
                    encounter -> AUTHORED_ALPHA_ENCOUNTERS.contains(encounter.canonicalEncounterId())
                            ? WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER
                            : WildEcologyDescriptorRegistry.PresentationCapabilities.NONE));

    private MareaWildEcologyContent() {}
    static List<WildPopulationProjectionProfile> projectionProfiles() { return PROJECTION_PROFILES; }
    static List<WildEcologyDescriptorRegistry.Descriptor> descriptors() { return DESCRIPTORS; }
}
