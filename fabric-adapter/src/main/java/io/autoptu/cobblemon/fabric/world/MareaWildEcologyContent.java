package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;

import java.util.List;

/**
 * Authored Marea wild-ecology content consumed by the region-agnostic Wild* runtimes.
 *
 * <p>This class contains data only. Projection lifecycle and server-clock resolution are owned by
 * {@link WildPopulationProjectionProfile} and {@link WildPopulationContentRegistry}. Adding another
 * migratory population means adding another profile to this content list, not another projection
 * runtime or resolver class.</p>
 */
final class MareaWildEcologyContent {
    private static final long LOWER_SHELF_CYCLE_TICKS = 168_000L;
    private static final long LOWER_SHELF_DEPARTURE_TICK = 84_000L;
    private static final long LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK = 90_001L;
    private static final long LOWER_SHELF_STOPOVER_END_TICK = 90_002L;
    private static final long LOWER_SHELF_FINAL_TRANSIT_END_TICK = 90_003L;
    private static final long LOWER_SHELF_ARRIVAL_END_TICK = 90_004L;
    private static final String LOWER_SHELF_STOPOVER_SITE_ID = "ouros.marea.sendero_crossing";

    private static final List<WildPopulationProjectionProfile> PROJECTION_PROFILES = List.of(
            new WildPopulationProjectionProfile(
                    "ouros.marea.migration.lower_shelf_to_crossing.v1",
                    CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
                    LOWER_SHELF_CYCLE_TICKS,
                    List.of(
                            WildPopulationProjectionProfile.Window.home(
                                    0L, LOWER_SHELF_DEPARTURE_TICK, MigrationPhase.PREPARING),
                            WildPopulationProjectionProfile.Window.home(
                                    LOWER_SHELF_DEPARTURE_TICK,
                                    LOWER_SHELF_DEPARTURE_TICK + 1L,
                                    MigrationPhase.DEPARTING),
                            WildPopulationProjectionProfile.Window.hidden(
                                    LOWER_SHELF_DEPARTURE_TICK + 1L,
                                    LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK,
                                    MigrationPhase.IN_TRANSIT),
                            WildPopulationProjectionProfile.Window.site(
                                    LOWER_SHELF_OUTBOUND_TRANSIT_END_TICK,
                                    LOWER_SHELF_STOPOVER_END_TICK,
                                    MigrationPhase.STOPOVER,
                                    LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(
                                    LOWER_SHELF_STOPOVER_END_TICK,
                                    LOWER_SHELF_FINAL_TRANSIT_END_TICK,
                                    MigrationPhase.IN_TRANSIT,
                                    LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(
                                    LOWER_SHELF_FINAL_TRANSIT_END_TICK,
                                    LOWER_SHELF_ARRIVAL_END_TICK,
                                    MigrationPhase.ARRIVING,
                                    LOWER_SHELF_STOPOVER_SITE_ID),
                            WildPopulationProjectionProfile.Window.site(
                                    LOWER_SHELF_ARRIVAL_END_TICK,
                                    LOWER_SHELF_CYCLE_TICKS,
                                    MigrationPhase.SEASONAL_RESIDENCE,
                                    LOWER_SHELF_STOPOVER_SITE_ID)
                    )
            )
    );

    private MareaWildEcologyContent() {}

    static List<WildPopulationProjectionProfile> projectionProfiles() {
        return PROJECTION_PROFILES;
    }
}
