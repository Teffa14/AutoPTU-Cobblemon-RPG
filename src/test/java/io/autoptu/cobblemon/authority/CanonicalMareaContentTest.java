package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalMareaContentTest {
    @Test
    void everyCanonicalNpcResolvesHomeWorkAndDialogue() {
        assertFalse(CanonicalNpcCatalogue.DEFAULT.npcs().isEmpty());
        for (var npc : CanonicalNpcCatalogue.DEFAULT.npcs()) {
            assertTrue(CanonicalWorldMapCatalogue.DEFAULT.site(npc.homeSiteId()).isPresent(),
                    () -> "missing NPC home site: " + npc.npcId() + " -> " + npc.homeSiteId());
            assertTrue(CanonicalWorldMapCatalogue.DEFAULT.site(npc.workSiteId()).isPresent(),
                    () -> "missing NPC work site: " + npc.npcId() + " -> " + npc.workSiteId());
            assertTrue(CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(npc.npcId()).isPresent(),
                    () -> "missing dialogue surface: " + npc.npcId());
        }
    }

    @Test
    void everyMareaQuestlineQuestAndReferenceResolves() {
        for (var questline : CanonicalQuestlineCatalogue.DEFAULT.questlines()) {
            for (String questId : questline.questIds()) {
                assertTrue(CanonicalQuestCatalogue.DEFAULT.quest(questId).isPresent(),
                        () -> "questline references missing quest: " + questline.questlineId() + " -> " + questId);
            }
            for (String locationId : questline.locationIds()) {
                assertTrue(CanonicalWorldMapCatalogue.DEFAULT.site(locationId).isPresent(),
                        () -> "questline references missing location: " + questline.questlineId() + " -> " + locationId);
            }
            for (String npcId : questline.npcIds()) {
                assertTrue(CanonicalNpcCatalogue.DEFAULT.npc(npcId).isPresent(),
                        () -> "questline references missing NPC: " + questline.questlineId() + " -> " + npcId);
            }
            for (String parentId : questline.parentQuestlineIds()) {
                assertTrue(CanonicalQuestlineCatalogue.DEFAULT.questline(parentId).isPresent(),
                        () -> "questline references missing parent: " + questline.questlineId() + " -> " + parentId);
            }
        }
    }

    @Test
    void everyMareaLocationDiscoveryIdHasFixedAnchor() {
        for (var location : CanonicalLocationCatalogue.DEFAULT.locations()) {
            if (!location.id().startsWith("ouros.marea.")) continue;
            assertTrue(CanonicalWorldMapCatalogue.DEFAULT.site(location.id()).isPresent(),
                    () -> "Marea discovery location has no fixed map anchor: " + location.id());
        }
    }

    @Test
    void mareaQuestsHaveAtLeastOneObjective() {
        for (var questline : CanonicalQuestlineCatalogue.DEFAULT.questlines()) {
            for (String questId : questline.questIds()) {
                assertFalse(CanonicalQuestObjectiveCatalogue.DEFAULT.objectivesForQuest(questId).isEmpty(),
                        () -> "Marea quest has no server-authored objective: " + questId);
            }
        }
    }
}
