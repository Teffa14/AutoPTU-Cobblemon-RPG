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

    public static final CanonicalWildEncounterCatalogue DEFAULT = new CanonicalWildEncounterCatalogue(List.of(
            mareaFletchling(
                    MAREA_FIRST_FLETCHLING_ID,
                    "ouros.marea.wild.sendero_lower_shelf.fletchling.v1",
                    "ouros.marea.sendero_vidrio",
                    "ouros.marea.sendero_vidrio",
                    "lower_shelf_first_slice_v1",
                    3, 1, 3),
            mareaFletchling(
                    MAREA_SECOND_FLETCHLING_ID,
                    "ouros.marea.wild.sendero_lower_shelf.fletchling.v1",
                    "ouros.marea.sendero_vidrio",
                    "ouros.marea.sendero_vidrio",
                    "lower_shelf_second_slice_v1",
                    8, 1, -2),
            mareaFletchling(
                    MAREA_CROSSING_FLETCHLING_ID,
                    "ouros.marea.wild.sendero_crossing.fletchling.v1",
                    "ouros.marea.sendero_crossing",
                    "ouros.marea.sendero_vidrio",
                    "seasonal_crossing_first_slice_v1",
                    2, 1, 1)
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
            int presentationOffsetX,
            int presentationOffsetY,
            int presentationOffsetZ
    ) {
        return new EncounterDefinition(
                canonicalEncounterId,
                populationId,
                siteId,
                zoneId,
                contextId,
                1,
                presentationOffsetX,
                presentationOffsetY,
                presentationOffsetZ,
                "fletchling",
                "standard",
                SpeciesStatus.OFFICIAL,
                false,
                "OUROS-CANON-APPROVED",
                "ouros.vertical_slice.ptu_1_05.fletchling_v1",
                5,
                Set.of("guster", "underdog"),
                Set.of(),
                new CanonicalStatusState(List.of()),
                new CanonicalCombatStats(8, 6, 6, 6, 9),
                new CanonicalHealth(39, 39),
                new CanonicalMoveLoadout(List.of("tackle", "growl")),
                new CanonicalBaseMovement(3, 0, 5, 1, 1),
                new CanonicalBattleTraits(List.of("normal", "flying"), List.of("big-pecks")),
                new CanonicalAccuracyEvasion(0, 1, 1, 1),
                new CanonicalInjuryState(0),
                null,
                0L
        );
    }

    public enum SpeciesStatus {
        OFFICIAL,
        UNOFFICIAL
    }

    public record EncounterDefinition(
            String canonicalEncounterId,
            String populationId,
            String siteId,
            String zoneId,
            String contextId,
            int side,
            int presentationOffsetX,
            int presentationOffsetY,
            int presentationOffsetZ,
            String speciesId,
            String formId,
            SpeciesStatus speciesStatus,
            boolean fusion,
            String ourosAuthorization,
            String mechanicalProfileId,
            int level,
            Set<String> capabilities,
            Set<String> statuses,
            CanonicalStatusState statusState,
            CanonicalCombatStats combatStats,
            CanonicalHealth health,
            CanonicalMoveLoadout moveLoadout,
            CanonicalBaseMovement baseMovement,
            CanonicalBattleTraits battleTraits,
            CanonicalAccuracyEvasion accuracyEvasion,
            CanonicalInjuryState injuryState,
            String heldItemInstanceId,
            long revision
    ) {
        public EncounterDefinition {
            canonicalEncounterId = requireText(canonicalEncounterId, "canonicalEncounterId");
            populationId = requireText(populationId, "populationId");
            siteId = requireText(siteId, "siteId");
            zoneId = requireText(zoneId, "zoneId");
            contextId = requireText(contextId, "contextId");
            if (side < 0) throw new IllegalArgumentException("side must be >= 0");
            speciesId = requireText(speciesId, "speciesId");
            formId = requireText(formId, "formId");
            speciesStatus = Objects.requireNonNull(speciesStatus, "speciesStatus");
            ourosAuthorization = requireText(ourosAuthorization, "ourosAuthorization");
            mechanicalProfileId = requireText(mechanicalProfileId, "mechanicalProfileId");
            if (level < 1) throw new IllegalArgumentException("level must be >= 1");
            capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
            statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
            statusState = Objects.requireNonNull(statusState, "statusState");
            combatStats = Objects.requireNonNull(combatStats, "combatStats");
            health = Objects.requireNonNull(health, "health");
            moveLoadout = Objects.requireNonNull(moveLoadout, "moveLoadout");
            baseMovement = Objects.requireNonNull(baseMovement, "baseMovement");
            battleTraits = Objects.requireNonNull(battleTraits, "battleTraits");
            accuracyEvasion = Objects.requireNonNull(accuracyEvasion, "accuracyEvasion");
            injuryState = Objects.requireNonNull(injuryState, "injuryState");
            heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank()
                    ? null
                    : heldItemInstanceId.strip();
            if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");

            if (fusion) {
                throw new IllegalArgumentException("Pokemon fusions are prohibited by the active Ouros project invariant");
            }
            if (speciesStatus == SpeciesStatus.UNOFFICIAL && !"OUROS-APPROVED".equals(ourosAuthorization)) {
                throw new IllegalArgumentException(
                        "unofficial species/forms require exceptional OUROS-APPROVED authorization"
                );
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
