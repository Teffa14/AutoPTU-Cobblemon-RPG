package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-authored visible-wild population policy.
 *
 * <p>This catalogue owns only world presence: which already-complete canonical encounter members
 * belong to a visible population and where that population becomes physically present. It never
 * derives species, level, stats, moves, HP, abilities or battle legality. Those facts stay frozen
 * in {@link CanonicalWildEncounterCatalogue} and are consumed by the battle handoff unchanged.</p>
 */
public final class CanonicalWildPopulationCatalogue {
    public static final String MAREA_LOWER_SHELF_POPULATION_ID =
            "ouros.marea.wild.sendero_lower_shelf.fletchling.v1";
    public static final String MAREA_CROSSING_POPULATION_ID =
            "ouros.marea.wild.sendero_crossing.fletchling.v1";
    public static final String MAREA_MIRADOR_TRANSECT_POPULATION_ID =
            "ouros.marea.wild.mirador_transect.fletchling.v1";
    public static final String MAREA_LOMA_WINDBREAK_POPULATION_ID =
            "ouros.marea.wild.loma_windbreak.fletchling.v1";

    public static final CanonicalWildPopulationCatalogue DEFAULT = new CanonicalWildPopulationCatalogue(List.of(
            new PopulationDefinition(
                    MAREA_LOWER_SHELF_POPULATION_ID,
                    "ouros.marea.sendero_vidrio",
                    "ouros.marea.sendero_vidrio",
                    List.of(
                            CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID,
                            CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID
                    ),
                    new PresenceFootprint(48, 24, 56),
                    new RoamingFootprint(28, 10, 34)
            ),
            new PopulationDefinition(
                    MAREA_CROSSING_POPULATION_ID,
                    "ouros.marea.sendero_crossing",
                    "ouros.marea.sendero_vidrio",
                    List.of(
                            CanonicalWildEncounterCatalogue.MAREA_CROSSING_FLETCHLING_ID,
                            CanonicalWildEncounterCatalogue.MAREA_CROSSING_SECOND_FLETCHLING_ID
                    ),
                    new PresenceFootprint(40, 24, 40),
                    new RoamingFootprint(20, 8, 20)
            ),
            new PopulationDefinition(
                    MAREA_MIRADOR_TRANSECT_POPULATION_ID,
                    "ouros.marea.mirador_transect",
                    "ouros.marea.estacion_mirador",
                    List.of(
                            CanonicalWildEncounterCatalogue.MAREA_MIRADOR_FLETCHLING_ID,
                            CanonicalWildEncounterCatalogue.MAREA_MIRADOR_SECOND_FLETCHLING_ID
                    ),
                    new PresenceFootprint(48, 28, 48),
                    new RoamingFootprint(26, 12, 26)
            ),
            new PopulationDefinition(
                    MAREA_LOMA_WINDBREAK_POPULATION_ID,
                    "ouros.marea.loma_windbreak",
                    "ouros.marea.loma_clara",
                    List.of(
                            CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_FLETCHLING_ID,
                            CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_SECOND_FLETCHLING_ID
                    ),
                    new PresenceFootprint(40, 24, 44),
                    new RoamingFootprint(22, 10, 26)
            )
    ));

    private final Map<String, PopulationDefinition> populations;

    public CanonicalWildPopulationCatalogue(List<PopulationDefinition> populations) {
        Objects.requireNonNull(populations, "populations");
        LinkedHashMap<String, PopulationDefinition> indexed = new LinkedHashMap<>();
        for (PopulationDefinition population : populations) {
            Objects.requireNonNull(population, "population");
            if (indexed.putIfAbsent(population.populationId(), population) != null) {
                throw new IllegalArgumentException("duplicate canonical wild population id: " + population.populationId());
            }
            validateMembers(population);
        }
        this.populations = Map.copyOf(indexed);
    }

    public Optional<PopulationDefinition> population(String populationId) {
        if (populationId == null || populationId.isBlank()) return Optional.empty();
        return Optional.ofNullable(populations.get(populationId.strip()));
    }

    public List<PopulationDefinition> populations() {
        return List.copyOf(populations.values());
    }

    public List<CanonicalWildEncounterCatalogue.EncounterDefinition> members(PopulationDefinition population) {
        Objects.requireNonNull(population, "population");
        return population.encounterIds().stream()
                .map(id -> CanonicalWildEncounterCatalogue.DEFAULT.encounter(id)
                        .orElseThrow(() -> new IllegalStateException("missing canonical wild population member: " + id)))
                .toList();
    }

