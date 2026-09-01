package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalNpcDialogueViewServiceTest {
    @TempDir Path tempDir;

    @Test
    void acceptedQuestBecomesPersistentContinueAffordanceAfterRepositoryReopen() {
        String playerId = "trainer:test";
        String npcId = "cedar-ranger";
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var viewService = new CanonicalNpcDialogueViewService(CanonicalNpcDialogueCatalogue.DEFAULT, CanonicalQuestCatalogue.DEFAULT, journals);
        var initial = option(viewService.inspect(playerId, npcId), "field-notes");
        assertFalse(initial.acceptedQuest());
        assertTrue(initial.eligibleQuest());
        assertNull(initial.lockReason());
        assertEquals("Anything I can help with?", initial.displayLabel());
        assertTrue(new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals).accept(playerId, npcId, "cedar-field-notes").newlyAccepted());
        var reopened = new CanonicalNpcDialogueViewService(CanonicalNpcDialogueCatalogue.DEFAULT, CanonicalQuestCatalogue.DEFAULT, new FileCanonicalQuestJournalRepository(tempDir));
        var persisted = option(reopened.inspect(playerId, npcId), "field-notes");
        assertTrue(persisted.acceptedQuest());
        assertTrue(persisted.eligibleQuest());
        assertEquals("Continue: Cedar Field Notes", persisted.displayLabel());
        assertEquals("cedar-field-notes", persisted.questId());
        var ordinary = option(reopened.inspect(playerId, npcId), "meadow");
        assertFalse(ordinary.acceptedQuest());
        assertTrue(ordinary.eligibleQuest());
    }

    @Test
    void questOptionShowsLockedUntilRequiredCanonicalQuestIsAccepted() {
        String playerId = "trainer:marea";
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var viewService = new CanonicalNpcDialogueViewService(CanonicalNpcDialogueCatalogue.DEFAULT, CanonicalQuestCatalogue.DEFAULT, journals);
        var locked = option(viewService.inspect(playerId, "ouros.npc.mara_veyra"), "route-check");
        assertFalse(locked.eligibleQuest());
        assertEquals("Locked: Reading the Sendero", locked.displayLabel());
        assertEquals("Accept first: The Thin Delivery Season", locked.lockReason());
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals).accept(playerId, "ouros.npc.ivo_serrat", "marea-market-shortfall");
        var unlocked = option(viewService.inspect(playerId, "ouros.npc.mara_veyra"), "route-check");
        assertTrue(unlocked.eligibleQuest());
        assertEquals("I can inspect the Sendero.", unlocked.displayLabel());
        assertNull(unlocked.lockReason());
    }

    @Test
    void cedarRangerProjectsPersistedStoryChoiceAsFollowUpQuestEligibility() {
        String playerId = "trainer:cedar-dialogue";
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var stories = new FileCanonicalWorldStoryRepository(tempDir);
        var viewService = new CanonicalNpcDialogueViewService(CanonicalNpcDialogueCatalogue.DEFAULT, CanonicalQuestCatalogue.DEFAULT, journals, stories);
        var locked = option(viewService.inspect(playerId, "cedar-ranger"), "observer-brief");
        assertFalse(locked.eligibleQuest());
        assertEquals("Locked: The Patient Approach", locked.displayLabel());
        assertEquals("Story choice required: Observe before approaching Cedar Meadow", locked.lockReason());
        new CanonicalWorldStoryService(CanonicalWorldStoryCatalogue.DEFAULT, stories).choose(playerId, "cedar-meadow-approach", "observe-first");
        var unlocked = option(viewService.inspect(playerId, "cedar-ranger"), "observer-brief");
        assertTrue(unlocked.eligibleQuest());
        assertEquals("I observed before approaching.", unlocked.displayLabel());
        assertNull(unlocked.lockReason());
    }

    private static CanonicalNpcDialogueViewService.OptionView option(CanonicalNpcDialogueViewService.DialogueView view, String optionId) {
        return view.options().stream().filter(option -> option.optionId().equals(optionId)).findFirst().orElseThrow();
    }
}
