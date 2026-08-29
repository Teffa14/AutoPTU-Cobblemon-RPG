package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CanonicalQuestJournalQueryServiceTest {
    @TempDir Path tempDir;

    @Test
    void listsOnlyOwnerJournalWithAuthoredQuestMetadata() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var acceptance = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository);
        acceptance.accept("player-1", "cedar-ranger", "cedar-field-notes");
        var query = new CanonicalQuestJournalQueryService(CanonicalQuestCatalogue.DEFAULT, repository);

        var playerOne = query.inspect("player-1");
        assertEquals(1L, playerOne.revision());
        assertEquals(1, playerOne.quests().size());
        var quest = playerOne.quests().getFirst();
        assertEquals("cedar-field-notes", quest.questId());
        assertEquals("Cedar Field Notes", quest.title());
        assertEquals("ACCEPTED", quest.state());
        assertEquals(1L, quest.acceptedRevision());

        var playerTwo = query.inspect("player-2");
        assertEquals(0L, playerTwo.revision());
        assertEquals(0, playerTwo.quests().size());
    }

    @Test
    void detailRejectsUnknownOrUnacceptedQuest() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var query = new CanonicalQuestJournalQueryService(CanonicalQuestCatalogue.DEFAULT, repository);

        assertThrows(IllegalArgumentException.class,
                () -> query.inspectQuest("player-1", "not-authored"));
        assertThrows(IllegalArgumentException.class,
                () -> query.inspectQuest("player-1", "cedar-field-notes"));
    }

    @Test
    void detailSurvivesRepositoryReopen() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");

        var reopenedQuery = new CanonicalQuestJournalQueryService(
                CanonicalQuestCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir)
        );
        var quest = reopenedQuery.inspectQuest("player-1", "cedar-field-notes");
        assertEquals("The Cedar Ranger asked you to begin a field journal for Cedar Meadow.", quest.summary());
        assertEquals("Return after observing the meadow lookout and feeding group.", quest.objectiveText());
    }
}
