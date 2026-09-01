package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalNpcDialogueViewServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptedQuestBecomesPersistentContinueAffordanceAfterRepositoryReopen() {
        String playerId = "trainer:test";
        String npcId = "cedar-ranger";
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var viewService = new CanonicalNpcDialogueViewService(
                CanonicalNpcDialogueCatalogue.DEFAULT,
                CanonicalQuestCatalogue.DEFAULT,
                journals
        );

        var initial = option(viewService.inspect(playerId, npcId), "field-notes");
        assertFalse(initial.acceptedQuest());
        assertEquals("Anything I can help with?", initial.displayLabel());

        var accepted = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept(playerId, npcId, "cedar-field-notes");
        assertTrue(accepted.newlyAccepted());

        var reopened = new CanonicalNpcDialogueViewService(
                CanonicalNpcDialogueCatalogue.DEFAULT,
                CanonicalQuestCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir)
        );
        var persisted = option(reopened.inspect(playerId, npcId), "field-notes");
        assertTrue(persisted.acceptedQuest());
        assertEquals("Continue: Cedar Field Notes", persisted.displayLabel());
        assertEquals("cedar-field-notes", persisted.questId());

        var ordinary = option(reopened.inspect(playerId, npcId), "meadow");
        assertFalse(ordinary.acceptedQuest());
        assertEquals("What should I watch for?", ordinary.displayLabel());
    }

    private static CanonicalNpcDialogueViewService.OptionView option(
            CanonicalNpcDialogueViewService.DialogueView view,
            String optionId
    ) {
        return view.options().stream()
                .filter(option -> option.optionId().equals(optionId))
                .findFirst()
                .orElseThrow();
    }
}
