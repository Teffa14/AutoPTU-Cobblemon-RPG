package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Data-driven server-owned schedule for projecting one canonical WILD population through Minecraft sites.
 *
 * <p>The profile changes world presence only. It never supplies species, level, stats, moves, HP,
 * abilities, statuses, battle legality, RNG or encounter outcomes. Region modules author profiles;
 * the global population runtime consumes the resulting projected site through
 * {@link WildPopulationContentRegistry.ProjectedSiteResolver}.</p>
 */
public record WildPopulationProjectionProfile(
        String profileId,
        String populationId,
        long cycleTicks,
        List<Window> windows
) {
    public WildPopulationProjectionProfile {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profileId is required");
        if (populationId == null || populationId.isBlank()) throw new IllegalArgumentException("populationId is required");
        if (cycleTicks <= 0L) throw new IllegalArgumentException("cycleTicks must be > 0");
        if (windows == null || windows.isEmpty()) throw new IllegalArgumentException("windows are required");
        profileId = profileId.strip();
        populationId = populationId.strip();
        windows = windows.stream().sorted(Comparator.comparingLong(Window::startTickInclusive)).toList();

        long expectedStart = 0L;
        for (Window window : windows) {
            if (window.startTickInclusive() != expectedStart) {
                throw new IllegalArgumentException("projection windows must be contiguous from tick 0");
            }
            expectedStart = window.endTickExclusive();
        }
        if (expectedStart != cycleTicks) {
            throw new IllegalArgumentException("projection windows must cover the complete cycle");
        }
    }

    public Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        return resolve(population, worldTick).siteId();
    }

    public Projection resolve(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        Objects.requireNonNull(population, "population");
        if (worldTick < 0L) throw new IllegalArgumentException("worldTick must be >= 0");
        if (!populationId.equals(population.populationId())) {
            return new Projection(MigrationPhase.PREPARING, Optional.of(population.siteId()));
        }

        long cycleTick = Math.floorMod(worldTick, cycleTicks);
        for (Window window : windows) {
            if (window.contains(cycleTick)) {
                Optional<String> site = switch (window.destination()) {
                    case HOME -> Optional.of(population.siteId());
                    case HIDDEN -> Optional.empty();
                    case SITE -> Optional.of(window.siteId());
                };
                return new Projection(window.phase(), site);
            }
        }
        throw new IllegalStateException("projection profile does not cover cycle tick " + cycleTick);
    }

    public record Projection(MigrationPhase phase, Optional<String> siteId) {
        public Projection {
            Objects.requireNonNull(phase, "phase");
            siteId = siteId == null ? Optional.empty() : siteId;
        }
    }

    public record Window(
            long startTickInclusive,
            long endTickExclusive,
            MigrationPhase phase,
            Destination destination,
            String siteId
    ) {
        public Window {
            if (startTickInclusive < 0L) throw new IllegalArgumentException("window start must be >= 0");
            if (endTickExclusive <= startTickInclusive) throw new IllegalArgumentException("window end must be > start");
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(destination, "destination");
            if (destination == Destination.SITE) {
                if (siteId == null || siteId.isBlank()) throw new IllegalArgumentException("SITE destination requires siteId");
                siteId = siteId.strip();
            } else if (siteId != null) {
                throw new IllegalArgumentException("HOME/HIDDEN destinations must not define siteId");
            }
        }

        boolean contains(long tick) {
            return tick >= startTickInclusive && tick < endTickExclusive;
        }

        public static Window home(long start, long end, MigrationPhase phase) {
            return new Window(start, end, phase, Destination.HOME, null);
        }

        public static Window hidden(long start, long end, MigrationPhase phase) {
            return new Window(start, end, phase, Destination.HIDDEN, null);
        }

        public static Window site(long start, long end, MigrationPhase phase, String siteId) {
            return new Window(start, end, phase, Destination.SITE, siteId);
        }
    }

    public enum Destination {
        HOME,
        HIDDEN,
        SITE
    }
}