    private static void validateMembers(PopulationDefinition population) {
        if (population.encounterIds().isEmpty()) {
            throw new IllegalArgumentException("canonical wild population requires at least one encounter member");
        }
        if (population.encounterIds().stream().distinct().count() != population.encounterIds().size()) {
            throw new IllegalArgumentException("canonical wild population encounter members must be unique");
        }
        for (String encounterId : population.encounterIds()) {
            var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(encounterId)
                    .orElseThrow(() -> new IllegalArgumentException("unknown canonical wild encounter member: " + encounterId));
            if (!population.populationId().equals(encounter.populationId())) {
                throw new IllegalArgumentException("wild population member has mismatched population id: " + encounterId);
            }
            if (!population.siteId().equals(encounter.siteId())) {
                throw new IllegalArgumentException("wild population member has mismatched site id: " + encounterId);
            }
            if (!population.zoneId().equals(encounter.zoneId())) {
                throw new IllegalArgumentException("wild population member has mismatched zone id: " + encounterId);
            }
        }
    }

    public record PopulationDefinition(
            String populationId,
            String siteId,
            String zoneId,
            List<String> encounterIds,
            PresenceFootprint presenceFootprint,
            RoamingFootprint roamingFootprint
    ) {
        public PopulationDefinition {
            populationId = requireText(populationId, "populationId");
            siteId = requireText(siteId, "siteId");
            zoneId = requireText(zoneId, "zoneId");
            encounterIds = encounterIds == null ? List.of() : List.copyOf(encounterIds);
            encounterIds.forEach(id -> requireText(id, "encounterId"));
            presenceFootprint = Objects.requireNonNull(presenceFootprint, "presenceFootprint");
            roamingFootprint = Objects.requireNonNull(roamingFootprint, "roamingFootprint");
            if (roamingFootprint.halfExtentXBlocks() > presenceFootprint.halfExtentXBlocks()
                    || roamingFootprint.halfExtentYBlocks() > presenceFootprint.halfExtentYBlocks()
                    || roamingFootprint.halfExtentZBlocks() > presenceFootprint.halfExtentZBlocks()) {
                throw new IllegalArgumentException("roaming footprint must fit inside presence footprint");
            }
        }

        /** Compatibility constructor for tests/tools; production populations should author both footprints explicitly. */
        public PopulationDefinition(
                String populationId,
                String siteId,
                String zoneId,
                List<String> encounterIds,
                PresenceFootprint presenceFootprint
        ) {
            this(populationId, siteId, zoneId, encounterIds, presenceFootprint, new RoamingFootprint(32, 16, 32));
        }

        /** Compatibility constructor for tests/tools; production populations should author the footprints explicitly. */
        public PopulationDefinition(String populationId, String siteId, String zoneId, List<String> encounterIds) {
            this(populationId, siteId, zoneId, encounterIds, new PresenceFootprint(96, 96, 96), new RoamingFootprint(32, 16, 32));
        }
    }

    /** Axis-aligned Minecraft-world activation footprint around the population's canonical site anchor. */
    public record PresenceFootprint(int halfExtentXBlocks, int halfExtentYBlocks, int halfExtentZBlocks) {
        public PresenceFootprint {
            requirePositiveExtents(halfExtentXBlocks, halfExtentYBlocks, halfExtentZBlocks, "presence footprint");
        }

        public boolean containsOffset(double dx, double dy, double dz) {
            return contains(halfExtentXBlocks, halfExtentYBlocks, halfExtentZBlocks, dx, dy, dz);
        }
    }

    /**
     * Minecraft presentation leash around each canonical encounter anchor.
     * This is world ecology only and never grants or restricts PTU tactical movement.
     */
    public record RoamingFootprint(int halfExtentXBlocks, int halfExtentYBlocks, int halfExtentZBlocks) {
        public RoamingFootprint {
            requirePositiveExtents(halfExtentXBlocks, halfExtentYBlocks, halfExtentZBlocks, "roaming footprint");
        }

        public boolean containsOffset(double dx, double dy, double dz) {
            return contains(halfExtentXBlocks, halfExtentYBlocks, halfExtentZBlocks, dx, dy, dz);
        }
    }

    private static boolean contains(int x, int y, int z, double dx, double dy, double dz) {
        return Math.abs(dx) <= x && Math.abs(dy) <= y && Math.abs(dz) <= z;
    }

    private static void requirePositiveExtents(int x, int y, int z, String field) {
        if (x <= 0 || y <= 0 || z <= 0) {
            throw new IllegalArgumentException(field + " extents must be positive");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
