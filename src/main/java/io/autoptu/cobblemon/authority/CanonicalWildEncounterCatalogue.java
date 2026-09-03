package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-authored complete wild encounter definitions used before any Cobblemon presentation actor exists.
 *
 * <p>This catalogue is deliberately content data rather than a species-to-PTU derivation service. Every
 * mechanical value required by the current wild encounter bootstrap is frozen here from an approved Ouros
 * content decision and source profile. Minecraft/Cobblemon may later render the species but may not fill in
 * or overwrite missing PTU values.</p>
 */
public final class CanonicalWildEncounterCatalogue {
    public static final String MAREA_FIRST_FLETCHLING_ID =
            "ouros.marea.encounter.sendero_lower_shelf.fletchling.0";
    public static final String MAREA_SECOND_FLETCHLING_ID =
            "ouros.marea.encounter.sendero_lower_shelf.fletchling.1";
    public static final String MAREA_CROSSING_FLETCHLING_ID =
            "ouros.marea.encounter.sendero_crossing.fletchling.0";
    public static final String MAREA_CROSSING_SECOND_FLETCHLING_ID =
            "ouros.marea.encounter.sendero_crossing.fletchling.1";
    public static final String MAREA_MIRADOR_FLETCHLING_ID =
            "ouros.marea.encounter.mirador_transect.fletchling.0";
    public static final String MAREA_MIRADOR_SECOND_FLETCHLING_ID =
            "ouros.marea.encounter.mirador_transect.fletchling.1";

    public static final CanonicalWildEncounterCatalogue DEFAULT = new CanonicalWildEncounterCatalogue(List.of(
            mareaFletchling(
                    MAREA_FIRST_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
                    "ouros.marea.sendero_vidrio",
                    "ouros.marea.sendero_vidrio",
                    "lower_shelf_first_slice_v1",
                    3, 1, 3),
            mareaFletchling(
                    MAREA_SECOND_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID,
                    "ouros.marea.sendero_vidrio",
                    "ouros.marea.sendero_vidrio",
                    "lower_shelf_second_slice_v1",
                    8, 1, -2),
            mareaFletchling(
                    MAREA_CROSSING_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_CROSSING_POPULATION_ID,
                    "ouros.marea.sendero_crossing",
                    "ouros.marea.sendero_vidrio",
                    "seasonal_crossing_first_slice_v1",
                    2, 1, 1),
            mareaFletchling(
                    MAREA_CROSSING_SECOND_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_CROSSING_POPULATION_ID,
                    "ouros.marea.sendero_crossing",
                    "ouros.marea.sendero_vidrio",
                    "seasonal_crossing_second_slice_v1",
                    -4, 1, 5),
            mareaFletchling(
                    MAREA_MIRADOR_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_MIRADOR_TRANSECT_POPULATION_ID,
                    "ouros.marea.mirador_transect",
                    "ouros.marea.estacion_mirador",
                    "mirador_transect_first_slice_v1",
                    3, 2, 2),
            mareaFletchling(
                    MAREA_MIRADOR_SECOND_FLETCHLING_ID,
                    CanonicalWildPopulationCatalogue.MAREA_MIRADOR_TRANSECT_POPULATION_ID,
                    "ouros.marea.mirador_transect",
                    "ouros.marea.estacion_mirador",
                    "mirador_transect_second_slice_v1",
                    -3, 2, -3)
    ));

    private final Map<String, EncounterDefinition> encounters;

    public CanonicalWildEncounterCatalogue(List<EncounterDefinition> encounters) {
        Objects.requireNonNull(encounters, "encounters");
        LinkedHashMap<String, EncounterDefinition> indexed = new LinkedHashMap<>();
        for (EncounterDefinition encounter : encounters) {
            Objects.requireNonNull(encounter, "encounter");
            if (indexed.putIfAbsent(encounter.canonicalEncounterId(), encounter) != null) {
                throw new IllegalArgumentException("duplicate canonical wild encounter id: "
                        + encounter.canonicalEncounterId());
            }
        }
        this.encounters = Map.copyOf(indexed);
    }

    public Optional<EncounterDefinition> encounter(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        return Optional.ofNullable(encounters.get(canonicalEncounterId.strip()));
    }

    public List<EncounterDefinition> encounters() {
        return List.copyOf(encounters.values());
    }

    public List<EncounterDefinition> encountersForPopulation(String populationId) {
        if (populationId == null || populationId.isBlank()) return List.of();
        String normalized = populationId.strip();
        return encounters.values().stream()
                .filter(encounter -> normalized.equals(encounter.populationId()))
                .toList();
    }

    private static EncounterDefinition mareaFletchling(
            String canonicalEncounterId,
            String populationId,
            String siteId,
            String zoneId,
            String contextId,
            int offsetX,
            int offsetY,
            int offsetZ
    ) {
        return new EncounterDefinition(
                canonicalEncounterId,
                populationId,
                siteId,
                zoneId,
                contextId,
                "fletchling",
                "standard",
                SpeciesStatus.OFFICIAL,
                false,
                5,
                35,
                12,
                8,
                10,
                9,
                11,
                8,
                5,
                4,
                4,
                4,
                Set.of("Overland:5"),
                Set.of(),
                Set.of("tackle", "growl", "quick_attack"),
                Set.of(),
                Set.of(),
                offsetX,
                offsetY,
                offsetZ
        );
    }

    public enum SpeciesStatus {
        OFFICIAL,
        APPROVED_ORIGINAL
    }

    public record EncounterDefinition(
            String canonicalEncounterId,
            String populationId,
            String siteId,
            String zoneId,
            String contextId,
            String speciesId,
            String formId,
            SpeciesStatus speciesStatus,
            boolean fusion,
            int level,
            int maxHp,
            int attack,
            int defense,
            int specialAttack,
            int specialDefense,
            int speed,
            int overland,
            int swim,
            int sky,
            int power,
            int jump,
            Set<String> capabilities,
            Set<String> abilities,
            Set<String> moveIds,
            Set<String> statuses,
            Set<String> heldItems,
            int presentationOffsetX,
            int presentationOffsetY,
            int presentationOffsetZ
    ) {
        public EncounterDefinition {
            canonicalEncounterId = requireText(canonicalEncounterId, "canonicalEncounterId");
            populationId = requireText(populationId, "populationId");
            siteId = requireText(siteId, "siteId");
            zoneId = requireText(zoneId, "zoneId");
            contextId = requireText(contextId, "contextId");
            speciesId = requireText(speciesId, "speciesId");
            formId = requireText(formId, "formId");
            speciesStatus = Objects.requireNonNull(speciesStatus, "speciesStatus");
            capabilities = immutableSet(capabilities);
            abilities = immutableSet(abilities);
            moveIds = immutableSet(moveIds);
            statuses = immutableSet(statuses);
            heldItems = immutableSet(heldItems);
            if (level < 1) throw new IllegalArgumentException("level must be positive");
            if (maxHp < 1) throw new IllegalArgumentException("maxHp must be positive");
            if (attack < 1 || defense < 1 || specialAttack < 1 || specialDefense < 1 || speed < 1) {
                throw new IllegalArgumentException("combat stats must be positive");
            }
            if (overland < 0 || swim < 0 || sky < 0 || power < 0 || jump < 0) {
                throw new IllegalArgumentException("movement/capability values cannot be negative");
            }
            if (moveIds.isEmpty()) throw new IllegalArgumentException("canonical wild encounter requires at least one move");
        }
    }

    private static Set<String> immutableSet(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        return Set.copyOf(values);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
