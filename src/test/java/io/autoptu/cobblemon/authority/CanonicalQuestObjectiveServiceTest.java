package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestObjectiveServiceTest {
    @TempDir Path tempDir;

    @Test
    void ignoresEventsUntilQuestAcceptedThenCompletesEachObjectiveOnceAcrossRestart() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var objectives = new FileCanonicalQuestObjectiveRepository(tempDir);
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, journals, objectives);

        assertFalse(service.observe("player-1", "cedar_meadow:lookout_watching").changed());

        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");

        var first = service.observe("player-1", "cedar_meadow:lookout_watching");
        assertTrue(first.changed());
        assertEquals(1L, first.updates().getFirst().questProgress().completedCount());
        assertFalse(first.updates().getFirst().questProgress().complete());

        var duplicate = service.observe("player-1", "cedar_meadow:lookout_watching");
        assertFalse(duplicate.changed());
        assertEquals(1L, duplicate.updates().getFirst().questProgress().completedCount());

        var reopened = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var second = reopened.observe("player-1", "cedar_meadow:feeders_alarmed");
        assertTrue(second.changed());
        assertEquals(2L, second.updates().getFirst().questProgress().completedCount());
        assertFalse(second.updates().getFirst().questProgress().complete());

        var third = reopened.observe("player-1", CanonicalQuestObjectiveCatalogue.AUTHORED_QUEST_OBJECT_INSPECTED_EVENT);
        assertTrue(third.changed());
        assertEquals(3L, third.updates().getFirst().questProgress().completedCount());
        assertTrue(third.updates().getFirst().questProgress().complete());

        var persisted = reopened.inspectQuest("player-1", "cedar-field-notes");
        assertEquals(3L, persisted.completedCount());
        assertTrue(persisted.complete());
    }

    @Test
    void patientApproachAdvancesFromThePhysicalFieldNotesEventAfterStoryUnlockAndPersists() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var story = new FileCanonicalWorldStoryRepository(tempDir);
        new CanonicalWorldStoryService(CanonicalWorldStoryCatalogue.DEFAULT, story)
                .choose("player-1", "cedar-meadow-approach", "observe-first");
        var acceptance = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals, story)
                .accept("player-1", "cedar-ranger", "cedar-observer-brief");
        assertTrue(acceptance.newlyAccepted());

        var service = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                journals,
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var reviewed = service.observe("player-1", CanonicalQuestObjectiveCatalogue.AUTHORED_QUEST_OBJECT_INSPECTED_EVENT);
        assertTrue(reviewed.changed());
        assertTrue(reviewed.updates().stream().anyMatch(update ->
                update.objective().questId().equals("cedar-observer-brief")
                        && update.objective().objectiveId().equals("review-field-notes")
                        && update.questProgress().complete()));

        assertFalse(service.observe("player-1", CanonicalQuestObjectiveCatalogue.AUTHORED_QUEST_OBJECT_INSPECTED_EVENT).changed());

        var reopened = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var persisted = reopened.inspectQuest("player-1", "cedar-observer-brief");
        assertEquals(1L, persisted.completedCount());
        assertEquals(1L, persisted.totalCount());
        assertTrue(persisted.complete());
    }

    @Test
    void physicalNpcTalkEventAdvancesOnlyTheAcceptedAuthoredQuestAndPersists() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var objectives = new FileCanonicalQuestObjectiveRepository(tempDir);
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, journals, objectives);
        String eventKey = CanonicalQuestObjectiveCatalogue.npcTalkedEvent("ouros.npc.taro_min");

        assertFalse(service.observe("player-1", eventKey).changed());
        assertEquals(0L, objectives.findOrCreate("player-1").revision());

        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept("player-1", "ouros.npc.nerea_sol", "marea-mirador-observations");

        var contact = service.observe("player-1", eventKey);
        assertTrue(contact.changed());
        assertEquals("consult-taro", contact.updates().getFirst().objective().objectiveId());
        assertEquals(1L, contact.updates().getFirst().questProgress().completedCount());
        assertEquals(3L, contact.updates().getFirst().questProgress().totalCount());
        assertFalse(contact.updates().getFirst().questProgress().complete());

        assertFalse(service.observe("player-1", eventKey).changed());

        var reopened = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var persisted = reopened.inspectQuest("player-1", "marea-mirador-observations");
        assertEquals(1L, persisted.completedCount());
        assertTrue(persisted.objectives().stream()
                .anyMatch(value -> value.objective().objectiveId().equals("consult-taro") && value.completed()));
    }

    @Test
    void tideglassComparisonRequiresReturningToPhysicalTaroAfterCurrentObservations() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var relationships = new FileCanonicalNpcRelationshipRepository(tempDir);
        new CanonicalNpcRelationshipService(CanonicalNpcDialogueCatalogue.DEFAULT, relationships)
                .observeContact("player-1", "ouros.npc.nerea_sol");
        var acceptance = new CanonicalQuestJournalService(
                CanonicalQuestCatalogue.DEFAULT,
                journals,
                null,
                relationships
        ).accept("player-1", "ouros.npc.taro_min", "marea-tideglass-comparison");
        assertTrue(acceptance.newlyAccepted());

        var service = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                journals,
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        assertTrue(service.observe("player-1", "location:ouros.marea.loma_storehouse").changed());
        assertTrue(service.observe("player-1", "location:ouros.marea.estacion_mirador").changed());
        var beforeReturn = service.inspectQuest("player-1", "marea-tideglass-comparison");
        assertEquals(2L, beforeReturn.completedCount());
        assertEquals(3L, beforeReturn.totalCount());
        assertFalse(beforeReturn.complete());

        String talkEvent = CanonicalQuestObjectiveCatalogue.npcTalkedEvent("ouros.npc.taro_min");
        var returned = service.observe("player-1", talkEvent);
        assertTrue(returned.changed());
        assertTrue(returned.updates().stream().anyMatch(update ->
                update.objective().questId().equals("marea-tideglass-comparison")
                        && update.objective().objectiveId().equals("return-to-taro")
                        && update.questProgress().complete()));
        assertFalse(service.observe("player-1", talkEvent).changed());

        var reopened = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var persisted = reopened.inspectQuest("player-1", "marea-tideglass-comparison");
        assertEquals(3L, persisted.completedCount());
        assertTrue(persisted.complete());
    }

    @Test
    void unknownWorldEventsDoNotMutateProgress() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");
        var repository = new FileCanonicalQuestObjectiveRepository(tempDir);
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, journals, repository);

        assertFalse(service.observe("player-1", "client:says_completed").changed());
        assertEquals(0L, repository.findOrCreate("player-1").revision());
    }
}
