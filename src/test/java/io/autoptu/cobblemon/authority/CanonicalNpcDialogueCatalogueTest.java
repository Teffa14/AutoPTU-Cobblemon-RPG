package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalNpcDialogueCatalogueTest {
    @Test
    void defaultCatalogueExposesAuthoredCedarRangerDialogue() {
        var dialogue = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue("cedar-ranger").orElseThrow();
        assertEquals("Cedar Ranger", dialogue.displayName());
        assertEquals(3, dialogue.options().size());
        assertEquals("meadow", dialogue.options().getFirst().optionId());
        assertTrue(dialogue.option("battles").isPresent());
        assertTrue(dialogue.option("missing").isEmpty());
    }

    @Test
    void duplicateNpcIdsFailClosed() {
        var option = new CanonicalNpcDialogueCatalogue.Option("hello", "Hello", "Hello there.");
        var dialogue = new CanonicalNpcDialogueCatalogue.Dialogue("npc", "NPC", "Opening", List.of(option));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalNpcDialogueCatalogue(List.of(dialogue, dialogue)));
    }

    @Test
    void duplicateOptionIdsFailClosed() {
        var first = new CanonicalNpcDialogueCatalogue.Option("same", "First", "One");
        var second = new CanonicalNpcDialogueCatalogue.Option("same", "Second", "Two");
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalNpcDialogueCatalogue.Dialogue("npc", "NPC", "Opening", List.of(first, second)));
    }
}
