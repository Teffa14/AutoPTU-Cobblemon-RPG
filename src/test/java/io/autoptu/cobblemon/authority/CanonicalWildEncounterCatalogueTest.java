package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalWildEncounterCatalogueTest {
    @Test
    void mareaWildCatalogueHasFourPopulationsBackedByTheApprovedCompleteFletchlingProfile() {
        var first = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
        var second = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID).orElseThrow();
        var crossing = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_CROSSING_FLETCHLING_ID).orElseThrow();
        var crossingSecond = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_CROSSING_SECOND_FLETCHLING_ID).orElseThrow();
        var mirador = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_MIRADOR_FLETCHLING_ID).orElseThrow();
        var miradorSecond = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_MIRADOR_SECOND_FLETCHLING_ID).orElseThrow();
        var windbreak = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_FLETCHLING_ID).orElseThrow();
        var windbreakSecond = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_LOMA_WINDBREAK_SECOND_FLETCHLING_ID).orElseThrow();

        assertEquals(8, CanonicalWildEncounterCatalogue.DEFAULT.encounters().size());
        assertEquals(2, CanonicalWildEncounterCatalogue.DEFAULT.encountersForPopulation(first.populationId()).size());
        assertEquals(2, CanonicalWildEncounterCatalogue.DEFAULT.encountersForPopulation(crossing.populationId()).size());
        assertEquals(2, CanonicalWildEncounterCatalogue.DEFAULT.encountersForPopulation(mirador.populationId()).size());
        assertEquals(2, CanonicalWildEncounterCatalogue.DEFAULT.encountersForPopulation(windbreak.populationId()).size());
        assertEquals(first.populationId(), second.populationId());
        assertEquals(crossing.populationId(), crossingSecond.populationId());
        assertEquals(mirador.populationId(), miradorSecond.populationId());
        assertEquals(windbreak.populationId(), windbreakSecond.populationId());
        assertNotEquals(first.populationId(), crossing.populationId());
        assertNotEquals(first.populationId(), mirador.populationId());
        assertNotEquals(first.populationId(), windbreak.populationId());
        assertNotEquals(crossing.populationId(), mirador.populationId());
        assertNotEquals(crossing.populationId(), windbreak.populationId());
        assertNotEquals(mirador.populationId(), windbreak.populationId());
        assertEquals(4, CanonicalWildPopulationCatalogue.DEFAULT.populations().size());
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            assertEquals(2, CanonicalWildPopulationCatalogue.DEFAULT.members(population).size());
        }

        assertCompleteApprovedMareaFletchling(first, "ouros.marea.sendero_vidrio");
        assertCompleteApprovedMareaFletchling(second, "ouros.marea.sendero_vidrio");
        assertCompleteApprovedMareaFletchling(crossing, "ouros.marea.sendero_vidrio");
        assertCompleteApprovedMareaFletchling(crossingSecond, "ouros.marea.sendero_vidrio");
        assertCompleteApprovedMareaFletchling(mirador, "ouros.marea.estacion_mirador");
        assertCompleteApprovedMareaFletchling(miradorSecond, "ouros.marea.estacion_mirador");
        assertCompleteApprovedMareaFletchling(windbreak, "ouros.marea.loma_clara");
        assertCompleteApprovedMareaFletchling(windbreakSecond, "ouros.marea.loma_clara");
    }

    private static void assertCompleteApprovedMareaFletchling(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String expectedZoneId
    ) {
        assertTrue(encounter.populationId().startsWith("ouros.marea.wild."));
        assertTrue(encounter.siteId().startsWith("ouros.marea."));
        assertTrue(CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId()).isPresent());
        assertEquals(expectedZoneId, encounter.zoneId());
        assertEquals("fletchling", encounter.speciesId());
        assertEquals("standard", encounter.formId());
        assertEquals(CanonicalWildEncounterCatalogue.SpeciesStatus.OFFICIAL, encounter.speciesStatus());
        assertFalse(encounter.fusion());
        assertEquals("OUROS-CANON-APPROVED", encounter.ourosAuthorization());
        assertEquals("ouros.vertical_slice.ptu_1_05.fletchling_v1", encounter.mechanicalProfileId());
        assertEquals(5, encounter.level());
        assertEquals(new CanonicalCombatStats(8, 6, 6, 6, 9), encounter.combatStats());
        assertEquals(new CanonicalHealth(39, 39), encounter.health());
        assertEquals(List.of("tackle", "growl"), encounter.moveLoadout().moveIds());
        assertEquals(new CanonicalBaseMovement(3, 0, 5, 1, 1), encounter.baseMovement());
        assertEquals(List.of("normal", "flying"), encounter.battleTraits().types());
        assertEquals(List.of("big-pecks"), encounter.battleTraits().abilities());
        assertEquals(new CanonicalAccuracyEvasion(0, 1, 1, 1), encounter.accuracyEvasion());
        assertEquals(0, encounter.injuryState().injuries());
        assertTrue(encounter.statuses().isEmpty());
        assertTrue(encounter.statusState().entries().isEmpty());
    }

    @Test
    void unofficialContentNeedsExceptionalOurosApprovalAndFusionsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> definition(CanonicalWildEncounterCatalogue.SpeciesStatus.UNOFFICIAL, false, "OUROS-CANON-APPROVED"));
        assertThrows(IllegalArgumentException.class, () -> definition(CanonicalWildEncounterCatalogue.SpeciesStatus.UNOFFICIAL, true, "OUROS-APPROVED"));
    }

    private static CanonicalWildEncounterCatalogue.EncounterDefinition definition(CanonicalWildEncounterCatalogue.SpeciesStatus speciesStatus, boolean fusion, String authorization) {
        return new CanonicalWildEncounterCatalogue.EncounterDefinition(
                "test.encounter", "test.population", "ouros.marea.sendero_vidrio", "test.zone", "test.context", 1,
                0, 1, 0, "test-species", "standard", speciesStatus, fusion, authorization, "test.profile", 5,
                Set.of(), Set.of(), new CanonicalStatusState(List.of()), new CanonicalCombatStats(1, 1, 1, 1, 1),
                new CanonicalHealth(1, 1), new CanonicalMoveLoadout(List.of("tackle")), new CanonicalBaseMovement(1, 0, 0, 0, 0),
                new CanonicalBattleTraits(List.of("normal"), List.of()), new CanonicalAccuracyEvasion(0, 0, 0, 0),
                new CanonicalInjuryState(0), null, 0L
        );
    }
}
