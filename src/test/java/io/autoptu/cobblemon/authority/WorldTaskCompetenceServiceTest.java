package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTaskCompetenceServiceTest {
    private final WorldTaskCompetenceService service = new WorldTaskCompetenceService();

    @Test
    void canonicalSkillRankSelectsServerAuthoredQualityBand() {
        CanonicalPlayerState trainer = trainer(Map.of("Survival", 4));
        WorldTaskDefinition task = new WorldTaskCatalogue().find("field_ration").orElseThrow();

        WorldTaskCompetenceService.Assessment assessment = service.assess(trainer, task);

        assertTrue(assessment.understood());
        assertEquals(4, assessment.canonicalSkillRank());
        assertEquals(new WorldTaskDefinition.QualityDistribution(20, 50, 30), assessment.distribution());
    }

    @Test
    void fieldCampUsesTheSameCanonicalTrainerBoundaryWithoutBecomingACraftingRecipe() {
        CanonicalPlayerState trainer = trainer(Map.of("Survival", 4));
        WorldTaskCatalogue catalogue = new WorldTaskCatalogue();
        WorldTaskDefinition task = catalogue.find(WorldTaskCatalogue.FIELD_CAMP_SETUP).orElseThrow();

        WorldTaskCompetenceService.Assessment assessment = service.assess(trainer, task);

        assertTrue(assessment.understood());
        assertEquals(4, assessment.canonicalSkillRank());
        assertEquals(new WorldTaskDefinition.QualityDistribution(20, 50, 30), assessment.distribution());
        assertTrue(catalogue.findRecipe(WorldTaskCatalogue.FIELD_CAMP_SETUP).isEmpty());
    }

    @Test
    void normalizedCanonicalSkillIdDoesNotRequireASecondClientSuppliedRank() {
        CanonicalPlayerState trainer = trainer(Map.of("technology_education", 3));
        WorldTaskDefinition task = new WorldTaskCatalogue().find("precision_poketech_parts").orElseThrow();

        WorldTaskCompetenceService.Assessment assessment = service.assess(trainer, task);

        assertTrue(assessment.understood());
        assertEquals(3, assessment.canonicalSkillRank());
        assertEquals(new WorldTaskDefinition.QualityDistribution(35, 45, 20), assessment.distribution());
    }

    @Test
    void hardKnowledgeRequirementFailsClosedWithoutInventingCompetence() {
        CanonicalPlayerState trainer = trainer(Map.of("Survival", 7));
        WorldTaskDefinition task = new WorldTaskCatalogue().find("occult_lure").orElseThrow();

        WorldTaskCompetenceService.Assessment assessment = service.assess(trainer, task);

        assertFalse(assessment.understood());
        assertEquals(0, assessment.canonicalSkillRank());
        assertTrue(assessment.detail().contains("Occult Education rank 1"));
    }

    @Test
    void conflictingNormalizedAliasesFailClosed() {
        CanonicalPlayerState trainer = trainer(Map.of(
                "Technology Education", 2,
                "technology_education", 4
        ));
        WorldTaskDefinition task = new WorldTaskCatalogue().find("precision_poketech_parts").orElseThrow();

        assertThrows(IllegalStateException.class, () -> service.assess(trainer, task));
    }

    @Test
    void taskQualityCurveMustBeCompleteAndProbabilityConserving() {
        TreeMap<Integer, WorldTaskDefinition.QualityDistribution> missingBaseline = new TreeMap<>();
        missingBaseline.put(1, new WorldTaskDefinition.QualityDistribution(50, 40, 10));
        assertThrows(IllegalArgumentException.class, () -> new WorldTaskDefinition(
                "bad", "Bad", "Survival", 0, missingBaseline));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTaskDefinition.QualityDistribution(50, 40, 20));
    }

    private static CanonicalPlayerState trainer(Map<String, Integer> skillRanks) {
        return new CanonicalPlayerState(
                "player-1",
                Set.of(),
                skillRanks,
                Set.of(),
                Set.of(),
                0,
                0,
                0,
                "player-1",
                0
        );
    }
}
